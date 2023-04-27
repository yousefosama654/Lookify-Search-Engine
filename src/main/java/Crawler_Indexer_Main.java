import java.io.IOException;

public class Crawler_Indexer_Main {
    public static void main(String[] args) throws IOException {
        //start crawling first
        Crawler crawler = new Crawler(5, 6000);
        crawler.JoinAll();
        crawler.finish();
        Indexer.StartIndexing();
    }
}
