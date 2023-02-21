package referencer.application;

import referencer.Cluster;
import referencer.Document;

import javax.swing.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.IOException;

public class GUI {
    public JPanel panel1;
    private JTextField textField1;
    private JButton searchButton;
    private JTextArea textArea1;

    public GUI() {
        searchButton.addActionListener(a -> {
            try {
                String link = textField1.getText();

                Document document = new Document(link);
                document.calculateTF(Main.glossary);

                document.calculateTFIDF(Main.glossary);

                Document similarDocument = document.getSimilarDocument(Main.corpus);
                StringBuilder text = new StringBuilder(similarDocument.link);

                for (Cluster cluster : Main.clusters) {
                    if (!cluster.documents.contains(similarDocument)) { continue; }

                    for (Document document1 : cluster.documents) {
                        if (document1.equals(similarDocument)) { continue; }
                        text.append("\n").append(document1.link);
                    }

                    break;
                }

                textArea1.setText(text.toString());

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        textField1.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                super.focusGained(e);

                textField1.selectAll();
                textArea1.setText("");
            }
        });
    }

    public static void initialize() {
        JFrame frame = new JFrame("GUI");
        frame.setContentPane(new GUI().panel1);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
