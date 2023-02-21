package referencer.application;

import referencer.Cluster;
import referencer.Document;

import java.io.*;
import java.util.ArrayList;

public class Main {
    public static ArrayList<Document> corpus = new ArrayList<>();
    public static ArrayList<Cluster> clusters = new ArrayList<>();
    public static Document glossary = new Document("https://en.wikipedia.org/wiki/Glossary");

    public static void main(String[] args) throws IOException {
        File folder = new File("clusters");
        File[] files = folder.listFiles();

        if (files == null) { return; }

        for (File file : files) {
            Cluster cluster = new Cluster(file.getName());
            cluster.readData(corpus);
            clusters.add(cluster);
        }

        glossary.readData();
        GUI.initialize();
    }
}
