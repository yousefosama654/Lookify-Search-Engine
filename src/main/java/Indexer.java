import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import org.bson.Document;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.tartarus.snowball.ext.porterStemmer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.json.simple.JSONObject;

public class Indexer {
    public static List<String> GetSeeds() {
        List<String> HtmlLinks = new ArrayList<String>();
        String line;
        try {
            FileReader reader = new FileReader("src/main/java/Seeds.txt");
            BufferedReader bufferedReader = new BufferedReader(reader);
            while ((line = bufferedReader.readLine()) != null) {
                HtmlLinks.add(line);
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
//        HtmlLinks.remove(HtmlLinks.size() - 1);
        return HtmlLinks;
    }

    public static void UploadtoDB(List<JSONObject> invertedIndexJSONParameter) {
        MongoCollection mongoCollection = MongoDB.GetCollection("InvertedIndex");
        MongoDB.RemoveCollection("InvertedIndex");
        for (int i = 0; i < invertedIndexJSONParameter.size(); i++) {
            org.bson.Document doc = new org.bson.Document(invertedIndexJSONParameter.get(i));
            mongoCollection.insertOne(doc);
        }
    }

    public static int GetPriority(String Tag) {
        if (Tag.equals("title")) {
            return 10;
        } else if (Tag.equals("h1")) {
            return 9;
        } else if (Tag.equals("h2")) {
            return 8;
        } else if (Tag.equals("h3")) {
            return 7;
        } else if (Tag.equals("h4")) {
            return 6;
        } else if (Tag.equals("h5")) {
            return 5;
        } else if (Tag.equals("h6")) {
            return 4;
        } else if (Tag.equals("p")) {
            return 3;
        } else if (Tag.equals("div")) {
            return 2;
        } else {
            return 1;
        }
    }

    public static String GetText(String Link, HashMap<String, Integer> WordsPrioity) {
        String text = "";
        try {
            // Create a Jsoup Document object by connecting to the URL
            org.jsoup.nodes.Document doc = Jsoup.connect(Link).get();
            // Get all elements in the HTML document
            Elements elements = doc.getAllElements();
            // Create a list to store elements without children
            List<Element> elementsWithoutChildren = new ArrayList<>();
            // Loop over each element in the Elements collection
            for (Element element : elements) {
                // Check if the element has any child elements
                if (element.children().isEmpty()) {
                    // If the element does not have any child elements, it is a leaf node in the document tree
                    elementsWithoutChildren.add(element);
                }
            }
            // Get the text content of each element and remove the HTML tags
            StringBuilder sb = new StringBuilder();
            for (Element element : elementsWithoutChildren) {
                String type = element.tagName();
                String ElementText = element.text();
                int priority = GetPriority(type);
                List<String> Words = StringProcessing.splitWords(ElementText);
                StringProcessing.ConvertToLower(Words);
                StringProcessing.Stemming(Words);
                StringProcessing.RemoveStopWords(Words);
                for (String word : Words) {
                    if (word.isEmpty()) {
                        continue;
                    }
                    if (WordsPrioity.containsKey(word)) {
                        WordsPrioity.put(word, Math.max(priority, WordsPrioity.get(word)));
                    } else
                        WordsPrioity.put(word, priority);
                    sb.append(word);
                    sb.append(" ");
                }
            }
            text = sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text;
    }

    /*
     * The InvertedIndex DataStructure
     * HashMap<String1, HashMap<String2, Pair>> InvertedIndex
     * String1 is the stemmed word
     * String2 is the name of current document file
     * Pair is structure holds some info about word in this document
     * TF occurrences of this word in the document
     * Size is the Size of stemmed words in this document
     */
    public static void BuildInvertedIndex(List<String> Words, String DocumentName, HashMap<String, HashMap<String, Pair>> InvertedIndex, HashMap<String, Integer> WordsPrioity) {
        for (int i = 0; i < Words.size(); i++) {
            String Word = Words.get(i);
            if (!InvertedIndex.containsKey(Word)) {
                HashMap<String, Pair> DocsMappedtoWord = new HashMap<String, Pair>();
                InvertedIndex.put(Word, DocsMappedtoWord);
            }
            HashMap<String, Pair> DocsMappedtoWord = InvertedIndex.get(Word);
            if (!DocsMappedtoWord.containsKey(DocumentName)) {
                Pair DocumentPair = new Pair(0, Words.size(), WordsPrioity.get(Word));
                DocsMappedtoWord.put(DocumentName, DocumentPair);
            }
            Pair DocumentPair = DocsMappedtoWord.get(DocumentName);
            DocumentPair.TF++;
        }
    }

    private static List<JSONObject> convertInvertedIndexToJSON(HashMap<String, HashMap<String, Pair>> InvertedIndex, int NumberOfDocuments) {
        List<JSONObject> JSONList = new ArrayList<JSONObject>();
        for (String Word : InvertedIndex.keySet()) {
            if (Word.isEmpty()) continue;
            JSONObject WordJSON = new JSONObject();
            List<JSONObject> documents = new ArrayList<JSONObject>();
            WordJSON.put("Word", Word);
            WordJSON.put("DF", InvertedIndex.get(Word).size());
            double IDF = Math.log((double) NumberOfDocuments / InvertedIndex.get(Word).size());
            WordJSON.put("IDF", IDF);
            for (String Doc : InvertedIndex.get(Word).keySet()) {
                JSONObject DocumentJSON = new JSONObject();
                DocumentJSON.put("Document", Doc);
                DocumentJSON.put("TF", InvertedIndex.get(Word).get(Doc).TF);
                DocumentJSON.put("Size", InvertedIndex.get(Word).get(Doc).Size);
                DocumentJSON.put("Priority", InvertedIndex.get(Word).get(Doc).Priority);
                documents.add(DocumentJSON);
            }
            WordJSON.put("Documents", documents);
            JSONList.add(WordJSON);
        }
        return JSONList;
    }

    public static void StartIndexing() throws IOException {
        List<String> HtmlLinks = GetSeeds();
        StringProcessing.ReadStopWords();
        HashMap<String, HashMap<String, Pair>> InvertedIndex = new HashMap<>();
        HashMap<String, Integer> WordsPrioity = new HashMap<>();
        for (String Link : HtmlLinks) {
            String Text = GetText(Link, WordsPrioity);
            List<String> Words = StringProcessing.splitWords(Text);
            BuildInvertedIndex(Words, Link, InvertedIndex, WordsPrioity);
        }
        List<JSONObject> JSONList = convertInvertedIndexToJSON(InvertedIndex, HtmlLinks.size());
        UploadtoDB(JSONList);
    }
}
