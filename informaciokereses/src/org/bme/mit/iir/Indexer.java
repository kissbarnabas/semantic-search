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
    private String searchDirectoryPath="../res/a";
    private String outputFilePath="indices.json";
    private String stopWordsFilePath="../res/stopwords.txt";
    private Map<String, Map<String, Integer>> indexMap = new HashMap<>();
    private Util util = new Util();
    private TermRecognizer termRecognizer;
    private Gson gson = new Gson();

    {
        try {
            termRecognizer = new TermRecognizer(getStopWordsSet());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Indexer() {

    }
    public Indexer(String searchDirectoryPath, String outputFilePath, String stopWordsFilePath) {
        this.searchDirectoryPath = searchDirectoryPath;
        this.outputFilePath = outputFilePath;
        this.stopWordsFilePath = stopWordsFilePath;
    }

    public Indexer(String searchDirectoryPath, String outputFilePath) {
        this.searchDirectoryPath = searchDirectoryPath;
        this.outputFilePath = outputFilePath;
    }

    public HashSet<String> getStopWordsSet() throws IOException {
        return new HashSet<String>(
                Util.readLinesIntoList(stopWordsFilePath));
    }

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

    public void readLinesOfFile() {
        try {
            //Map with key: word, value: a map with key: document, value: frequency
            var frequency = new HashMap<String,Map<String, Integer>>() ;

            var filesOfDir = getFileNames(searchDirectoryPath);

            for(Path file : filesOfDir){
                String filename = file.getFileName().toString();

                //Get frequencies in current file
                var frequenciesOfFile = getFrequenciesOfFile(file.toString());

                for (var frequencyOfWord : frequenciesOfFile.entrySet()){
                    String word = frequencyOfWord.getKey();

                    var currentValue = frequency.get(word);

                    if(currentValue == null){
                        var newWordMap = new HashMap<String, Integer>();
                        newWordMap.put(filename, frequencyOfWord.getValue());
                        frequency.put(word, newWordMap);
                    } else {
                        currentValue.put(filename, frequencyOfWord.getValue());
                    }

                }
            }

            frequency.keySet().forEach(System.out::println);


            String json = gson.toJson(frequency);
            printToFile(outputFilePath, json);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void findDocumentsWithAllWords(List<String> words, int minSearchWords){
        HashMap<String,Map<String, Double>> dx = gson.fromJson(readFile(outputFilePath), HashMap.class);
        HashMap<String, DocumentStats> result = new HashMap<>();

        for(String word : words){
            var docs = dx.get(word);
            for(var entry: docs.entrySet()){
                String doc = entry.getKey();

                var statEntry = result.get(doc);
                if(statEntry == null){
                    var newDocumentStat = new DocumentStats();
                    newDocumentStat.incrementSearchWords();
                    newDocumentStat.increaseTotalFrequency(entry.getValue());

                    result.put(doc, newDocumentStat);
                } else{
                    statEntry.incrementSearchWords();
                    statEntry.increaseTotalFrequency(entry.getValue());
                }
            }
        }
        HashMap<String, Integer> finalResultMap = new HashMap<>();
        for(var entry: result.entrySet()){
            if(entry.getValue().getSearchWords()>=minSearchWords){
                //Value considered by search: product of search words and total frequency
                finalResultMap.put(entry.getKey(), (int) (entry.getValue().searchWords*entry.getValue().totalFrequency));
            }
        }
        var megaResults = sortMapByValue(finalResultMap);
        megaResults.forEach(System.out::println);
    }

    class DocumentStats{
        private Double searchWords = 0.0;
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

    public Stream<Map.Entry<String, Integer>> sortMapByValue(Map<String, Integer> map ){
        Stream<Map.Entry<String, Integer>> sorted =
                map.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue());
        return sorted;
    }

    //Returns all files in the directory and subdirectories
    public Map<String, Integer> getFrequenciesOfFile(String filePath) throws IOException {
        var lines = Util.readLinesIntoList(filePath);
        return termRecognizer.termFrequency(String.join(" ",lines ));
    }

    public void printToFile(String filePath, String text){
        try (PrintWriter out = new PrintWriter(filePath)) {
            out.println(text);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public String readFile(String filePath){
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
