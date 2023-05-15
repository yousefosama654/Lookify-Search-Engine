package com.lookify.Lookify;

import org.bson.Document;
import java.io.*;
import java.util.*;
public class Crawler_Indexer_Main {
    public static void main(String[] args) throws IOException {
//        while (true) {
//            try {
//                Crawler crawler = new Crawler(5, 6000);
//                crawler.JoinAll();
//                crawler.finish();
//                Indexer.StartIndexing();
//                Popularity.rankPopularity(crawler.visitedURLs);
//                Thread.sleep(3 * 24 * 60 * 60 * 1000);
//            } catch (InterruptedException e) {
//            }
//        }
//    }
        Crawler crawler = new Crawler(4, 6000);
        crawler.JoinAll();
        //   crawler.finish();
        Indexer.StartIndexing();
        Popularity.rankPopularity(crawler.visitedURLs);
    }
}