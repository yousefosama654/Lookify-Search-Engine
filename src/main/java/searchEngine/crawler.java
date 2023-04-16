package searchEngine;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.exists;

public class crawler implements Runnable {
    private Queue<String> urlQueue;
    private List<String> visitedURLs;
    private List<String> normlizedurl;
    private int breakpoint;
    private int thread_num;
    private Thread[] MyThreads;
    MongoCollection<Document> links;
    MongoCollection<Document> BreakPoint;
    com.mongodb.client.MongoClient mongoClient;

    public crawler(int n, int b) {

        //// connection with data base
        connctToDataBase();
        //////////////////
        urlQueue = new LinkedList<String>();
        visitedURLs = new ArrayList<String>();
        normlizedurl = new ArrayList<String>();
        thread_num = n;
        ///// fill from data base or not
        if (fillFromDB()) {
            urlQueue.add("https://www.usatoday.com/news-sitemap.xml");
            urlQueue.add("https://www.theguardian.com/sitemaps/news.xml");
            urlQueue.add("https://www.bbc.co.uk/bitesize/");

            normlizedurl.add(normalized("https://www.usatoday.com/news-sitemap.xml"));
            normlizedurl.add(normalized("https://www.theguardian.com/sitemaps/news.xml"));
            normlizedurl.add(normalized("https://www.bbc.co.uk/bitesize/"));

            visitedURLs.add("https://www.usatoday.com/news-sitemap.xml");
            visitedURLs.add("https://www.theguardian.com/sitemaps/news.xml/");
            visitedURLs.add("https://www.bbc.co.uk/bitesize/");

            Document doc1=new Document("links","https://www.usatoday.com/news-sitemap.xml").append("normlizied", normalized("https://www.usatoday.com/news-sitemap.xml")).append("removed", "0");
            links.insertOne(doc1);
            Document doc2=new Document("links","https://www.theguardian.com/sitemaps/news.xml").append("normlizied", normalized("https://www.theguardian.com/sitemaps/news.xml")).append("removed", "0");
            links.insertOne(doc2);
            Document doc3=new Document("links","https://www.bbc.co.uk/bitesize/").append("normlizied", normalized("https://www.bbc.co.uk/bitesize/")).append("removed", "0");
            links.insertOne(doc3);


            Document doc=new Document("BreakPoint",b);
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

    ///////////////////////////////////////////////////
    public void crawl() {

        String s = "";
        while (true) {

            // remove the next url string from the queue to begin traverse.
            if (getBreakPoint() == 0) {
                break;
            }
            synchronized (this.urlQueue)
            {
                while (urlQueue.isEmpty() ) {
                    try {
                        urlQueue.wait();
                    } catch (InterruptedException e) {
                        continue;
                    }
                }
                s = urlQueue.peek();

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
            getBreakpoint(matcher);

            // exit the outermost loop if it reaches the breakpoint.
            if (getBreakPoint() == 0) {
                break;
            }
        }

    }

    ///////////////////////////////////////////
    private void getBreakpoint(Matcher matcher) {

        outerLoop: while (matcher.find() && getBreakPoint() > 0) {
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
                // TODO Auto-generated catch block
//				e.printStackTrace();
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
                        }

                        else {
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
                                }

                                else if (rule.startsWith("*")) {
                                    int start1 = rule.indexOf("*") + 1;
                                    int end1 = rule.length();
                                    String r = rule.substring(start1, end1);
                                    if (actualURL.endsWith(r))
                                        continue outerLoop;
                                }

                                else if (rule.endsWith("*")) {
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
                    // TODO Auto-generated catch block
//				e.printStackTrace();
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

            if (!visitedURLs.contains(actualURL)) {

                if (getBreakPoint() == 0) {
                    break;
                }

                synchronized (this.visitedURLs) {
                    visitedURLs.add(actualURL);
                    Document doc=new Document("links",actualURL).append("normlizied", normlized).append("removed", "0");
                    links.insertOne(doc);
                }
                System.out.println("From thread" + Thread.currentThread().getName() + " Website found with URL "
                        + actualURL );
                synchronized (this.urlQueue) {
                    urlQueue.add(actualURL);
                    urlQueue.notifyAll();
                }

                decreamentBreakPoint();
            }

            // exit the loop if it reaches the breakpoint.

        }

       String s = urlQueue.remove();
        Document filter = new Document("links", s);

        // Define the update operation(s) to apply to the matched document(s)
        Document update = new Document("$set", new Document("removed", "1"));

        links.updateOne(filter, update);
    }

    ///////////////////////////////////////////////////
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

    ///////////////////////////////////////////
    public void run() {
        // TODO Auto-generated method stub
        crawl();

    }

    public synchronized int getBreakPoint() {
        return breakpoint;
    }
    //////////////////////
    public synchronized void decreamentBreakPoint() {
        this.breakpoint--;
//		System.out.println("   breakpoint =" + getBreakPoint());
        Document filter = new Document("BreakPoint", this.breakpoint+1);

        // Define the update operation(s) to apply to the matched document(s)
        Document update = new Document("$set", new Document("BreakPoint", this.breakpoint));

        BreakPoint.updateOne(filter, update);
    }

    public void JoinAll() {
        for (int i = 0; i < thread_num; i++) {
            try {
                MyThreads[i].join();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
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
    ///////////////////////////////////////////////////
    public boolean fillFromDB() {
        int count = 0;

        MongoCursor<Document> cur = links.find().iterator();



        while (cur.hasNext()) {

            Document document = cur.next();
            String y = document.getString("removed");
            String norm = document.getString("normlizied");
            String url = document.getString("links");

            if (y.equals("0")) {
                urlQueue.add(url);
                visitedURLs.add(url);

            } else {
                visitedURLs.add(url);
            }
            normlizedurl.add(norm);
            // Do something with the document
            count++;

        }
        if (count == 0)
            return true;
        else
        {
            MongoCursor<Document> cur1 = BreakPoint.find().iterator();


            while (cur1.hasNext())
            {
                Document document = cur1.next();
                int y = document.getInteger("BreakPoint");
                this.breakpoint=y;

            }

            return false;
        }
    }

    protected void finish()
    {

        links.deleteMany(exists("_id"));
        BreakPoint.deleteMany(exists("_id"));

    }

}

//public void crawl( ) {
////  urlQueue.add(rootURL);
////  visitedURLs.add(rootURL);
//
//	while(urlQueue.isEmpty() );
//
//  while(!urlQueue.isEmpty() ){
//
//
//      // remove the next url string from the queue to begin traverse.
//
//  	synchronized (this.urlQueue)
//  	{
//      String s = urlQueue.remove();
//  	}
//
//      String rawHTML = "";
//      try{
//          // create url with the string.
//          URL url = new URL(s);
//          BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
//          String inputLine = in.readLine();
//
//          // read every line of the HTML content in the URL
//          // and concat each line to the rawHTML string until every line is read.
//
//          while(inputLine  != null){
//
//              rawHTML += inputLine;
//
//              inputLine = in.readLine();
//          }
//
//          in.close();
//      } catch (Exception e){
//          //e.printStackTrace();
//      }
//
//      // create a regex pattern matching a URL
//      // that will validate the content of HTML in search of a URL.
//      String urlPattern = "(www|http:\\/\\/|https:\\/\\/)+[^\\s]+[\\w]";
//      Pattern pattern = Pattern.compile(urlPattern);
//      Matcher matcher = pattern.matcher(rawHTML);
//
//      // Each time the regex matches a URL in the HTML,
//      // add it to the queue for the next traverse and the list of visited URLs.
//      breakpoint = getBreakpoint(breakpoint, matcher);
//
//      // exit the outermost loop if it reaches the breakpoint.
//      if(breakpoint == 0){
//          break;
//      }
//  }
//
//}