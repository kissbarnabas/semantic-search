package org.bme.mit.iir;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Indexer {
    private String searchDirectoryPath;
    private String outputFilePath;
    private String stopWordsFilePath;
    private Map<String, Map<String, Integer>> indexMap = new HashMap<>();
    private Util util = new Util();
    private TermRecognizer termRecognizer;
    private Gson gson = new Gson();

    public Indexer(String searchDirectoryPath, String outputFilePath, String stopWordsFilePath) {
        this.searchDirectoryPath = searchDirectoryPath;
        this.outputFilePath = outputFilePath;
        this.stopWordsFilePath = stopWordsFilePath;

        try {
            termRecognizer = new TermRecognizer(getStopWordsSet());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Returns a list constructed from the file containing the stop words
    private HashSet<String> getStopWordsSet() throws IOException {
        return new HashSet<String>(
                Util.readLinesIntoList(stopWordsFilePath));
    }

    // Returns all filenames of regular files in search directory and all subdirectories
    public List<Path> getFileNames(String sDir) {
        try {
            int i= 0;
            return Files.find(Paths.get(sDir), 999, (p, bfa) -> bfa.isRegularFile())
                    .collect (Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Creates index file
    public void createIndexFile() {
        try {
            //Map with key: word, value: a map with key: filename, value: frequency of occurrence
            var frequency = new HashMap<String,Map<String, Integer>>() ;

            var filesOfDir = getFileNames(searchDirectoryPath);

            for(Path file : filesOfDir){
                String filename = file.getFileName().toString();

                //Get all words with frequencies in current file
                var frequenciesOfFile = getFrequenciesOfFile(file.toString());

                //Iterating through all words in file and creating indices
                for (var frequencyOfWord : frequenciesOfFile.entrySet()){
                    String word = frequencyOfWord.getKey();

                    var currentValue = frequency.get(word);

                    //If entry does not exist, create new entry
                    if(currentValue == null){
                        var newWordMap = new HashMap<String, Integer>();
                        newWordMap.put(filename, frequencyOfWord.getValue());
                        frequency.put(word, newWordMap);
                    } else {
                        currentValue.put(filename, frequencyOfWord.getValue());
                    }

                }
            }

            String json = gson.toJson(frequency);
            printToFile(outputFilePath, json);
            System.out.println("Success! Results can be found in file "+outputFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void findDocumentsWithAllWords(List<String> searchKeywords, int minSearchWords){
        //A map containing the indices. Key: word, value: a map with key: filename, value: frequency of occurrence
        HashMap<String,Map<String, Double>> indices = gson.fromJson(readFile(outputFilePath), HashMap.class);

        //A map containing the results of the search.
        // The DocumentStats stores the number of search words, that can be found in the documents, and the cumulated frequencies.
        HashMap<String, DocumentStats> result = new HashMap<>();

        //A map containing the search keywords, that can actually be found in a document.
        HashMap<String, List<String>> wordsFound = new HashMap<>();

        for(String word : searchKeywords){
            //Documents containing the current keyword (with frequencies)
            Map<String, Double> docs = indices.get(word);
            if(docs != null ){
                for(var entry: docs.entrySet()){

                    String documentName = entry.getKey();

                    //Update list of keywords found in document
                    var foundInDoc = wordsFound.get(documentName);
                    if(foundInDoc == null){
                        foundInDoc = new ArrayList<String>();
                        wordsFound.put(documentName, foundInDoc);
                    }
                    foundInDoc.add(word);

                    //Update statistics of document
                    var statEntry = result.get(documentName);
                    if(statEntry == null){
                        var newDocumentStat = new DocumentStats();
                        newDocumentStat.incrementSearchWords();
                        newDocumentStat.increaseTotalFrequency(entry.getValue());

                        result.put(documentName, newDocumentStat);
                    } else{
                        statEntry.incrementSearchWords();
                        statEntry.increaseTotalFrequency(entry.getValue());
                    }
                }
            } else {
                System.out.println("Not found!");
            }
        }

        //This map stores only the entries of those documents, that contain at least so many search keywords, as specified by minSearchWords
        HashMap<String, Integer> finalResultMap = new HashMap<>();
        for(var entry: result.entrySet()){
            if(entry.getValue().getSearchWords()>=minSearchWords){
                //Value considered by the ranking of results: product of search searchWords and total frequency
                finalResultMap.put(entry.getKey(), (int) (entry.getValue().searchWords*entry.getValue().totalFrequency));
            }
        }
        var resultList = sortMapByValue(finalResultMap);
        resultList.forEach(System.out::println);
        for(var res : resultList){
            var found = wordsFound.get(res.getKey());
            System.out.println("Words found in "+res.getKey()+": "+String.join(", ", found));
        }
        System.out.println("Number of relevant documents: "+resultList.size());
    }

    //A class responsible to store the statistics of a document
    class DocumentStats{
        //Number of search keywords contained in the document
        private Double searchWords = 0.0;
        //Cumulated frequencies of keywords in document
        private Double totalFrequency = 0.0;

        public void incrementSearchWords(){
            searchWords++;
        }
        public void increaseTotalFrequency(Double increaseValue){
            totalFrequency += increaseValue;
        }

        public Double getSearchWords() {
            return searchWords;
        }

        public Double getTotalFrequency() {
            return totalFrequency;
        }
    }

    private List<Map.Entry<String, Integer>> sortMapByValue(Map<String, Integer> map ){
        Stream<Map.Entry<String, Integer>> sorted =
                map.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue());
        List<Map.Entry<String, Integer>> list = sorted.collect(Collectors.toList());
        Collections.reverse(list);
        return list;
    }

    //Returns all files in the directory and subdirectories
    private Map<String, Integer> getFrequenciesOfFile(String filePath) throws IOException {
        var lines = Util.readLinesIntoList(filePath);
        return termRecognizer.termFrequency(String.join(" ",lines ));
    }

    private void printToFile(String filePath, String text){
        try (PrintWriter out = new PrintWriter(filePath)) {
            out.println(text);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    //Reads file and returns its content as a string
    private String readFile(String filePath){
        String json = "";
        try {
            try (BufferedReader br = Files.newBufferedReader(Paths.get(filePath), StandardCharsets.UTF_8)) {
                for (String line = null; (line = br.readLine()) != null;) {
                    json += line;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return json;
    }
}
