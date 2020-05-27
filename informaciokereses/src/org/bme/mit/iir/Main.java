package org.bme.mit.iir;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args){
        Indexer indexer = new Indexer();
        ArrayList<String> words = new ArrayList<>();
        words.add("kutya");
        words.add( "macska");
        words.add( "alma");
        words.add( "gyertya");
        indexer.findDocumentsWithAllWords(words, 2);
    }
}
