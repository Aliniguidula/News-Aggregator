import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class IndexBuilder implements IIndexBuilder {
    @Override
    public Map<String, List<String>> parseFeed(List<String> feeds) {
        Map<String, List<String>> map = new HashMap<>();
        for (String s: feeds) {
            try {
                Document doc = Jsoup.connect(s).get();
                Elements links = doc.getElementsByTag("link");
                for (Element link : links) {
                    String linkText = link.text();
                    Document web = Jsoup.connect(linkText).get();
                    Elements body = web.getElementsByTag("body");
                    String bodyText = body.text();
                    bodyText = bodyText.toLowerCase();
                    String[] results = bodyText.split(" ");
                    for (int i = 0; i < results.length; i++) {
                        results[i] = results[i].replaceAll("[^a-z0-9]", "");
                    }
                    List<String> value = Arrays.asList(results);
                    map.put(linkText, value);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return map;
    }

    @Override
    public Map<String, Map<String, Double>> buildIndex(Map<String, List<String>> docs) {
        Map<String, Map<String, Double>> res = new HashMap<>();
        Map<String, Double> idfCache = calculateIdf(docs);

        for (Map.Entry<String, List<String>> entry : docs.entrySet()) {
            String document = entry.getKey();
            List<String> terms = entry.getValue();

            Map<String, Double> tfMap = calculateTf(terms);
            Map<String, Double> tfIdfMap = calculateTfIdf(tfMap, idfCache);

            res.put(document, new TreeMap<>(tfIdfMap));
        }

        return res;
    }

    private Map<String, Double> calculateTf(List<String> terms) {
        Map<String, Double> tf = new HashMap<>();
        terms.forEach(term -> tf.put(term, tf.getOrDefault(term, 0.0) + 1));
        tf.forEach((term, count) -> tf.put(term, count / terms.size()));
        return tf;
    }

    private Map<String, Double> calculateIdf(Map<String, List<String>> docs) {
        Map<String, Double> idf = new HashMap<>();
        Map<String, Integer> docCount = new HashMap<>();

        docs.values().forEach(terms -> {
            new HashSet<>(terms).forEach(term ->
                    docCount.put(term, docCount.getOrDefault(term, 0) + 1));
        });

        final int totalDocs = docs.size();
        docCount.forEach((term, count) -> idf.put(term, Math.log(totalDocs / (double) count)));

        return idf;
    }

    private Map<String, Double> calculateTfIdf(Map<String, Double> tf,
                                               Map<String, Double> idfCache) {
        Map<String, Double> tfIdf = new HashMap<>();
        tf.forEach((term, value) -> tfIdf.put(term, value * idfCache.getOrDefault(term, 0.0)));
        return tfIdf;
    }


    @Override
    public Map<String, List<Entry<String, Double>>> buildInvertedIndex(Map<String,
            Map<String, Double>> index) {
        Map<String, List<Entry<String, Double>>> invertedIndex = new HashMap<>();

        index.forEach((docId, terms) -> {
            terms.forEach((term, tfIdf) -> {
                invertedIndex.computeIfAbsent(term, k -> new ArrayList<>())
                        .add(new AbstractMap.SimpleEntry<>(docId, tfIdf));
            });
        });

        invertedIndex.forEach((term, entries) -> {
            entries.sort(Comparator.comparing(Entry<String, Double>::getValue).reversed());
        });

        return invertedIndex;
    }

    @Override
    public Collection<Map.Entry<String, List<String>>> buildHomePage(Map<?, ?> invertedIndex) {
        removeStopWords(invertedIndex);

        List<Entry<String, List<String>>> results = new ArrayList<>();

        for (Map.Entry<?, ?> entry : invertedIndex.entrySet()) {
            if (entry.getKey() instanceof String && entry.getValue() instanceof Collection) {
                String term = (String) entry.getKey();
                Collection<?> possibleEntries = (Collection<?>) entry.getValue();
                List<String> articles = possibleEntries.stream()
                        .filter(e -> e instanceof Entry)
                        .map(e -> (Entry<?, ?>) e)
                        .filter(e -> e.getKey() instanceof String && e.getValue() instanceof Double)
                        .map(e -> (String) e.getKey())
                        .collect(Collectors.toList());

                results.add(new AbstractMap.SimpleEntry<>(term, articles));
            }
        }

        results.sort(getEntryComparator());
        return results;
    }


    private void removeStopWords(Map<?, ?> invertedIndex) {
        invertedIndex.keySet().removeIf(key -> key instanceof String && STOPWORDS.contains(key));
    }

    private Comparator<Entry<String, List<String>>> getEntryComparator() {
        return (o1, o2) -> {
            int sizeComparison = Integer.compare(o2.getValue().size(), o1.getValue().size());
            return sizeComparison != 0 ? sizeComparison : o2.getKey().compareTo(o1.getKey());
        };
    }

    @Override
    public Collection<?> createAutocompleteFile(
            Collection<Map.Entry<String, List<String>>> homepage) {
        List<String> words = extractWords(homepage);
        sortWords(words);
        writeToFile("autocomplete.txt", words);
        return words;
    }

    private List<String> extractWords(Collection<Entry<String, List<String>>> homepage) {
        return homepage.stream()
                .map(Entry::getKey)
                .collect(Collectors.toList());
    }

    private void sortWords(List<String> words) {
        words.sort(Comparator.naturalOrder());
    }

    private void writeToFile(String filename, List<String> words) {
        try {
            BufferedWriter writer = Files.newBufferedWriter(Paths.get(filename));
            try {
                StringBuilder sb = new StringBuilder();
                sb.append(words.size()).append("\n");
                for (String word : words) {
                    sb.append("\t0 ").append(word).append("\n");
                }
                writer.write(sb.toString());
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            System.err.println("Failed to write to file: " + filename);
            e.printStackTrace();
        }
    }

    @Override
    public List<String> searchArticles(String queryTerm, Map<?, ?> invertedIndex) {
        Object articles = invertedIndex.get(queryTerm);
        return extractArticleURLs(articles);
    }

    private List<String> extractArticleURLs(Object articles) {
        List<String> result = new ArrayList<>();
        if (articles instanceof List) {
            List<?> entries = (List<?>) articles;
            entries.stream()
                    .filter(entry -> entry instanceof Entry)
                    .map(entry -> (Entry<?, ?>) entry)
                    .filter(entry -> entry.getKey() instanceof String)
                    .forEach(entry -> result.add((String) entry.getKey()));
        }
        return result;
    }
}