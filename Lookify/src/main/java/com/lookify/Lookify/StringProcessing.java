package com.lookify.Lookify;

import org.tartarus.snowball.ext.porterStemmer;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class StringProcessing {
    protected static List<String> StopWords;

    protected static List<String> splitWords(String Lines) {
        List<String> words = new ArrayList<String>();
        Pattern pattern = Pattern.compile("\\w+");
        Matcher match = pattern.matcher(Lines);
        while (match.find()) {
            String Word = match.group();
            if (Word.length() <= 20)
                words.add(Word);
        }
        return words;
    }

    protected static void ConvertToLower(List<String> Words) {
        for (int i = 0; i < Words.size(); i++) {
            Words.set(i, Words.get(i).toLowerCase());
        }
    }

    protected static void Stemming(List<String> Words) {
        porterStemmer stemmer = new porterStemmer();
        for (int i = 0; i < Words.size(); i++) {
            stemmer.setCurrent(Words.get(i));
            stemmer.stem();
            Words.set(i, stemmer.getCurrent());
        }
    }

    protected static void ReadStopWords() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader("StopWords.txt"));
        StopWords = new ArrayList<String>();
        String word;
        while ((word = reader.readLine()) != null) {
            StopWords.add(word);
        }
    }

    protected static void RemoveStopWords(List<String> Words) {
        Words.removeAll(StopWords);
    }


    protected static LinkedHashMap<String, String> splitForLogic(String Query) {
        ArrayList<String> Quereies = new ArrayList<String>(Arrays.asList(Query.split("\\b(OR|AND|NOT)\\b")));
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < Quereies.size(); i++) {
            String temp1 = Quereies.get(i) + "OR";
            String temp2 = Quereies.get(i) + "AND";
            String temp3 = Quereies.get(i) + "NOT";
            if (Query.contains(temp1)) {
                result.put(Quereies.get(i), "OR");
            } else if (Query.contains(temp2)) {
                result.put(Quereies.get(i), "AND");
            } else if (Query.contains(temp3)) {
                result.put(Quereies.get(i), "NOT");
            } else {
                result.put(Quereies.get(i), " ");
            }
        }
        return result;
    }
}
