package com.lookify.Lookify;

import java.io.*;
import java.util.*;


import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;


public class QueryProcessor {
    static public HashSet<Document> words_documents;
    static public HashSet<Document> temp;
    static public HashMap<String, intPair> PhraseSearchingLinks;
    static public List<String> QueryWords;
    static public HashSet<String> words;
    static public HashMap<String, intPair> temp2;
    static public Boolean PhraseSearching = false;
    static public int Nth = 7;
    static boolean andFlag = false;


    public static void GetDocsFromDB() {
        MongoClient mongoClient = MongoClients.create(MongoDB.getConnectionString());
        MongoDatabase mongodb = mongoClient.getDatabase("Lookify");
        MongoCollection mongoCollection = mongodb.getCollection("InvertedIndex");
        for (String word : QueryWords) {
            Document d = new Document("Word", word);
            Document found = (Document) mongoCollection.find(d).first();
            if (found != null) {
                if (andFlag) temp.add(found);
                else words_documents.add(found);
            }
        }
    }


    public static Boolean CheckIfOnlyPhrase(String Query) {
        if (Query.startsWith("\"") && Query.endsWith("\"")) return true;
        else return false;
    }

    public static Boolean CheckIfNotOnlyPhrase(String Query) {
        if ((Query.startsWith("\"") && !Query.endsWith("\""))
                || (!Query.startsWith("\"") && Query.endsWith("\""))
                || (Query.indexOf("\"") > 0 && Query.lastIndexOf("\"") < Query.length() - 1)) return true;
        else return false;
    }

    public static Boolean CheckCobinations(String Query) {
        if (Query.contains("OR") || Query.contains("AND") || Query.contains("NOT")) return true;
        else return false;
    }


    public static void getLinksForPhrase(List<String> QueryWordsPhrase) throws IOException {
        Iterator<Document> itr;
        if (andFlag) itr = temp.iterator();
        else itr = words_documents.iterator();
        while (itr.hasNext()) {
            ArrayList<Document> docList = (ArrayList<Document>) itr.next().get("Documents");
            for (Document doc : docList) {
                String link = doc.getString("Document");
                ArrayList<Document> paragraph = (ArrayList<Document>) doc.get("Paragraphs");
                outerloop:
                for (Document parag1 : paragraph) {
                    ArrayList<Integer> firstWord = new ArrayList<Integer>();
                    String parag = parag1.getString("link");
                    parag = parag.toLowerCase();
                    List<String> S = StringProcessing.splitWords(parag);
                    for (int m = 0; m < S.size(); m++) {
                        if (S.get(m).equals(QueryWordsPhrase.get(0)))
                            firstWord.add(m);
                    }
                    for (int t = 0; t < firstWord.size(); t++) {
                        int prev = -1;
                        int current = -1;
                        int m;
                        int end;
                        if ((QueryWordsPhrase.size() * Nth) + firstWord.get(t) <= S.size() - 1)
                            end = (QueryWordsPhrase.size() * Nth) + firstWord.get(t);
                        else
                            end = S.size();
                        List<String> subparagraph = S.subList(firstWord.get(t), end);
                        for (m = 0; m < QueryWordsPhrase.size(); m++) {
                            if (subparagraph.contains(QueryWordsPhrase.get(m))) {
                                current = S.indexOf(QueryWordsPhrase.get(m));
                                if (prev != -1)
                                    if (current - prev > Nth)
                                        break;
                                prev = current;
                            } else break;
                        }
                        if (m == QueryWordsPhrase.size()) {
                            intPair p = new intPair(parag, doc.getInteger("Priority"));
                            if (andFlag) temp2.put(link, p);
                            else PhraseSearchingLinks.put(link, p);
                            break outerloop;
                        }
                    }
                }
            }
        }
    }


    public static void QuereyLinks(String Query) throws IOException {
        Query = Query.trim();
        temp = new HashSet<>();
        temp2 = new HashMap<>();
        QueryWords = StringProcessing.splitWords(Query);
        List<String> QueryWordsPhrase = new ArrayList<>(QueryWords);
        StringProcessing.ConvertToLower(QueryWords);
        StringProcessing.ConvertToLower(QueryWordsPhrase);
        StringProcessing.ReadStopWords();
        StringProcessing.RemoveStopWords(QueryWords);
        StringProcessing.RemoveStopWords(QueryWordsPhrase);
        StringProcessing.Stemming(QueryWords);
        QueryWords = new ArrayList<>(new HashSet<>(QueryWords));
        GetDocsFromDB();
        if (CheckIfOnlyPhrase(Query) == true) {
            PhraseSearching = true;
            getLinksForPhrase(QueryWordsPhrase);
        }
    }

    public static void QueryResult(String Query) throws IOException {
        PhraseSearchingLinks = new HashMap<>();
        words_documents = new HashSet<>();
        words = new HashSet<>();
        if (CheckIfNotOnlyPhrase(Query) == true && CheckCobinations(Query) == false) {
            ArrayList<String> Queries = new ArrayList<String>(Arrays.asList(Query.split("\"")));
            for (int i = 0; i < Queries.size(); i++) {
                String temp = "\"" + Queries.get(i) + "\"";
                if (Query.contains(temp)) {
                    QuereyLinks(temp);
                } else {
                    QuereyLinks(Queries.get(i));
                }
            }
        } else if (CheckCobinations(Query) == true) {
            Query = Query.trim();
            LinkedHashMap<String, String> Queries = StringProcessing.splitForLogic(Query);
            int i = 0;
            String prev = "";
            for (Map.Entry<String, String> entry : Queries.entrySet()) {
                if (i == 0) {
                    QuereyLinks(entry.getKey());
                    prev = entry.getValue();
                    words.addAll(QueryWords);
                } else {
                    if (prev == "OR") {
                        QuereyLinks(entry.getKey());
                        prev = entry.getValue();
                        words.addAll(QueryWords);
                    } else if (prev == "AND") {
                        andFlag = true;
                        QuereyLinks(entry.getKey());
                        words_documents.retainAll(temp);
//                        PhraseSearchingLinks.retainAll(temp2);
                        HashMap<String, intPair> commonElements = new HashMap<>();
                        for (Map.Entry<String, intPair> element : temp2.entrySet()) {
                            String link = element.getKey();

                            if (PhraseSearchingLinks.containsKey(link)) {
                                intPair commonElement1 = PhraseSearchingLinks.get(link);
                                intPair commonElement2 = element.getValue();
                                if (commonElement1.getSecond() >= commonElement2.getSecond())
                                    commonElements.put(link, commonElement1);
                                else
                                    commonElements.put(link, commonElement2);
                            }
                        }
                        PhraseSearchingLinks = null;
                        PhraseSearchingLinks = new HashMap<>(commonElements);
                        prev = entry.getValue();
                        words.addAll(QueryWords);
                        andFlag = false;
                    } else if (prev == "NOT") {
                        andFlag = true;
                        QuereyLinks(entry.getKey());
                        Iterator<Document> itr1 = temp.iterator();
                        while (itr1.hasNext()) {
                            Document el = itr1.next();
                            if (words_documents.contains(el))
                                words_documents.remove(el);
                        }
                        for (Map.Entry<String, intPair> element : temp2.entrySet()) {
                            if (PhraseSearchingLinks.containsKey(element.getKey()))
                                PhraseSearchingLinks.remove(element.getKey());
                        }
                        prev = entry.getValue();
                        andFlag = false;
                    }
                }
                i++;
            }
        } else QuereyLinks(Query);
    }
}