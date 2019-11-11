package ir.vsr;

import java.io.*;
import java.util.*;
import java.lang.*;

import ir.utilities.*;
import ir.classifiers.*;

/**
 * An inverted index for vector-space information retrieval. Contains methods
 * for creating an inverted index from a set of documents and retrieving ranked
 * matches to queries using standard TF/IDF weighting and cosine similarity.
 *
 * @author Ray Mooney
 */
public class PageRankInvertedIndex extends InvertedIndex {

    /**
     * A scaling parameter for PageRank
     */
    double weight;

    /**
     * A datastructure that maps document names to their PageRank score
     */
    HashMap<String, Double> pageRank;

    /**
     * Create an inverted index of the documents in a directory.
     *
     * @param dirFile  The directory of files to index.
     * @param docType  The type of documents to index (See docType in
     *                 DocumentIterator)
     * @param stem     Whether tokens should be stemmed with Porter stemmer.
     * @param feedback Whether relevance feedback should be used.
     * @param weight   a scaling parameter for PageRank
     */
    public PageRankInvertedIndex(File dirFile, short docType, boolean stem, boolean feedback, double weight) {
        super(dirFile, docType, stem, feedback);
        this.weight = weight;
        this.pageRank = new HashMap<String, Double>();
        getPageRanks();
    }

    /**
     * Create an inverted index of the documents in a List of Example objects of
     * documents for text categorization.
     *
     * @param examples A List containing the Example objects for text categorization
     *                 to index
     */
    public PageRankInvertedIndex(List<Example> examples) {
        super(examples);
    }

    /**
     * Reads the values from page_ranks.txt into the pageRank HashMap.
     */
    private void getPageRanks() {
        File[] files;

        // Get the page_ranks.txt file from the indexed directory
        FilenameFilter filenameFilter = new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                if (name.equals("page_ranks.txt")) {
                    return true;
                } else {
                    return false;
                }
            }
        };

        files = dirFile.listFiles(filenameFilter);

        // Read the input from page_ranks.txt to be stored in the map
        try {
            BufferedReader br = new BufferedReader(new FileReader(files[0]));
            String st;

            while ((st = br.readLine()) != null) {
                String[] sts = st.split(" ");
                pageRank.put(sts[0], Double.valueOf(sts[1]));
            }
        } catch (Exception e) {
            System.out.println(e);
            System.exit(0);
        }
    }

    /**
     * Print out an inverted index by listing each token and the documents it occurs
     * in. Include info on IDF factors, occurrence counts, and document vector
     * lengths.
     */
    public void print() {
        // Iterate through each token in the index
        for (Map.Entry<String, TokenInfo> entry : tokenHash.entrySet()) {
            String token = entry.getKey();
            // Print the token and its IDF factor
            System.out.println(token + " (IDF=" + entry.getValue().idf + ") occurs in:");
            // For each document referenced, print its name, occurrence count for this
            // token, and
            // document vector length (|D|).
            for (TokenOccurrence occ : entry.getValue().occList) {
                System.out.println(
                        "   " + occ.docRef.file.getName() + " " + occ.count + " times; |D|=" + occ.docRef.length);
            }
        }
    }

    /**
     * Calculate the final score for a retrieval and return a Retrieval object
     * representing the retrieval with its final score.
     *
     * @param queryLength The length of the query vector, incorporated into the
     *                    final score
     * @param docRef      The document reference for the document concerned
     * @param score       The partially computed score
     * @return The retrieval object for the document described by docRef and score
     *         under the query with length queryLength
     */
    protected Retrieval getRetrieval(double queryLength, DocumentReference docRef, double score) {
        // Normalize score for the lengths of the two document vectors
        score = score / (queryLength * docRef.length);

        // Add scaled PageRank to score
        score += pageRank.get(docRef.toString()) * weight;

        // Add a Retrieval for this document to the result array
        return new Retrieval(docRef, score);
    }

    /**
     * Index a directory of files and then interactively accept retrieval queries.
     * Command format: "InvertedIndex [OPTION]* [DIR]" where DIR is the name of the
     * directory whose files should be indexed, and OPTIONs can be "-html" to
     * specify HTML files whose HTML tags should be removed. "-stem" to specify
     * tokens should be stemmed with Porter stemmer. "-feedback" to allow relevance
     * feedback from the user.
     */
    public static void main(String[] args) {
        // Parse the arguments into a directory name and optional flag

        String dirName = args[args.length - 1];
        short docType = DocumentIterator.TYPE_TEXT;
        boolean stem = false, feedback = false;
        double weight = 0.0;
        for (int i = 0; i < args.length - 1; i++) {
            String flag = args[i];
            if (flag.equals("-html"))
                // Create HTMLFileDocuments to filter HTML tags
                docType = DocumentIterator.TYPE_HTML;
            else if (flag.equals("-stem"))
                // Stem tokens with Porter stemmer
                stem = true;
            else if (flag.equals("-feedback"))
                // Use relevance feedback
                feedback = true;
            else if (flag.equals("-weight"))
                weight = Double.valueOf(args[++i]);
            else {
                throw new IllegalArgumentException("Unknown flag: " + flag);
            }
        }

        // Create an inverted index for the files in the given directory.
        PageRankInvertedIndex index = new PageRankInvertedIndex(new File(dirName), docType, stem, feedback, weight);
        // index.print();
        // Interactively process queries to this index.
        index.processQueries();
    }

}
