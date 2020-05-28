package org.bme.mit.iir;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args){
        String searchDirectoryPath = "../res/corpus";
        String outputFilePath = "indices.json";
        String stopWordsFilePath = "../res/stopwords.txt";

        Indexer indexer = new Indexer(searchDirectoryPath, outputFilePath, stopWordsFilePath );

        indexer.createIndexFile();

        ArrayList<String> words = new ArrayList<>();
        ArrayList<String> expandedQueryWords = new ArrayList<>();
        ReasoningExample reasoning = new ReasoningExample("../res/pc_shop.owl_modified.xml");


        words.add("processzor");
        words.add("videokártya");
        words.add("merevlemez");
        words.add("legjobb");
        System.out.println("A megadott keresőszavak: "+String.join(", ", words));
        //indexer.readLinesOfFile();

        expandedQueryWords.addAll(words);

        for(var searchWord : words){
            expandedQueryWords.addAll(reasoning.getQueryExpansion(searchWord));
        }

        System.out.println("A kibővített keresőszavak listája: "+String.join(", ", expandedQueryWords));

        indexer.findDocumentsWithAllWords(expandedQueryWords, 3);
    }
}
