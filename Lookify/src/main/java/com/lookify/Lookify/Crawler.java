package com.lookify.Lookify;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import opennlp.tools.cmdline.langdetect.LanguageDetectorModelLoader;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.json.simple.JSONObject;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

import opennlp.tools.langdetect.*;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


import static com.mongodb.client.model.Filters.exists;

public class Crawler implements Runnable {
    private Queue<String> urlQueue;
    public HashMap<String, Set<String>> visitedURLs;
    private static String[] notValid;
    private static HashMap<String,String>content;
    private List<String> normlizedurl;
    private int breakpoint;
    private int thread_num;
    private Thread[] MyThreads;
    MongoCollection<Document> links;
    MongoCollection<Document> BreakPoint;
    com.mongodb.client.MongoClient mongoClient;

    public List<String> GetSeeds() {
        List<String> HtmlLinks = new ArrayList<String>();
        String line;
        File file = new File("C:\\Users\\sggln\\Downloads\\Lookify\\Lookify\\initialseeds.txt");
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
            FileWriter writer = new FileWriter("Seeds.txt", true);
            writer.write(url);
            writer.write("\r\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Crawler(int n, int b) {
        // connection with data base
        connctToDataBase();
        content=new HashMap<>();
        urlQueue = new LinkedList<String>();
        visitedURLs = new HashMap<String, Set<String>>();
        notValid = new String[]{".pdf", ".doc", ".zip", ".xls", ".png", ".jpg", ".jepg", ".gif", ".css", ".mp4", ".wav", "audio/", "image/", ".js", ".xml", "css/", "Img/", "log-in", "login", "sign-in", "sign-up"};
        normlizedurl = new ArrayList<String>();
        thread_num = n;
        if (fillFromDB()) {
            deletefromseeds();
            List<String> HTMLSeed = GetSeeds();
            for (String seed : HTMLSeed) {
                urlQueue.add(seed);
                normlizedurl.add(normalized(seed));
                Set<String> temp = new HashSet<>();
                temp.add(" ");
                visitedURLs.put(seed, temp);
                List<JSONObject> List = new ArrayList<JSONObject>();
                Document doc1 = new Document("links", seed).append("normlizied", normalized(seed)).append("removed", "0").append("List", List);
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

    public void crawl() throws IOException, URISyntaxException {
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
            String rawHTML = content.get(s);
            if(rawHTML==null)
            {
                rawHTML=retrieveHTMLContent(rawHTML);
            }



            try {
                org.jsoup.nodes.Document htmlDocument =
                        Jsoup.connect(s)
                                .ignoreHttpErrors(true)
                                .timeout(5000)
                                .get();
                Elements linksOnPage = htmlDocument.select("a[href]");
                // create a regex pattern matching a URL
                // that will validate the content of HTML in search of a URL.
                //// look at it later (link pattern)
                //  String urlPattern = "^(www|http:\\/\\/|https:\\/\\/)[^\\s]+[\\w]$";
                List<String> links = new ArrayList<>();
                int c=0;
                for (Element link : linksOnPage) {
                    if(c>50)break;
                    String URL = link.absUrl("href");
                    if (URL.startsWith("https://"))
                        links.add(URL);
                    c++;

                }
                //  Pattern pattern = Pattern.compile(urlPattern);
                //  Matcher matcher = pattern.matcher(rawHTML);
                // Each time the regex matches a URL in the HTML,
                // add it to the queue for the next traverse and the list of visited URLs.
                getBreakpoint(links, s);
                // exit the outermost loop if it reaches the breakpoint.
                if (getBreakPoint() <= 0) {
                    break;
                }
            }
            catch (IOException e) {
                // update the state to 2 to indicate that this url was fetched successfully

                continue;
            }
        }
    }

    private void getBreakpoint( List<String>Urls, String parent) throws URISyntaxException, MalformedURLException {
        outerLoop:
        for(String actualURL:Urls ){
//            String actualURL = matcher.group();
            String normlized = normalized(actualURL);

            if (!visitedURLs.containsKey(actualURL)) {
                if (getBreakPoint() <= 0) {
                    break;

                }
                synchronized (this.visitedURLs) {
                    Set<String> temp = new HashSet<>();
                    temp.add(parent);
                    //   URL url = new URL(actualURL);
                    //  actualURL = url.toURI().normalize().toString();
                    synchronized (this.normlizedurl) {
                        if (checknormilze(normlized) && !normlized.equals(""))
                            normlizedurl.add(normlized);

                        else {
                            continue;
                        }
                    }
                    if (!isValid(actualURL)||!checkIfAllowed(actualURL)) {
                        continue;
                    }


                    visitedURLs.put(actualURL, temp);
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
            } else {

                Set<String> temp = visitedURLs.get(actualURL);
                Document filter = new Document("links", actualURL);
                temp.add(parent);
                visitedURLs.replace(actualURL, temp);
                //  links.deleteOne(filter);
                // Define the update operation(s) to apply to the matched document(s)
                Document update = new Document("$set", new Document("List", temp));
                links.updateOne(filter, update);
            }
        }

        // exit the loop if it reaches the breakpoint.
    }
    private boolean checknormilze(String normlized)
    {
        for(String s:normlizedurl) {
            double similarity = StringUtils.getJaroWinklerDistance(s, normlized);
            if(similarity>=0.8)return false;
        }
        return true;
    }

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
        content.put(actualURL,rawHTML);
        return normlized;
    }

    public void run() {
        try {
            crawl();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized int getBreakPoint() {
        return breakpoint;
    }

    public synchronized void decreamentBreakPoint() {
        this.breakpoint--;
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
//        String connectionString = "mongodb+srv://sarahgerges01:eng8987458@cluster0.mamfgqv.mongodb.net/?retryWrites=true&w=majority";
//        MongoClientSettings settings = MongoClientSettings.builder()
//                .applyConnectionString(new ConnectionString(connectionString)).build();
        // Create a new client and connect to the server
        try {
//            mongoClient = MongoClients.create(settings);
            // Send a ping to confirm a successful connection
//            MongoDatabase database = mongoClient.getDatabase("Crawler");
            // database.runCommand(new Document("ping", 1));
            links = MongoDB.GetCollection("links");
            BreakPoint = MongoDB.GetCollection("BreakPoint");
            System.out.println("Pinged your deployment. You successfully connected to MongoDB!");
        } catch (MongoException e) {
            System.out.println("error");
        }
    }

    public boolean fillFromDB() {
        int count = 0;
        MongoCursor<Document> curr = BreakPoint.find().iterator();
        int c = 0;
        if (!curr.hasNext())
        {
            finish();
            return true;
        }
        MongoCursor<Document> cur = links.find().iterator();
        while (cur.hasNext()) {
            Document document = cur.next();
            String y = document.getString("removed");
            String norm = document.getString("normlizied");
            String url = document.getString("links");
            //  ArrayList<Document> docList = (ArrayList<Document>) document.get("List");
            if (y.equals("0")) {
                urlQueue.add(url);
                Set<String> temp = new HashSet<>();
                temp.addAll(document.getList("List",String.class));

                visitedURLs.put(url, temp);
            }
            else {
                Set<String> temp = new HashSet<>();
                temp.addAll(document.getList("List",String.class));

                visitedURLs.put(url, temp);
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

    public static boolean isValid(String link) {
        try {

            HttpURLConnection connection = (HttpURLConnection) new URL(link).openConnection();
            connection.setConnectTimeout(2000); // 10 seconds timeout
            connection.setReadTimeout(2000); // 10 seconds timeout

            connection.setRequestMethod("HEAD");
            String contentType = connection.getHeaderField("Content-Type");
            if (contentType != null && contentType.contains("text/html")) {
                for (int i = 0; i < notValid.length; i++)
                    if (link.contains(notValid[i])) {
                        System.out.println("the link isnot valid because itsnot valid" + link);
                        return false;
                    }
                try {
                    int responseCode = connection.getResponseCode();
                    if (responseCode >= 400 && responseCode < 600) {
                        System.out.println("the link isnot valid because error code" + link);
                        return false;
                    } else {

                        // Retrieve the HTML content of the URL
                        String htmlContent = content.get(link);
                        // Check if the HTML code contains English text
                        if (containsEnglishText(htmlContent)) {
                            {
                                System.out.println("The content is in English." + link);
                                try {
                                    org.jsoup.nodes.Document doc = Jsoup.connect(link).get();
                                    return true;
                                } catch (IOException e) {
                                    return false;
                                }
                            }
                        } else {
                            {
                                System.out.println("The content is not in English."+ link);
                                return false;
                            }
                        }
                    }

                } catch (IOException e)
                {
//                    e.printStackTrace();
                    return false;
                }

            } else {
                System.out.println("the link isnot valid because itsnot html" + link);
                return false;
            }
        }
        catch (SocketTimeoutException e) {
            System.err.println("Connection timed out: " + e.getMessage());
            return false;
            // Handle the exception here
        }
        catch (IOException e) {
//            e.printStackTrace();
            return false;
        }


    }



    public Boolean checkIfAllowed(String actualURL) {
        URL url = null;
        URL urlRobot = null;
        String strHost = "";
        try {
            url = new URL(actualURL);
            strHost = url.getHost();
            String strRobot = "http://" + strHost + "/robots.txt";
            urlRobot = new URL(strRobot);

        } catch (MalformedURLException e) {
            e.printStackTrace();
            return true;
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
                        return true;
                    } else {
                        int start = Line.indexOf(":") + 1;
                        int end = Line.length() - 1;
                        String rule = Line.substring(start, end);
                        rule.trim();
                        if (rule.length() == 0)
                            break;
                        else if (rule.equals("/"))
                            return false;
                        // disallow **
                        if (rule.startsWith("") || rule.endsWith("")) {
                            if (rule.startsWith("") && rule.endsWith("")) {
                                int start1 = rule.indexOf("*") + 1;
                                int end1 = rule.length() - 2;
                                String r = rule.substring(start1, end1);
                                if (actualURL.contains(r))
                                    return false;
                            } else if (rule.startsWith("*")) {
                                int start1 = rule.indexOf("*") + 1;
                                int end1 = rule.length();
                                String r = rule.substring(start1, end1);
                                if (actualURL.endsWith(r))
                                    return false;
                            } else if (rule.endsWith("*")) {
                                int end1 = rule.length() - 2;
                                String r = rule.substring(0, end1);
                                if (actualURL.startsWith(r))
                                    return false;
                            }
                        }
                        // disallow exitions
                        else if (rule.startsWith("/*")) {
                            int start1 = rule.indexOf(".") + 1;
                            int end1 = rule.length() - 1;
                            String r = rule.substring(start1, end1);
                            if (actualURL.endsWith(r))
                                return false;
                        }
                        // disallow folder or file
                        else if (rule.startsWith("/")) {
                            String r = strHost + rule;
                            if (actualURL.startsWith(r))
                                return false;
                        }

                        Line = input.readLine();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                return true;

            }
        }
        return true;
    }

    private static String retrieveHTMLContent(String link) throws IOException {
        String rawHTML = "";

        try {
            // create url with the string.
            URL url = new URL(link);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String inputLine = in.readLine();
            // read every line of the HTML content in the URL
            // and concat each line to the rawHTML string until every line is read.
            while (inputLine != null) {
                int l = inputLine.length();
                rawHTML += inputLine;
                inputLine = in.readLine();
            }
            in.close();
        } catch (Exception e) {
            // e.printStackTrace();
        }
        content.put(link,rawHTML);
        return rawHTML;
    }

    private static boolean containsEnglishText(String htmlContent) {
        // Convert the HTML code to lowercase for case-insensitive comparison
//        String html = "<html lang=\"en\"><head><title>Example</title></head><body><p>Hello, world!</p></body></html>";
        org.jsoup.nodes.Document doc = Jsoup.parse(htmlContent);

// Get the language tag from the HTML document
        String lang = doc.select("html").attr("lang");

// Print the language tag
        //System.out.println(lang);
        if(lang!=null)
            lang.toLowerCase();
        return (lang.contains("en")||lang==null||lang.equals("")||lang.equals(" "));
        // Add more English-specific keywords or patterns as necessary

    }

    public void deletefromseeds() {
        File file = new File("Seeds.txt");
        file.delete();
    }

}