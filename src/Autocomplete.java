import java.util.List;
import java.io.*;
import java.util.*;

public class Autocomplete implements IAutocomplete {
    private Node root;
    private int k;

    public Autocomplete() {
        root = new Node("", 0);
    }

    @Override
    public void addWord(String word, long weight) {
        if (word == null) {
            return;
        }

        word = word.toLowerCase();
        for (char ch : word.toCharArray()) {
            if (!Character.isLetter(ch)) {
                return;
            }
        }

        Node current = root;
        for (int i = 0; i < word.length(); i++) {
            char currentChar = word.charAt(i);
            int index = currentChar - 'a';
            current.setPrefixes(current.getPrefixes() + 1);

            if (current.getReferences()[index] == null) {
                current.getReferences()[index] = new Node();
            }
            current = current.getReferences()[index];
        }

        current.setWords(1);
        current.setTerm(new Term(word, weight));
        current.setPrefixes(current.getPrefixes() + 1);
    }

    @Override
    public Node buildTrie(String filename, int k) {
        this.k = k;

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] content = line.trim().split("\t");
                if (content.length == 2) {
                    try {
                        long weight = Long.parseLong(content[0]);
                        String term = content[1];
                        addWord(term, weight);
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading file: " + filename, e);
        }

        return root;
    }

    @Override
    public int numberSuggestions() {
        return this.k;
    }

    @Override
    public Node getSubTrie(String prefix) {
        if (prefix == null) {
            return null;
        }

        prefix = prefix.toLowerCase();
        Node current = root;

        for (int i = 0; i < prefix.length(); i++) {
            char ch = prefix.charAt(i);
            if (!Character.isLetter(ch)) {
                return null;
            }

            int index = ch - 'a';
            Node next = current.getReferences()[index];
            if (next == null) {
                return null;
            }

            current = next;
        }
        return current;
    }

    @Override
    public int countPrefixes(String prefix) {
        Node node = getSubTrie(prefix);
        if (node != null) {
            return node.getPrefixes();
        } else {
            return 0;
        }
    }

    @Override
    public List<ITerm> getSuggestions(String prefix) {
        List<ITerm> suggestions = new ArrayList<>();
        Node startNode = getSubTrie(prefix);
        if (startNode == null) {
            return suggestions;
        }

        Queue<Node> queue = new LinkedList<>();
        queue.add(startNode);
        while (!queue.isEmpty()) {
            Node pulledOut = queue.poll();
            if (pulledOut.getWords() != 0) {
                suggestions.add(new Term(pulledOut.getTerm().getTerm(),
                        pulledOut.getTerm().getWeight()));
            }
            for (Node child: pulledOut.getReferences()) {
                if (child != null) {
                    queue.offer(child);
                }
            }
        }
        suggestions.sort(ITerm.byReverseWeightOrder());
        return suggestions;
    }
}