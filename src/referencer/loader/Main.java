package referencer.loader;

import referencer.Cluster;
import referencer.Document;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class Main implements Serializable {
    public static ArrayList<Document> corpus = new ArrayList<>();
    public static ArrayList<Cluster> clusters = new ArrayList<>();
    public static Document glossary = new Document("https://en.wikipedia.org/wiki/Glossary");
    public static final int NUM_CLUSTERS = 10;

    public static void main(String[] args) throws IOException {
        File file = new File("links.txt");
        Scanner scanner = new Scanner(file);

        while (scanner.hasNextLine()) {
            String link = scanner.nextLine();

            Document document = new Document(link);
            document.calculateTF(glossary);

            corpus.add(document);
        }

        scanner.close();
        glossary.calculateIDF(corpus);

        for (Document document : corpus) {
            document.calculateTFIDF(glossary);
            document.writeData();
        }

        glossary.writeData();
        ArrayList<Document> medoids = new ArrayList<>();

        for (int i = 0; i < NUM_CLUSTERS; i++) {
            Document document = corpus.get(i);

            Cluster cluster = new Cluster("Cluster" + i);
            cluster.documents.add(document);
            cluster.medoid = document;

            clusters.add(cluster);
            medoids.add(document);
        }

        double totalSimilarity = addDocuments(clusters, medoids);

        for (int i = 0; i < 1000; i++) {
            ArrayList<Cluster> newClusters = new ArrayList<>();
            ArrayList<Document> newMedoids = new ArrayList<>();

            for (Cluster cluster : clusters) {
                Cluster newCluster = new Cluster(cluster.name);
                newCluster.documents.add(cluster.medoid);
                newCluster.medoid = cluster.medoid;

                newClusters.add(newCluster);
                newMedoids.add(cluster.medoid);
            }

            double newTotalSimilarity = addDocuments(newClusters, newMedoids);

            if (newTotalSimilarity > totalSimilarity) {
                clusters = newClusters;
            }
        }

        for (Cluster cluster : clusters) {
            cluster.writeData();
        }
    }

    public static double addDocuments(ArrayList<Cluster> clusters, ArrayList<Document> medoids) {
        double totalSimilarity = 0;

        for (Document document : corpus) {
            if (medoids.contains(document)) { continue; }
            document.getSimilarCluster(clusters);
        }

        for (Cluster cluster : clusters) {
            cluster.shuffleMedoid();
            totalSimilarity += cluster.similarity;
        }

        return totalSimilarity;
    }
}
