package referencer.seeder;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException {
        File file = new File("seed-links.txt");
        Scanner scanner = new Scanner(file);

        ArrayList<String> seedLinks = new ArrayList<>();
        ArrayList<String> links = new ArrayList<>();

        while (scanner.hasNextLine()) {
            String link = scanner.nextLine();

            seedLinks.add(link);
            org.jsoup.nodes.Document html = Jsoup.connect(link).get();

            String nextHeaderIndex = "";
            Element nextHeader = html.selectFirst("h2:has(span#See_also) ~ h2");
            if (nextHeader != null) { nextHeaderIndex = ":lt(" + nextHeader.elementSiblingIndex() + ")"; }

            Element ul = html.selectFirst("h2:has(span#See_also) ~ ul" + nextHeaderIndex);

            if (ul == null) {
                Element div = html.selectFirst("h2:has(span#See_also) ~ div.div-col" + nextHeaderIndex);
                if (div == null) { continue; }

                ul = div.selectFirst("ul");
                if (ul == null) { continue; }
            }

            Elements a = ul.select("li a");
            ArrayList<String> newLinks = new ArrayList<>();

            for (Element element : a) {
                String newLink = element.attr("abs:href");

                if (!newLink.contains("wikipedia.org")) { continue; }

                if (newLink.contains("Outline")) { continue; }
                if (newLink.contains("List")) { continue; }
                if (newLink.contains("Portal")) { continue; }
                if (newLink.contains("Glossary")) { continue; }
                if (newLink.contains("Category")) { continue; }
                if (newLink.contains("Index")) { continue; }

                newLink = newLink.replace("%27", "'");
                newLinks.add(newLink);
                links.add(newLink);

                if (newLinks.size() == 9) { break; }
            }

            if (links.size() == 90) { break; }
        }

        scanner.close();
        BufferedWriter out = new BufferedWriter(new FileWriter("links.txt", true));

        for (String link : seedLinks) {
            out.write(link + "\n");
        }

        for (String link : links) {
            out.write("\n" + link);
        }

        out.close();
    }
}
