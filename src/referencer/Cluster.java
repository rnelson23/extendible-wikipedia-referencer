package referencer;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class Cluster {
    public ArrayList<Document> documents = new ArrayList<>();
    public Document medoid;
    public double similarity = 0;
    public String name;

    public Cluster(String name) {
        this.name = name;
    }

    public void shuffleMedoid() {
        for (Document document : documents) {
            double score = 0;

            for (Document other : documents) {
                if (other.link.equals(document.link)) { continue; }
                score += other.calculateSimilarity(document);
            }

            if (score > similarity) {
                medoid = document;
                similarity = score;
            }
        }
    }

    public void readData(ArrayList<Document> corpus) throws IOException {
        RandomAccessFile reader = new RandomAccessFile("clusters/" + name, "r");
        FileChannel channel = reader.getChannel();

        ByteBuffer header = ByteBuffer.allocate(Integer.BYTES);
        channel.read(header);

        int bytes = header.rewind().getInt();
        ByteBuffer link = ByteBuffer.allocate(bytes);
        channel.read(link);

        medoid = new Document(new String(link.array(), StandardCharsets.UTF_8));
        medoid.readData();

        corpus.add(medoid);

        while (true) {
            header.rewind();
            if (channel.read(header) == -1) { break; }

            bytes = header.rewind().getInt();
            link = ByteBuffer.allocate(bytes);
            channel.read(link);

            Document document = new Document(new String(link.array(), StandardCharsets.UTF_8));
            document.readData();

            documents.add(document);
            corpus.add(document);
        }

        channel.close();
        reader.close();
    }

    public void writeData() throws IOException {
        RandomAccessFile writer = new RandomAccessFile("clusters/" + name, "rw");
        FileChannel channel = writer.getChannel();
        channel.truncate(0);

        ByteBuffer header = ByteBuffer.allocate(Integer.BYTES);
        ByteBuffer link = ByteBuffer.wrap(medoid.link.getBytes(StandardCharsets.UTF_8));

        header.putInt(link.capacity());
        channel.write(header.rewind());
        channel.write(link.rewind());

        for (Document document : documents) {
            link = ByteBuffer.wrap(document.link.getBytes(StandardCharsets.UTF_8));
            header.rewind().putInt(link.capacity());

            channel.write(header.rewind());
            channel.write(link.rewind());
        }

        channel.close();
        writer.close();
    }
}
