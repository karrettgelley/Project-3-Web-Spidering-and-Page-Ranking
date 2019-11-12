package ir.webutils;

import java.util.*;
import java.io.*;

/**
 * Node in the the Graph data structure.
 *
 * @see Graph
 *
 * @author Garrett Kelley
 */
public class PageRankNode {

    /**
     * Name of the node.
     */
    String name;

    /**
     * Name of document associated with this node.
     */
    String pageNumber;

    /**
     * Denotes whether or not the document associated with this node has been indexed.
     */
    boolean isIndexed;

    /**
     * Lists of incoming and outgoing edges.
     */
    List<PageRankNode> edgesOut = new ArrayList<PageRankNode>();
    List<PageRankNode> edgesIn = new ArrayList<PageRankNode>();

    /**
     * Constructs a node with that name.
     */
    public PageRankNode(String name) {
        this.name = name;
        this.isIndexed = false;
        this.pageNumber = "";
    }

    /**
     * Adds an outgoing edge
     */
    public void addEdge(PageRankNode node) {
        edgesOut.add(node);
        node.addEdgeFrom(this);
    }

    /**
     * Adds an incoming edge
     */
    void addEdgeFrom(PageRankNode node) {
        edgesIn.add(node);
    }

    /**
     * Returns the name of the node
     */
    public String toString() {
        return name;
    }

    /**
     * Gives the list of outgoing edges
     */
    public List<PageRankNode> getEdgesOut() {
        return edgesOut;
    }

    /**
     * Gives the list of incoming edges
     */
    public List<PageRankNode> getEdgesIn() {
        return edgesIn;
    }
}
