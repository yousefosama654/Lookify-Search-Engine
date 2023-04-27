import org.tartarus.snowball.ext.porterStemmer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringProcessing {
    protected static List<String> StopWords;

    protected static List<String> splitWords(String Lines) {
        List<String> words = new ArrayList<String>();
        Pattern pattern = Pattern.compile("\\w+");
        Matcher match = pattern.matcher(Lines);
        while (match.find()) {
            String Word = match.group();
            if (!Word.matches("[0-9]+") && Word.length() <= 20)
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
        BufferedReader reader = new BufferedReader(new FileReader("src/main/java/StopWords.txt"));
        StopWords = new ArrayList<String>();
        String word;
        while ((word = reader.readLine()) != null) {
            StopWords.add(word);
        }
    }

    protected static void RemoveStopWords(List<String> Words) {
        Words.removeAll(StopWords);
    }

}
