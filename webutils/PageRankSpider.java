package ir.webutils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;

import ir.utilities.*;

/**
 * Spider defines a framework for writing a web crawler. Users can change the
 * behavior of the spider by overriding methods. Default spider does a breadth
 * first crawl starting from a given URL up to a specified maximum number of
 * pages, saving (caching) the pages in a given directory. Also adds a "BASE"
 * HTML command to cached pages so links can be followed from the cached
 * version.
 *
 * @author Ted Wild and Ray Mooney
 */

public class PageRankSpider extends Spider {

    /**
     * A graph that represents the crawl before doing PageRank
     */
    Graph crawlGraph = new Graph();

    /**
     * Checks command line arguments and performs the crawl.
     * <p>
     * This implementation calls <code>processArgs</code> and <code>doCrawl</code>.
     *
     * @param args Command line arguments.
     */
    public void go(String[] args) {
        processArgs(args);
        doCrawl();

        // Removes extraneous nodes from the crawl graph
        crawlGraph = cleanCrawlGraph();
        System.out.println("Graph Structure");
        crawlGraph.print();

        // Does PageRank on the crawl graph
        doPageRank();
    }

    /**
     * "Indexes" a <code>HTMLpage</code>. This version just writes it out to a file
     * in the specified directory with a "P<count>.html" file name.
     *
     * @param page An <code>HTMLPage</code> that contains the page to index.
     */

    protected void indexPage(HTMLPage page) {
        // Define the page number of this document
        String pageNumber = "P" + MoreString.padWithZeros(count, (int) Math.floor(MoreMath.log(maxCount, 10)) + 1);

        // Get the Node associated with the current page
        Node node = crawlGraph.getNode(page.link.getURL().toString());
        node.pageNumber = pageNumber;
        node.isIndexed = true;

        for (Link link : new LinkExtractor(page).extractLinks()) {
            node.addEdge(crawlGraph.getNode(link.getURL().toString()));
        }

        page.write(saveDir, pageNumber);
    }

    /**
     * Computes the PageRank of all of the indexed documents and stores the values
     * in page_ranks.txt. The entries in the text file are of the form: P1.html 0.0
     */
    protected void doPageRank() {
        // Define PageRank hyperparameters
        double alpha = 0.15;
        double rankSource = alpha / count;
        int iterations = 50;

        // Initialize PageRank of every indexed document
        HashMap<String, Double> rank = new HashMap<String, Double>();
        HashMap<String, Double> newRank = new HashMap<String, Double>();
        initializePageRank(rank, newRank);

        for (int i = 0; i < iterations; i++) {
            // For every indexed document
            crawlGraph.resetIterator();
            Node node = crawlGraph.nextNode();
            while (node != null) {
                // Compute the PageRank for the current iteration and store it
                newRank.put(node.pageNumber, getNewPageRank(node, rank, alpha, rankSource));

                node = crawlGraph.nextNode();
            }

            // Normalize the ranks
            normalizeRanks(newRank);

            // Update rank with the current iteration PageRank's
            for (Map.Entry<String, Double> entry : newRank.entrySet()) {
                rank.put(entry.getKey(), entry.getValue());
            }
        }

        writePageRank(newRank);
    }

    /**
     * Writes the result of the PageRank algorithm to a file page_ranks.txt in
     * saveDir
     * 
     * @param rank
     */
    protected void writePageRank(HashMap<String, Double> rank) {
        try {
            // Write PageRank to page_ranks.txt file
            PrintWriter out = new PrintWriter(new FileWriter(new File(saveDir, "page_ranks.txt")));
            for (Map.Entry<String, Double> entry : rank.entrySet()) {
                out.println(entry.getKey() + " " + String.valueOf(entry.getValue()));
            }
            out.close();
        } catch (IOException e) {
            System.out.println(e);
            System.exit(0);
        }
    }

    /**
     * Initializes the PageRank of the indexed documents.
     * 
     * @param rank    A HashMap that maps the name of a document (i.e. P001.html) to
     *                its PageRank
     * @param newRank A subsidiary HashMap similar to rank to aid in testing
     *                PageRank convergence
     */
    protected void initializePageRank(HashMap<String, Double> rank, HashMap<String, Double> newRank) {
        // Iterate every indexed document
        crawlGraph.resetIterator();
        Node n = crawlGraph.nextNode();
        while (n != null) {
            // Initialize the PageRank to 1/|S| where S is the set of indexed docuemnts
            rank.put(n.pageNumber, 1.0 / count);
            newRank.put(n.pageNumber, 1.0 / count);
            n = crawlGraph.nextNode();
        }
    }

    /**
     * Normalizes the PageRank of each indexed document.
     * 
     * @param newRank a HashMap that maps the name of a document to its PageRank
     */
    protected void normalizeRanks(HashMap<String, Double> newRank) {
        double sumPageRank = 0.0;

        // Compute the ΣR(p) where p is a document and R(p) is the PageRank of document
        // p
        for (Map.Entry<String, Double> entry : newRank.entrySet()) {
            sumPageRank += entry.getValue();
        }

        // We normalize by 1/ΣR(p)
        double normalizationFactor = 1.0 / sumPageRank;

        // Normalize the PageRank of every indexed document
        for (Map.Entry<String, Double> entry : newRank.entrySet()) {
            newRank.put(entry.getKey(), normalizationFactor * entry.getValue());
        }
    }

    /**
     * Calculates the sum of the PageRanks of the nodes incident to node.
     * 
     * @param node the node to be evaluated
     * @param rank a HashMap that maps a node to its PageRank
     * @return the sum of the PageRanks of the nodes incident to node
     */
    protected double getNewPageRank(Node node, HashMap<String, Double> rank, double alpha, double rankSource) {
        double sumPageRank = 0.0;

        // Get the list of nodes that are incident to node
        List<Node> incomingNodes = node.getEdgesIn();

        // Calculate ΣR(p)/N_p where p is a node incidient to node, N is the number of
        // outlinks from p, and R(p) is the PageRank of p
        for (Node n : incomingNodes) {
            sumPageRank += rank.get(n.pageNumber) / (n.getEdgesOut().size());
        }

        double newPageRank = ((1 - alpha) * sumPageRank) + rankSource;
        return newPageRank;
    }

    /**
     * Creates a new Graph which only contains indexed Node(s).
     */
    protected Graph cleanCrawlGraph() {
        Graph cleanedGraph = new Graph();

        crawlGraph.resetIterator();
        Node node = crawlGraph.nextNode();
        while (node != null) {
            if (node.isIndexed) {
                // Get the node with matching name from new Graph
                Node cleanedNode = cleanedGraph.getNode(node.name);
                cleanedNode.pageNumber = node.pageNumber;
                cleanedNode.isIndexed = true;

                // Copy out edges from indexed nodes
                for (Node n : node.edgesOut) {
                    if (n.isIndexed && !cleanedNode.edgesOut.contains(cleanedGraph.getNode(n.name))) {
                        cleanedNode.addEdge(cleanedGraph.getNode(n.name));
                    }
                }

                // // Copy in edges from indexed nodes
                // for (Node n : node.edgesIn) {
                // if (n.isIndexed) {
                // cleanedNode.addEdgeFrom(cleanedGraph.getNode(n.name));
                // }
                // }
            } else {
                crawlGraph.iterator.remove();
            }

            node = crawlGraph.nextNode();
        }

        return cleanedGraph;
    }

    /**
     * Spider the web according to the following command options:
     * <ul>
     * <li>-safe : Check for and obey robots.txt and robots META tag
     * directives.</li>
     * <li>-d &lt;directory&gt; : Store indexed files in &lt;directory&gt;.</li>
     * <li>-c &lt;maxCount&gt; : Store at most &lt;maxCount&gt; files (default is
     * 10,000).</li>
     * <li>-u &lt;url&gt; : Start at &lt;url&gt;.</li>
     * <li>-slow : Pause briefly before getting a page. This can be useful when
     * debugging.
     * </ul>
     */
    public static void main(String args[]) {
        new PageRankSpider().go(args);
    }
}
