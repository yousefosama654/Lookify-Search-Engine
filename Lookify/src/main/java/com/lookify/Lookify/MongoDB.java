package com.lookify.Lookify;

import com.mongodb.*;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class MongoDB {
    public static String ConnectionString;

    public static String getConnectionString() {
        String line = "";
        try {
            FileReader reader = new FileReader("ConnectionString.txt");
            BufferedReader bufferedReader = new BufferedReader(reader);
            line = bufferedReader.readLine();
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return line;
    }

    public static void RemoveCollection(String collectionName) {
        MongoClient mongoClient = MongoClients.create(MongoDB.getConnectionString());
        MongoDatabase mongodb = mongoClient.getDatabase("Lookify");
        MongoCollection mongoCollection = mongodb.getCollection(collectionName);
        mongoCollection.drop();
    }

    public static MongoCollection GetCollection(String collectionName) {
        MongoClient mongoClient = MongoClients.create(MongoDB.getConnectionString());
        MongoDatabase mongodb = mongoClient.getDatabase("Lookify");
        return mongodb.getCollection(collectionName);
    }
}
