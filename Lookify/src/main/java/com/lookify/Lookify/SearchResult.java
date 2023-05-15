package com.lookify.Lookify;

import java.util.List;

public class SearchResult {
    String link;
    String paragraph;
    String title;
    List<String> indecies;

    public SearchResult(String link, String paragraph, String title, List<String> indecies) {
        this.link = link;
        this.paragraph = paragraph;
        this.title = title;
        this.indecies = indecies;
    }

}
