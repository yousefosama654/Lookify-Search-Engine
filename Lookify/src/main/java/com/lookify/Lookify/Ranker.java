package com.lookify.Lookify;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.json.simple.JSONObject;

import java.util.*;


public class Ranker {
    com.mongodb.client.MongoClient mongoClient;
    MongoCollection<Document> invertedwords;
    HashSet<String> querywords;
    HashMap<String, Double> relvence;
    HashMap<String, intPair> phraseSearchingLinks;
    HashSet<Document> words_documents;
    HashMap<String, Double> pop;
    HashMap<String, intPair> snippets;
    HashMap<String, String> titles;
    HashMap<String, Double> result;

    public Ranker(HashSet<Document> words_documents, boolean flag, HashMap<String, intPair> phraseSearchingLinks, HashMap<String, Double> pop, HashMap<String, String> titles,HashSet<String>Words) {
        snippets = new HashMap<>();
        querywords = new HashSet<String>();
        if(flag) querywords.addAll(Words);
        else
        {
            Iterator<Document> i = words_documents.iterator();
            while (i.hasNext())
            {
                querywords.add(i.next().getString("Word"));
            }

        }

        this.titles = titles;
        this.pop = pop;
        result = new HashMap<String, Double>();
        this.words_documents = words_documents;
        this.phraseSearchingLinks = phraseSearchingLinks;
        if (flag == false) rankrelevence(words_documents);
        result(flag);
    }

    public void rankrelevence(HashSet<Document> words_documents) {
        relvence = new HashMap<>();


        Iterator<Document> itr = words_documents.iterator();


        while (itr.hasNext()) {
            Document d = itr.next();
            int DF = d.getInteger("DF");
            String word = d.getString("Word");
            Double IDF = d.getDouble("IDF");
            if (checkIfNumber(word)) {
                IDF = IDF * 0.001;
            }
            ArrayList<Document> docList = (ArrayList<Document>) d.get("Documents");
            for (Document doc : docList) {
                int TF = doc.getInteger("TF");
                int Size = doc.getInteger("Size");
                double normliziedTF = TF / (double) Size;
                if (normliziedTF > 0.5) continue;
                String link = doc.getString("Document");
                double Priority = doc.getInteger("Priority")*0.01;
                double relv = Priority+10* normliziedTF * IDF;
                if (relvence.containsKey(link)) {
                    double r = relvence.get(link);
                    r += relv;
                    relvence.replace(link, r);
                } else {
                    relvence.put(link, relv);
                }


                ArrayList<Document> paragraph = (ArrayList<Document>) doc.get("Paragraphs");
                int max = 0;
                intPair maxsnippet = new intPair(" ",-1);
                String snippet = " ";
                for (Document para : paragraph) {
                    snippet = para.getString("link");
                    List<String> Words = StringProcessing.splitWords(snippet);
                    StringProcessing.ConvertToLower(Words);
                    StringProcessing.Stemming(Words);
                    int j = 0;
                    for (String w : querywords) {
                        if (Words.contains(w) && !checkIfNumber(w)) {
                            j++;


                        }

                    }
                    if (j > max) {
                        max = j;
                        maxsnippet.first = snippet;
                        maxsnippet.second=j;


                    }

                }
                if(!snippets.containsKey(link))
                {   if(!checkIfNumber(word))
                    snippets.put(link, maxsnippet);
                else
                {
                    intPair p=new intPair(snippet,-1);
                    snippets.put(link,p);
                }
                }
                else {
                    intPair p=snippets.get(link);
                    if(p.second<maxsnippet.second)
                        snippets.put(link, maxsnippet);

                }            }


        }

//        List<Map.Entry<String, Double>> sorted = new ArrayList<>(relvence.entrySet());
//        sorted.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
//        sorted.forEach(System.out::println);

    }

    public boolean checkIfNumber(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void connctToDataBase() {
//        String connectionString = "mongodb+srv://sarahgerges01:eng8987458@cluster0.mamfgqv.mongodb.net/?retryWrites=true&w=majority";
//        MongoClientSettings settings = MongoClientSettings.builder()
//                .applyConnectionString(new ConnectionString(connectionString)).build();
        // Create a new client and connect to the server
        try {
//            mongoClient = MongoClients.create(settings);
            // Send a ping to confirm a successful connection
//            MongoDatabase database = mongoClient.getDatabase("Lookify");
            invertedwords = MongoDB.GetCollection("InvertedIndex");
            System.out.println("Pinged your deployment. You successfully connected to MongoDB!");
        } catch (MongoException e) {
            System.out.println("error");
        }
    }

    public void result(boolean flag) {

        if (flag) {

            for (String link : phraseSearchingLinks.keySet()) {

                Double popul = pop.get(link);
                intPair p = phraseSearchingLinks.get(link);
                Double r=popul * p.second;
                if(!Double.isNaN(r))
                    result.put(link, r);
                else result.put(link, 0.0);
                intPair pair=new intPair(p.first,0);
                snippets.put(link,pair);


            }
        } else {

            Iterator<Document> itr = words_documents.iterator();

            while (itr.hasNext()) {
                ArrayList<Document> docList = (ArrayList<Document>) itr.next().get("Documents");
                for (Document doc : docList) {
                    String link = doc.getString("Document");

                    Double popul = pop.get(link);
                    Double relv = relvence.get(link);
                    Double r=popul + 100*relv;
                    if(!Double.isNaN(r)) {
                        result.put(link, popul + 100 * relv);
                    }else
                    {
                        result.put(link, 0.0);
                    }

                }
            }
        }

    }

    public List<SearchResult> fillWeb() {
        List<SearchResult> finalresult = new ArrayList<SearchResult>();
        List<Map.Entry<String, Double>> sorted = new ArrayList<>(result.entrySet());
        sorted.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        for (int k = 0; k < sorted.size(); k++) {
            Map.Entry<String, Double> entry = sorted.get(k);
            String key = entry.getKey();
            Double value = entry.getValue();
            String para = snippets.get(key).first;
            HashSet<String> Bolded = new HashSet<>();
            List<String> Words = StringProcessing.splitWords(para);

            for (int i = 0; i < Words.size(); i++) {
                List<String> w = new ArrayList<>();
                w.add(Words.get(i));
                StringProcessing.ConvertToLower(w);
                StringProcessing.Stemming(w);
                if (querywords.contains(w.get(0))) {
                    Bolded.add(Words.get(i));
                }
            }
            List<String>bold=new ArrayList<String>(Bolded);
            SearchResult toshow = new SearchResult(key, para, titles.get(key), bold);
            finalresult.add(toshow);
        }


        for (int k = 0; k < sorted.size(); k++) {
            Map.Entry<String, Double> entry = sorted.get(k);
            System.out.println(entry.getKey()+"-----------"+entry.getValue());
        }
        return finalresult;

    }

}


