package referencer;

import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class Document {
    public String link;
    private int wordCount = 0;
    private int globalDepth = 1;
    private final int CAPACITY = 3;
    private final int HEADER_BYTES = Integer.BYTES * 2;
    private final int BUCKET_BYTES = HEADER_BYTES + ((Integer.BYTES + Double.BYTES) * CAPACITY);
    private ArrayList<ByteBuffer> buckets = new ArrayList<>();
    private int[] directory;

    public Document(String link) {
        this.link = link;

        directory = new int[(int) Math.pow(2, globalDepth)];

        for (int i = 0; i < directory.length; i++) {
            buckets.add(ByteBuffer.allocate(BUCKET_BYTES).putInt(0).putInt(1));
            directory[i] = i;
        }
    }

    public void readData() throws IOException {
        RandomAccessFile reader = new RandomAccessFile("documents/" + link.substring(30), "r");
        FileChannel channel = reader.getChannel();

        ByteBuffer header = ByteBuffer.allocate(HEADER_BYTES);
        channel.read(header);

        wordCount = header.rewind().getInt();
        globalDepth = header.getInt();

        directory = new int[(int) Math.pow(2, globalDepth)];

        for (int i = 0; i < directory.length; i++) {
            ByteBuffer index = ByteBuffer.allocate(Integer.BYTES);
            channel.read(index);

            directory[i] = index.rewind().getInt();
        }

        buckets = new ArrayList<>();

        while (true) {
            ByteBuffer bucket = ByteBuffer.allocate(BUCKET_BYTES);
            if (channel.read(bucket) == -1) { break; }

            buckets.add(bucket.rewind());
        }

        channel.close();
        reader.close();
    }

    public void writeData() throws IOException {
        RandomAccessFile writer = new RandomAccessFile("documents/" + link.substring(30), "rw");
        FileChannel channel = writer.getChannel();
        channel.truncate(0);

        ByteBuffer header = ByteBuffer.allocate(HEADER_BYTES).putInt(wordCount).putInt(globalDepth).rewind();
        channel.write(header);

        for (int index : directory) {
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES).putInt(index).rewind();
            channel.write(buffer);
        }

        for (ByteBuffer bucket : buckets) {
            channel.write(bucket.rewind());
        }

        channel.close();
        writer.close();
    }

    public void calculateTF(Document glossary) throws IOException {
        org.jsoup.nodes.Document html = Jsoup.connect(link).get();

        Elements h2 = html.select("div.mw-parser-output > h2");
        Elements h3 = html.select("div.mw-parser-output > h3");
        Elements p = html.select("div.mw-parser-output > p");

        String text = h2.text() + " " + h3.text() + " " + p.text();
        String[] words = text.toLowerCase().split("[^A-Za-z_'-]+");

        for (String word : words) {
            int hash = word.hashCode();

            glossary.add(hash);
            add(hash);
        }

        for (ByteBuffer bucket : buckets) {
            int size = bucket.rewind().getInt();
            bucket.getInt();

            for (int i = 0; i < size; i++) {
                bucket.getInt();

                double count = bucket.mark().getDouble();
                bucket.reset().putDouble(count / (double) wordCount);
            }
        }
    }

    public void calculateIDF(ArrayList<Document> corpus) {
        for (ByteBuffer bucket : buckets) {
            int size = bucket.rewind().getInt();
            bucket.getInt();

            for (int i = 0; i < size; i++) {
                int hash = bucket.getInt();
                int documentCount = 0;

                for (Document document : corpus) {
                    if (document.get(hash) == -1) { continue; }
                    documentCount++;
                }

                bucket.putDouble(Math.log((double) corpus.size() / (double) documentCount));
            }
        }
    }

    public void calculateTFIDF(Document glossary) {
        for (ByteBuffer bucket : buckets) {
            int size = bucket.rewind().getInt();
            bucket.getInt();

            for (int i = 0; i < size; i++) {
                int hash = bucket.getInt();

                double tf = bucket.mark().getDouble();
                double idf = glossary.get(hash);

                bucket.reset().putDouble(tf * idf);
            }
        }
    }

    public Document getSimilarDocument(ArrayList<Document> corpus) {
        Document mostSimilar = corpus.get(0);
        double highestScore = 0;

        for (Document document : corpus) {
            double score = calculateSimilarity(document);

            if (score > highestScore) {
                mostSimilar = document;
                highestScore = score;
            }
        }

        return mostSimilar;
    }

    public void getSimilarCluster(ArrayList<Cluster> clusters) {
        Cluster mostSimilar = clusters.get(0);
        double highestScore = 0;

        for (Cluster cluster : clusters) {
            double score = calculateSimilarity(cluster.medoid);

            if (score > highestScore) {
                mostSimilar = cluster;
                highestScore = score;
            }
        }

        mostSimilar.similarity += highestScore;
        mostSimilar.documents.add(this);
    }

    public double calculateSimilarity(Document document) {
        double dotProduct = 0;
        double length1 = 0;
        double length2 = 0;

        for (ByteBuffer bucket : buckets) {
            int size = bucket.rewind().getInt();
            bucket.getInt();

            for (int i = 0; i < size; i++) {
                int hash = bucket.getInt();

                double tfidf1 = bucket.getDouble();
                double tfidf2 = document.get(hash);

                if (tfidf2 == -1) { tfidf2 = 0; }
                dotProduct += tfidf1 * tfidf2;

                length1 += Math.pow(tfidf1, 2);
                length2 += Math.pow(tfidf2, 2);
            }
        }

        double lengths = Math.sqrt(length1) * Math.sqrt(length2);
        return dotProduct / lengths;
    }

    private int mask(int hash, int depth) {
        return hash & ((1 << depth) - 1);
    }

    public double get(int hash) {
        ByteBuffer bucket = buckets.get(directory[mask(hash, globalDepth)]);

        int size = bucket.rewind().getInt();
        bucket.getInt();

        for (int i = 0; i < size; i++) {
            int otherHash = bucket.getInt();
            double value = bucket.getDouble();

            if (otherHash != hash) { continue; }
            return value;
        }

        return -1;
    }

    public void add(int hash) {
        ByteBuffer bucket = buckets.get(directory[mask(hash, globalDepth)]);

        wordCount++;
        
        int size = bucket.rewind().getInt();
        int localDepth = bucket.getInt();
        
        for (int i = 0; i < size; i++) {
            int otherHash = bucket.getInt();
            double count = bucket.mark().getDouble();

            if (hash != otherHash) { continue; }
            
            bucket.reset().putDouble(count + 1);
            return;
        }

        if (size < CAPACITY) {
            bucket.putInt(hash).putDouble(1);
            bucket.rewind().putInt(size + 1);
            return;
        }

        resize(localDepth, hash);

        bucket.rewind().getInt();
        bucket.putInt(localDepth + 1);

        ByteBuffer tempBucket = emptyBucket(bucket);
        tempBucket.rewind();

        for (int i = 0; i < CAPACITY; i++) {
            int oldHash = tempBucket.getInt();
            double count = tempBucket.getDouble();

            for (int j = 0; j < count; j++) {
                wordCount--;
                add(oldHash);
            }
        }

        wordCount--;
        add(hash);
    }

    private void resize(int localDepth, int hash) {
        if (localDepth == globalDepth) {
            globalDepth++;
            
            int[] newDirectory = new int[(int) Math.pow(2, globalDepth)];

            for (int i = 0; i < newDirectory.length; i++) {
                newDirectory[i] = directory[mask(i, localDepth)];
            }

            directory = newDirectory;
        }

        ByteBuffer bucket = ByteBuffer.allocate(BUCKET_BYTES);
        bucket.putInt(0).putInt(localDepth + 1);

        buckets.add(bucket);

        int index = mask(hash, localDepth) + (1 << localDepth);

        for (int i = 0; i < directory.length; i++) {
            if (mask(i, localDepth + 1) == index) {
                directory[i] = buckets.size() - 1;
            }
        }
    }

    private ByteBuffer emptyBucket(ByteBuffer bucket) {
        ByteBuffer tempBucket = ByteBuffer.allocate(BUCKET_BYTES - HEADER_BYTES);
        bucket.rewind().putInt(0).getInt();

        for (int i = 0; i < CAPACITY; i++) {
            tempBucket.putInt(bucket.mark().getInt());
            tempBucket.putDouble(bucket.getDouble());

            bucket.reset().putInt(0);
            bucket.putDouble(0);
        }

        return tempBucket;
    }
}
