package searchEngine;

import java.util.*;

public class Pair {
    int TF;
    int Size;
    int Priority;
    List<String> Paragraphs;

    public Pair(int TF, int Size, int Proiority, List<String> Paragraphs) {
        this.TF = TF;
        this.Size = Size;
        this.Priority = Proiority;
        this.Paragraphs = Paragraphs;
    }
}
