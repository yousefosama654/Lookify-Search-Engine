package com.lookify.Lookify;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.*;

public class Popularity {
    public static HashMap<String, Double> rankPopularity(HashMap<String, Set<String>> visitedURLs) {
        HashMap<String, Double> prev = new HashMap<String, Double>();
        HashMap<String, Double> current = new HashMap<String, Double>();
        Double c = 1.0 / visitedURLs.size();
        for (Map.Entry<String, Set<String>> entry : visitedURLs.entrySet()) {
            String link = entry.getKey();
            prev.put(link, c);
        }
        for (int i = 0; i < 100; i++) {
            for (Map.Entry<String, Double> entry : prev.entrySet()) {
                String link = entry.getKey();
                Set<String> temp = visitedURLs.get(link);
                Double pagerank = 0.0;
                for (String s:temp) {
                    Double r;
                    int size;
                    if (s.equals(" ")) {
                        size = visitedURLs.size();
                        r = 1.0;
                    } else {
                        size = visitedURLs.get(s).size();
                        r = prev.get(s);
                    }
                    r = r / size;
                    pagerank += r;
                }
                current.put(link, pagerank);
            }
            for (Map.Entry<String, Double> entry : current.entrySet()) {
                String link = entry.getKey();
                Double t = entry.getValue();
                prev.replace(link, t);
            }
        }
        List<Document> scores = new ArrayList<>();
        for (Map.Entry<String, Double> entry : current.entrySet()) {
            String link = entry.getKey();
            Double o = entry.getValue();
            Document doc1 = new Document("link", entry.getKey()).append("score", entry.getValue());
            scores.add(doc1);
        }
        connectdb(scores);
        return current;
    }

    public static void connectdb(List<Document> scores) {
        try {
            MongoDB.RemoveCollection("pop");
            MongoCollection<Document> pop = MongoDB.GetCollection("pop");

            System.out.println("Pinged your deployment. You successfully connected to MongoDB!");
            pop.insertMany(scores);

        } catch (MongoException e) {
            System.out.println("error");
        }
    }
}