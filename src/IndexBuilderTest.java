import static org.junit.Assert.*;

import java.util.*;
import java.util.Map.Entry;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ericfouh
 */
public class IndexBuilderTest {
    private IndexBuilder indexBuilder;
    private Map<String, List<String>> parsedFeed;
    private Map<String, Map<String, Double>> index;
    private Map<?, ?> invertedIndex;

    private static final String RSS_FEED_URL =
            "https://www.cis.upenn.edu/~cit5940/sample_rss_feed.xml";
    private static final String[] EXPECTED_PAGES = {
        "https://www.seas.upenn.edu/~cit5940/page1.html",
        "https://www.seas.upenn.edu/~cit5940/page2.html",
        "https://www.seas.upenn.edu/~cit5940/page3.html",
        "https://www.seas.upenn.edu/~cit5940/page4.html",
        "https://www.seas.upenn.edu/~cit5940/page5.html"
    };

    @Before
    public void setUp() {
        indexBuilder = new IndexBuilder();
        List<String> testFeed = Collections.singletonList(RSS_FEED_URL);
        parsedFeed = indexBuilder.parseFeed(testFeed);
        index = indexBuilder.buildIndex(parsedFeed);
        invertedIndex = indexBuilder.buildInvertedIndex(index);
    }

    @Test
    public void testParseFeedReturnsCorrectNumberOfPages() {
        assertEquals("Should parse all pages from RSS feed",
                EXPECTED_PAGES.length, parsedFeed.size());

        for (String page : EXPECTED_PAGES) {
            assertTrue("All expected pages should be present",
                    parsedFeed.containsKey(page));
        }
    }

    @Test
    public void testParseFeedPageContentNotEmpty() {
        for (List<String> words : parsedFeed.values()) {
            assertFalse("Each page should have some content",
                    words.isEmpty());
        }
    }

    @Test
    public void testBuildIndexCreatesCorrectTFIDFValues() {
        Map<String, Double> page1Terms = index.get(EXPECTED_PAGES[0]);
        assertNotNull("Page terms should not be null", page1Terms);

        assertTrue("Common term should be present", page1Terms.containsKey("data"));
        assertEquals(0.1021, page1Terms.get("data"), 0.0001);

        assertTrue("Another common term should be present", page1Terms.containsKey("lists"));
        assertEquals(0.0916, page1Terms.get("lists"), 0.0001);
    }

    @Test
    public void testBuildInvertedIndexOrganizesByTerm() {
        List<?> linkedListEntries = (List<?>) invertedIndex.get("linkedlist");
        assertNotNull("Term should exist in inverted index", linkedListEntries);
        assertEquals("Term should appear in correct number of pages",
                2, linkedListEntries.size());
    }

    @Test
    public void testBuildHomePageOrdersByFrequency() {
        Collection<Entry<String, List<String>>> homePage =
                indexBuilder.buildHomePage(invertedIndex);
        List<Entry<String, List<String>>> homePageList = new ArrayList<>(homePage);

        Entry<String, List<String>> firstEntry = homePageList.get(0);
        assertEquals("Most frequent term should be first", "data", firstEntry.getKey());
        assertTrue("Frequent term should appear in multiple pages",
                firstEntry.getValue().size() > 1);
    }

    @Test
    public void testSearchArticlesReturnsCorrectPages() {
        List<String> articles = indexBuilder.searchArticles("data", invertedIndex);
        assertEquals("Term should appear in correct number of pages", 3, articles.size());

        assertEquals(EXPECTED_PAGES[0], articles.get(0));
        assertEquals(EXPECTED_PAGES[1], articles.get(1));
    }

    @Test
    public void testCreateAutocompleteFileContainsAllTerms() {
        Collection<Entry<String, List<String>>> homePage =
                indexBuilder.buildHomePage(invertedIndex);
        Collection<?> autoCompleteTerms =
                indexBuilder.createAutocompleteFile(homePage);

        assertEquals("Autocomplete file should contain all terms",
                homePage.size(), autoCompleteTerms.size());
    }
}