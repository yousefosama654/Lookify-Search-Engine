package com.lookify.Lookify;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.twelvemonkeys.util.regex.RegExTokenIterator;
import org.bson.Document;
import org.json.simple.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;

import java.util.*;

@RestController
@RequestMapping("/history")
public class HistoryController {


    @CrossOrigin(origins = "http://localhost:4200")
    @GetMapping("/all")
    public JSONArray GetHistory() {
        MongoCollection mongoCollection = MongoDB.GetCollection("History");
        MongoCursor<Document> curr = mongoCollection.find().iterator();
        JSONArray jsonArray = new JSONArray();
        while (curr.hasNext()) {
            Document doc = curr.next();
            jsonArray.add(doc.getString("word"));
        }
        return jsonArray;
    }

    @CrossOrigin(origins = "http://localhost:4200")
    @PostMapping("/add/{query}")
    public void AddToHistory(@PathVariable String query) {
        MongoCollection mongoCollection = MongoDB.GetCollection("History");
        Document document = new Document("word", query);
        mongoCollection.insertOne(document);
    }
}
