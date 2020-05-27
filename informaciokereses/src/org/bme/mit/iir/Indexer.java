package org.bme.mit.iir;

import com.google.gson.Gson;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Indexer {
    private String searchDirectoryPath="../res/corpus";
    private String outputFilePath="indices.json";
    private String stopWordsFilePath="../res/stopwords.txt";
    private Map<String, Map<String, Integer>> indexMap = new HashMap<>();
    private Util util = new Util();
    private TermRecognizer termRecognizer;

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

            var map = frequency.get("baba");

            for (Map.Entry<String, Integer> entry : map.entrySet() ) {
                System.out.println(entry.getKey() + ":" + entry.getValue());
            }

            Gson gson = new Gson();
            String json = gson.toJson(frequency);
            printToFile(outputFilePath, json);

        } catch (IOException e) {
            e.printStackTrace();
        }
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
}
