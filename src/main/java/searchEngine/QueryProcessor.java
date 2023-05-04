package searchEngine;

import java.io.IOException;
import java.util.*;

import org.tartarus.snowball.ext.porterStemmer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.mongodb.*;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

public class QueryProcessor {
    public static List<Document> GetDocsFromDB(List<String> QueryWords) {
        List<Document> result = new Vector<>();
        MongoClient mongoClient = MongoClients.create(MongoDB.getConnectionString());
        MongoDatabase mongodb = mongoClient.getDatabase("Lookify");
        MongoCollection mongoCollection = mongodb.getCollection("InvertedIndex");
        for (String word : QueryWords) {
            Document found = (Document) mongoCollection.find(new Document("word", word)).first();
            if (found != null)
                result.add(found);
        }
        return result;
    }

    public static void main(String[] args) throws IOException {
        String Query = "yousef osama11 githubing githubed 123";
        List<String> QueryWords = StringProcessing.splitWords(Query);
        StringProcessing.ConvertToLower(QueryWords);
        StringProcessing.Stemming(QueryWords);
        StringProcessing.ReadStopWords();
        StringProcessing.RemoveStopWords(QueryWords);
        QueryWords = new ArrayList<>(new HashSet<>(QueryWords));
        for (String word : QueryWords) {
            System.out.println(word);
        }
        List<Document> words_documents = GetDocsFromDB(QueryWords);
    }
}
