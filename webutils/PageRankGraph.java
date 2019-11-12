package ir.webutils;

import java.util.*;
import java.io.*;

/**
 * Graph data structure.
 *
 * @author Garrett Kelley
 */
public class PageRankGraph {

    private Map<String, PageRankNode> nodeMap = new HashMap<String, PageRankNode>();
    Iterator<Map.Entry<String, PageRankNode>> iterator;

    /**
     * Basic constructor.
     */
    public PageRankGraph() {
    }

    /**
     * Adds an edge from xName to yName.
     */
    public void addEdge(String xName, String yName) {
        PageRankNode xNode = getNode(xName);
        PageRankNode yNode = getNode(yName);
        xNode.addEdge(yNode);
    }

    /**
     * Adds a node if it is not already present.
     */
    public boolean addNode(String name) {
        PageRankNode node = getExistingNode(name);
        if (node == null) {
            node = new PageRankNode(name);
            nodeMap.put(name, node);
            return true;
        } else
            return false;
    }

    /**
     * Returns the node with that name, creates one if not already present.
     */
    public PageRankNode getNode(String name) {
        PageRankNode node = getExistingNode(name);
        if (node == null) {
            node = new PageRankNode(name);
            nodeMap.put(name, node);
        }
        return node;
    }

    /**
     * Reads graph from file where each line consists of a node-name followed by a list of the names
     * of nodes to which it points
     */
    public void readFromFile(String fileName) throws IOException {
        String line;
        BufferedReader in = new BufferedReader(new FileReader(fileName));
        while ((line = in.readLine()) != null) {
            StringTokenizer tokenizer = new StringTokenizer(line);
            String source = tokenizer.nextToken();
            while (tokenizer.hasMoreTokens()) {
                addEdge(source, tokenizer.nextToken());
            }
        }
        in.close();
    }

    /**
     * Returns the node with that name
     */
    public PageRankNode getExistingNode(String name) {
        return nodeMap.get(name);
    }

    /**
     * Resets the iterator.
     */
    public void resetIterator() {
        iterator = nodeMap.entrySet().iterator();
    }

    /**
     * Returns the next node in an iterator over the nodes of the graph
     */
    public PageRankNode nextNode() {
        if (iterator == null) {
            throw new IllegalStateException("Graph: Error: Iterator not set.");
        }
        if (iterator.hasNext())
            return iterator.next().getValue();
        else
            return null;
    }

    /**
     * Prints the entire graph on stdout.
     */
    public void print() {
        PageRankNode node;
        resetIterator();
        while ((node = nextNode()) != null) {
            System.out.println(node + "->" + node.getEdgesOut());
        }
    }

    /**
     * Returns all the nodes of the graph.
     */
    public PageRankNode[] nodeArray() {
        PageRankNode[] nodes = new PageRankNode[nodeMap.size()];
        PageRankNode node;
        int i = 0;
        resetIterator();
        while ((node = nextNode()) != null) {
            nodes[i++] = node;
        }
        return nodes;
    }


    public static void main(String[] args) throws IOException {
        PageRankGraph graph = new PageRankGraph();
        graph.readFromFile(args[0]);
        graph.print();
        System.out.println("\n" + graph.nodeArray().toString());
    }
}
