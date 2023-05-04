package searchEngine;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.*;
import java.util.stream.Stream;

import static java.lang.Math.log;

public class Ranker {
    com.mongodb.client.MongoClient mongoClient;
    MongoCollection<Document> invertedwords;
    ArrayList<String>searchwords;
    Map<String,Double>relvence;
    public Ranker(ArrayList<String>s,HashMap<String,List<String>> visitedURLs){
        rankrelevence(s);

        rankPopularity(visitedURLs);
    }
    public  HashMap<String, Double> rankPopularity(HashMap<String,List<String>> visitedURLs) {
        HashMap<String, Double> prev = new HashMap<String, Double>();
        HashMap<String, Double> current = new HashMap<String, Double>();
        Double c=1.0/visitedURLs.size();

        for (Map.Entry<String, List<String>> entry : visitedURLs.entrySet())
        {
            String link = entry.getKey();
            prev.put(link,c);

         }

        for(int i=0;i<100;i++)
        {
            for (Map.Entry<String,Double> entry : prev.entrySet())
            {
                String link = entry.getKey();
              List<String>temp=visitedURLs.get(link);
              Double pagerank=0.0;
                for(int j=0;j<temp.size();j++)
                {

                        Double r= prev.get(temp.get(j));
                        int size=visitedURLs.get(temp.get(j)).size();
                        r=r/size;
                        pagerank+=r;


                }
                current.put(link,pagerank);

            }
            for (Map.Entry<String,Double> entry : current.entrySet())

            {
                String link = entry.getKey();
                Double t=entry.getValue();
                prev.replace(link,t);

            }

         }
        for (Map.Entry<String,Double> entry : current.entrySet()) {
            String link = entry.getKey();
            Double o=entry.getValue();
            System.out.print(link+" ");
            System.out.print(o);
            System.out.println("");

        }
        return current;



    }
    public void rankrelevence(ArrayList<String>s){
        connctToDataBase();
        relvence=new HashMap<>() ;
        searchwords=s;
        MongoCursor<Document> cur = invertedwords.find().iterator();
        while (cur.hasNext())
        {
            Document document = cur.next();
            String word = document.getString("Word");
            if(searchwords.contains(word))
            {
                int DF = document.getInteger("DF");
                double IDF = document.getDouble("IDF");

                ArrayList<Document> docList = (ArrayList<Document>) document.get("Documents");
                for (Document doc : docList) {
                    int TF = doc.getInteger("TF");
                    int Size = doc.getInteger("Size");
                    double normliziedTF=TF/(double)Size;
                    String link=doc.getString("Document");
                    int Priority= doc.getInteger("Priority");
                    double relv=Priority*normliziedTF*IDF;
                    if(relvence.containsKey(link))
                    {
                        double r=relvence.get(link);
                        r+=relv;
                        relvence.replace(link,r);
                    }
                    else {
                        relvence.put(link,relv);
                    }
                }
            }
        }
        List<Map.Entry<String,Double>> sorted =new ArrayList<>(relvence.entrySet());
        sorted.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        sorted.forEach(System.out::println);
    }

    public void connctToDataBase() {
        String connectionString = "mongodb+srv://sarahgerges01:eng8987458@cluster0.mamfgqv.mongodb.net/?retryWrites=true&w=majority";
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString)).build();
        // Create a new client and connect to the server
        try {
            mongoClient = MongoClients.create(settings);
            // Send a ping to confirm a successful connection
            MongoDatabase database = mongoClient.getDatabase("Lookify");
            // database.runCommand(new Document("ping", 1));
            invertedwords = database.getCollection("InvertedIndex");
            System.out.println("Pinged your deployment. You successfully connected to MongoDB!");

        } catch (MongoException e) {
            System.out.println("error");
        }
    }
}
