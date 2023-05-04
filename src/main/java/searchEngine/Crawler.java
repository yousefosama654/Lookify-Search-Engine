package searchEngine;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.json.simple.JSONObject;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.exists;
public class Crawler implements Runnable {
    private Queue<String> urlQueue;
    public HashMap<String,List<String>> visitedURLs;

    private List<String> normlizedurl;
    private int breakpoint;
    private int thread_num;
    private Thread[] MyThreads;
    MongoCollection<Document> links;
    MongoCollection<Document> BreakPoint;
    com.mongodb.client.MongoClient mongoClient;

    public  List<String> GetSeeds() {
        List<String> HtmlLinks = new ArrayList<String>();
        String line;
        File file=new File("C:\\Users\\sggln\\Downloads\\Lookify\\Lookify\\Lookify-Search-Engine\\src\\main\\java\\searchEngine\\Seeds.txt");
        try {

            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            while ((line = bufferedReader.readLine()) != null) {
                HtmlLinks.add(line);
            }
            bufferedReader.close();
        } catch (Exception e) {
            System.out.println("file not found");
            e.printStackTrace();
        }
        return HtmlLinks;
    }

    public void WriteSeeds(String url) {
        try {
            FileWriter writer = new FileWriter("C:\\Users\\sggln\\Downloads\\Lookify\\Lookify\\Lookify-Search-Engine\\src\\main\\java\\searchEngine\\Seeds.txt",true);
            writer.write(url);
            writer.write("\r\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Crawler(int n, int b) {
        //// connection with data base
        connctToDataBase();
        //////////////////
        urlQueue = new LinkedList<String>();
        visitedURLs = new HashMap<String,List<String>>();

        normlizedurl = new ArrayList<String>();
        thread_num = n;
        ///// fill from data base or not

        if (fillFromDB()) {
            List<String> HTMLSeed = GetSeeds();
            for (String seed : HTMLSeed)
            {
                urlQueue.add(seed);
                normlizedurl.add(normalized(seed));
                List<String>temp=new ArrayList<String>();
                visitedURLs.put(seed,temp);

                List<JSONObject> List = new ArrayList<JSONObject>();

                Document doc1 = new Document("links", seed).append("normlizied", normalized(seed)).append("removed", "0").append("List",List);
                links.insertOne(doc1);
           }
            Document doc = new Document("BreakPoint", b);
            BreakPoint.insertOne(doc);
            this.breakpoint = b;
        }
        MyThreads = new Thread[thread_num];
        for (int i = 0; i < thread_num; i++) {
            MyThreads[i] = new Thread(this);
            MyThreads[i].setName(Integer.toString(i));
           MyThreads[i].start();
        }

    }

    public void crawl() {

        String s = "";
        while (true) {

            // remove the next url string from the queue to begin traverse.
            if (getBreakPoint() == 0) {
                break;
            }
            synchronized (this.urlQueue) {
                while (urlQueue.isEmpty()) {
                    try {
                        urlQueue.wait();
                    } catch (InterruptedException e) {
                        continue;
                    }
                }
                s = urlQueue.remove();
                Document filter = new Document("links", s);

                // Define the update operation(s) to apply to the matched document(s)
                Document update = new Document("$set", new Document("removed", "1"));

                links.updateOne(filter, update);

            }

            String rawHTML = "";
            try {
                // create url with the string.
                URL url = new URL(s);
                BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
                String inputLine = in.readLine();

                // read every line of the HTML content in the URL
                // and concat each line to the rawHTML string until every line is read.

                while (inputLine != null) {

                    rawHTML += inputLine;

                    inputLine = in.readLine();
                }

                in.close();
            } catch (Exception e) {
                // e.printStackTrace();
                continue;
            }

            // create a regex pattern matching a URL
            // that will validate the content of HTML in search of a URL.
            //// look at it later (link pattern)
            String urlPattern = "(www|http:\\/\\/|https:\\/\\/)+[^\\s]+[\\w]";
            Pattern pattern = Pattern.compile(urlPattern);
            Matcher matcher = pattern.matcher(rawHTML);

            // Each time the regex matches a URL in the HTML,
            // add it to the queue for the next traverse and the list of visited URLs.
            getBreakpoint(matcher,s);

            // exit the outermost loop if it reaches the breakpoint.
            if (getBreakPoint() <= 0) {
                break;
            }
        }

    }
//
    private void getBreakpoint(Matcher matcher,String parent) {

        outerLoop:
        while (matcher.find() && getBreakPoint() > 0) {
            String actualURL = matcher.group();
            URL url = null;
            URL urlRobot = null;
            String strHost = "";
            /////////////////////////////////////////
            try {
                url = new URL(actualURL);
                strHost = url.getHost();
                String strRobot = "http://" + strHost + "/robots.txt";
                urlRobot = new URL(strRobot);

            } catch (MalformedURLException e) {
				e.printStackTrace();
                continue;
            }

            BufferedReader input;
            if (urlRobot != null) {
                try {
                    input = new BufferedReader(new InputStreamReader(urlRobot.openStream()));
                    String Line = input.readLine();
                    while (Line != null) {
                        if (Line.equals("User-agent: *"))
                            break;
                        else
                            Line = input.readLine();
                    }

                    Line = input.readLine();
                    while (Line != null && (Line.startsWith("Disallow") || Line.startsWith("Allow"))) {
                        if (Line.startsWith("Allow")) {
                            Line = input.readLine();
                            continue;
                        } else {
                            int start = Line.indexOf(":") + 1;
                            int end = Line.length();
                            String rule = Line.substring(start, end);
                            rule.trim();
                            if (rule.length() == 0)
                                break;
                            else if (rule.equals("/"))
                                continue outerLoop;

                            //// disallow **
                            if (rule.startsWith("") || rule.endsWith("")) {
                                if (rule.startsWith("") && rule.endsWith("")) {
                                    int start1 = rule.indexOf("*") + 1;
                                    int end1 = rule.length() - 1;
                                    String r = rule.substring(start1, end1);
                                    if (actualURL.contains(r))
                                        continue outerLoop;
                                } else if (rule.startsWith("*")) {
                                    int start1 = rule.indexOf("*") + 1;
                                    int end1 = rule.length();
                                    String r = rule.substring(start1, end1);
                                    if (actualURL.endsWith(r))
                                        continue outerLoop;
                                } else if (rule.endsWith("*")) {
                                    int end1 = rule.length() - 1;
                                    String r = rule.substring(0, end1);
                                    if (actualURL.startsWith(r))
                                        continue outerLoop;
                                }
                            }

                            //// disallow exitions
                            else if (rule.startsWith("/*")) {
                                int start1 = rule.indexOf(".") + 1;
                                int end1 = rule.length() - 1;
                                String r = rule.substring(start1, end1);
                                if (actualURL.endsWith(r))
                                    continue outerLoop;
                            }
                            //// disallow folder or file
                            else if (rule.startsWith("/")) {
                                String r = strHost + rule;
                                if (actualURL.startsWith(r))
                                    continue outerLoop;
                            }

                            Line = input.readLine();
                        }
                    }

                } catch (IOException e) {
				e.printStackTrace();
                    continue;
                }
            }
            /////////////////////////////////////////

            String normlized = normalized(actualURL);

            if (!normlizedurl.contains(normlized) && !normlized.equals("")) {

                synchronized (this.normlizedurl) {
                    normlizedurl.add(normlized);
                }
            } else {
                continue;
            }

            if (!visitedURLs.containsKey(actualURL)) {

                if (getBreakPoint() <= 0) {
                    break;
                }

                synchronized (this.visitedURLs) {
                    List<String>temp=new ArrayList<String>();
                    temp.add(parent);
                    visitedURLs.put(actualURL,temp);
                    this.WriteSeeds(actualURL);
                    Document doc = new Document("links", actualURL).append("normlizied", normlized).append("removed", "0").append("List", temp);
                    links.insertOne(doc);
                }
                System.out.println("From thread" + Thread.currentThread().getName() + " Website found with URL "
                        + actualURL);
                synchronized (this.urlQueue) {
                    urlQueue.add(actualURL);
                    urlQueue.notifyAll();
                }

                decreamentBreakPoint();
            }
            else {

               List<String>temp= visitedURLs.get(actualURL);
                Document filter = new  Document("links", actualURL).append("normlizied", normlized).append("removed", "0").append("List",temp);
               temp.add(parent);
               visitedURLs.replace(actualURL,temp);


                // Define the update operation(s) to apply to the matched document(s)
                Document update =  new Document("links", actualURL).append("normlizied", normlized).append("removed", "0").append("List",temp);
                links.updateOne(filter, update);
            }
            }

            // exit the loop if it reaches the breakpoint.

        }


//
    public String normalized(String actualURL) {
        String rawHTML = "";
        String normlized = "";

        try {
            // create url with the string.
            URL url = new URL(actualURL);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String inputLine = in.readLine();

            // read every line of the HTML content in the URL
            // and concat each line to the rawHTML string until every line is read.
            while (inputLine != null) {
                int l = inputLine.length();
                rawHTML += inputLine;
                normlized += inputLine.charAt(l / 2);
                inputLine = in.readLine();
            }

            in.close();

        } catch (Exception e) {
            // e.printStackTrace();
        }
        return normlized;

    }
//
    public void run() {
       crawl();
    }

    public synchronized int getBreakPoint() {
        return breakpoint;
    }

    public synchronized void decreamentBreakPoint() {
        this.breakpoint--;
//		System.out.println("   breakpoint =" + getBreakPoint());
        Document filter = new Document("BreakPoint", this.breakpoint + 1);

        // Define the update operation(s) to apply to the matched document(s)
        Document update = new Document("$set", new Document("BreakPoint", this.breakpoint));

        BreakPoint.updateOne(filter, update);
    }

    public void JoinAll() {
        for (int i = 0; i < thread_num; i++) {
            try {
                MyThreads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void connctToDataBase() {
        String connectionString = "mongodb+srv://sarahgerges01:eng8987458@cluster0.mamfgqv.mongodb.net/?retryWrites=true&w=majority";
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString)).build();
        // Create a new client and connect to the server

        try {
            mongoClient = MongoClients.create(settings);
            // Send a ping to confirm a successful connection
            MongoDatabase database = mongoClient.getDatabase("Crawler");
            // database.runCommand(new Document("ping", 1));
            links = database.getCollection("links");
            BreakPoint = database.getCollection("BreakPoint");

            System.out.println("Pinged your deployment. You successfully connected to MongoDB!");

        } catch (MongoException e) {
            System.out.println("error");
        }
    }

//    ///////////////////////////////////////////////////
    public boolean fillFromDB()
    {
        int count = 0;
        MongoCursor<Document> cur = links.find().iterator();
        while (cur.hasNext()) {
            Document document = cur.next();
            String y = document.getString("removed");
            String norm = document.getString("normlizied");
            String url = document.getString("links");
            ArrayList<Document> docList = (ArrayList<Document>) document.get("List");
            if (y.equals("0")) {
                urlQueue.add(url);
                List<String>temp=new ArrayList<String>();

                for (Document doc : docList) {
                    temp.add(doc.getString("link"));

                }
                visitedURLs.put(url,temp);

            } else {
                List<String>temp=new ArrayList<String>();

                for (Document doc : docList) {
                    temp.add(doc.getString("link"));

                }
                visitedURLs.put(url,temp);
            }
            normlizedurl.add(norm);
            // Do something with the document
            count++;
        }
        if (count == 0)
            return true;
        else {
            MongoCursor<Document> cur1 = BreakPoint.find().iterator();


            while (cur1.hasNext()) {
                Document document = cur1.next();
                int y = document.getInteger("BreakPoint");
                this.breakpoint = y;

            }

            return false;
        }
    }
    protected void finish() {

        links.deleteMany(exists("_id"));
        BreakPoint.deleteMany(exists("_id"));

    }

}
