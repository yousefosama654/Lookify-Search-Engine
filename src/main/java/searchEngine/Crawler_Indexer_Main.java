package searchEngine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Crawler_Indexer_Main {
    public static void main(String[] args) throws IOException {
        //   start crawling first

        Crawler crawler = new Crawler(3, 100);
        crawler.JoinAll();

        crawler.finish();
        HashMap<String, List<String>> temp = crawler.visitedURLs;
        Indexer.StartIndexing();
//       String s="";
//       String[] g=s.split(" ");
//       List<String> myList = new ArrayList<>(Arrays.asList(g));
//       Ranker r = new Ranker((ArrayList<String>) myList,temp);


    }
}
