package com.lookify.Lookify;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.*;
import org.bson.Document;
import org.json.simple.JSONObject;
import org.springframework.web.bind.annotation.*;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.io.*;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONArray;
@RestController

public class HomeController {
    HashMap<String, Double> pop = getfromDB();
    HashMap<String, String> titles = gettitles();

    @CrossOrigin(origins = "http://localhost:4200")
    @RequestMapping(value = "/{query}", method = RequestMethod.GET)
    @ResponseBody

    public List<JSONObject> getSearchResult(@PathVariable("query") String query) throws IOException {
        QueryProcessor Q = new QueryProcessor();
        Q.QueryResult(query);
        HashMap<String, intPair> PhraseSearchingLinks = Q.PhraseSearchingLinks;
        HashSet<Document> Links = Q.words_documents;
        Ranker ranker = new Ranker(Links, Q.PhraseSearching, PhraseSearchingLinks, pop, titles,Q.words);
        List<SearchResult>SearchResults= ranker.fillWeb();
        List<JSONObject> JSONList = new ArrayList<JSONObject>();
        for (SearchResult SR : SearchResults) {
            JSONObject SRJson = new JSONObject();
            SRJson.put("title",SR.title);
            SRJson.put("link",SR.link);
            SRJson.put("paragraph",SR.paragraph);
            List<JSONObject> Bolded = new ArrayList<JSONObject>();
            JSONArray jsonArray = new JSONArray();
            for(String s:SR.indecies)
            {
                jsonArray.add(s);
            }
            SRJson.put("Bolded",jsonArray);
            JSONList.add(SRJson);
        }
        return JSONList;
    }

    public static HashMap<String, Double> getfromDB() {
        HashMap<String, Double> popularity = null;
        try {
            MongoCollection<Document> pop = MongoDB.GetCollection("pop");
            popularity = new HashMap<>();
            MongoCursor<Document> curr = pop.find().iterator();
            while (curr.hasNext()) {
                Document document = curr.next();
                String link = document.getString("link");
                Double score = document.getDouble("score");
                popularity.put(link, score);
            }
        } catch (MongoException e) {
            System.out.println("error");
        }
        return popularity;
    }

    public static HashMap<String, String> gettitles() {
        HashMap<String, String> titles = new HashMap<>() ;
        MongoCollection mongoCollection = MongoDB.GetCollection("Titles");
        MongoCursor<Document> curr = mongoCollection.find().iterator();
        while (curr.hasNext()) {
            Document doc = curr.next();
            String link = doc.getString("Site");
            String title = doc.getString("Title");

            titles.put(link, title);
        }
        return titles;
    }
}