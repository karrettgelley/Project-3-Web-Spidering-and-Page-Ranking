package ir.webutils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;

import ir.utilities.*;

/**
 * Spider defines a framework for writing a web crawler. Users can change the behavior of the spider
 * by overriding methods. Default spider does a breadth first crawl starting from a given URL up to
 * a specified maximum number of pages, saving (caching) the pages in a given directory. Also adds a
 * "BASE" HTML command to cached pages so links can be followed from the cached version.
 *
 * @author Ted Wild and Ray Mooney
 */

public class PageRankSpider extends Spider {

    /**
     * A graph that represents the crawl before doing PageRank
     */
    PageRankGraph crawlGraph = new PageRankGraph();

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
     * "Indexes" a <code>HTMLpage</code>. This version just writes it out to a file in the specified
     * directory with a "P<count>.html" file name.
     *
     * @param page An <code>HTMLPage</code> that contains the page to index.
     */

    protected void indexPage(HTMLPage page) {
        // Define the page number of this document
        String pageNumber = "P"
                + MoreString.padWithZeros(count, (int) Math.floor(MoreMath.log(maxCount, 10)) + 1);

        // Get the Node associated with the current page
        PageRankNode node = crawlGraph.getNode(page.link.getURL().toString());
        node.pageNumber = pageNumber + ".html";
        node.isIndexed = true;

        for (Link link : new LinkExtractor(page).extractLinks()) {
            // Add edge if page does not link to istelf
            String linkName = link.getURL().toString();
            if (!linkName.equals(node.name)) {
                node.addEdge(crawlGraph.getNode(linkName));
            }
        }

        page.write(saveDir, pageNumber);
    }

    /**
     * Computes the PageRank of all of the indexed documents and stores the values in
     * page_ranks.txt. The entries in the text file are of the form: P1.html 0.0
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
            PageRankNode node = crawlGraph.nextNode();
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
        printPageRank(newRank);
    }


    /**
     * Prints the result of the PageRank algorithm to the console
     * 
     * @param rank a map from document names to PageRanks
     */
    protected void printPageRank(HashMap<String, Double> rank) {
        System.out.println("\nPage Rank:");

        for (PageRankNode n : crawlGraph.nodeArray()) {
            System.out.println("PR(" + n.name + "):" + rank.get(n.pageNumber));
        }
    }

    /**
     * Writes the result of the PageRank algorithm to a file page_ranks.txt in saveDir
     * 
     * @param rank a map from document names to PageRanks
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
     * @param rank    A HashMap that maps the name of a document (i.e. P001.html) to its PageRank
     * @param newRank A subsidiary HashMap similar to rank to aid in testing PageRank convergence
     */
    protected void initializePageRank(HashMap<String, Double> rank,
            HashMap<String, Double> newRank) {
        // Iterate every indexed document
        crawlGraph.resetIterator();
        PageRankNode n = crawlGraph.nextNode();
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
    protected double getNewPageRank(PageRankNode node, HashMap<String, Double> rank, double alpha,
            double rankSource) {
        double sumPageRank = 0.0;

        // Get the list of nodes that are incident to node
        List<PageRankNode> incomingNodes = node.getEdgesIn();

        // Calculate ΣR(p)/N_p where p is a node incidient to node, N is the number of
        // outlinks from p, and R(p) is the PageRank of p
        for (PageRankNode n : incomingNodes) {
            sumPageRank += rank.get(n.pageNumber) / (n.getEdgesOut().size());
        }

        double newPageRank = ((1 - alpha) * sumPageRank) + rankSource;
        return newPageRank;
    }

    /**
     * Creates a new Graph which only contains indexed Node(s).
     */
    protected PageRankGraph cleanCrawlGraph() {
        PageRankGraph cleanedGraph = new PageRankGraph();

        // Build a new graph
        crawlGraph.resetIterator();
        PageRankNode node = crawlGraph.nextNode();
        while (node != null) {
            if (node.isIndexed) {
                // Get the node with matching name from new Graph
                PageRankNode cleanedNode = cleanedGraph.getNode(node.name);
                cleanedNode.pageNumber = node.pageNumber;
                cleanedNode.isIndexed = true;

                // Copy edges from indexed nodes and remove duplicate edges
                for (PageRankNode n : node.edgesOut) {
                    if (n.isIndexed
                            && !cleanedNode.edgesOut.contains(cleanedGraph.getNode(n.name))) {
                        cleanedNode.addEdge(cleanedGraph.getNode(n.name));
                    }
                }
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
     * <li>-safe : Check for and obey robots.txt and robots META tag directives.</li>
     * <li>-d &lt;directory&gt; : Store indexed files in &lt;directory&gt;.</li>
     * <li>-c &lt;maxCount&gt; : Store at most &lt;maxCount&gt; files (default is 10,000).</li>
     * <li>-u &lt;url&gt; : Start at &lt;url&gt;.</li>
     * <li>-slow : Pause briefly before getting a page. This can be useful when debugging.
     * </ul>
     */
    public static void main(String args[]) {
        new PageRankSpider().go(args);
    }
}
