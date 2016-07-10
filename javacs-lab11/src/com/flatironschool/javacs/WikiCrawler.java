package com.flatironschool.javacs;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import redis.clients.jedis.Jedis;


public class WikiCrawler {
	// keeps track of where we started
	private final String source;

	// the index where the results go
	private JedisIndex index;

	// queue of URLs to be indexed
	private Queue<String> queue = new LinkedList<String>();

	// fetcher used to get pages from Wikipedia
	final static WikiFetcher wf = new WikiFetcher();

	/**
	 * Constructor.
	 *
	 * @param source
	 * @param index
	 */
	public WikiCrawler(String source, JedisIndex index) {
		this.source = source;
		this.index = index;
		queue.offer(source);
	}

	/**
	 * Returns the number of URLs in the queue.
	 *
	 * @return
	 */
	public int queueSize() {
		return queue.size();
	}

	/**
	 * Gets a URL from the queue and indexes it.
	 * @param b
	 *
	 * @return Number of pages indexed.
	 * @throws IOException
	 */
	public String crawl(boolean testing) throws IOException {
        // FILL THIS IN!
		/* remove from queue in FIFO order */
		String url = queue.poll();
		if (url == null) {
			// TODO what to do when it's null?
			return null;
		}
		if (testing == true) {
			Elements paras = wf.readWikipedia(url);
			System.out.println("**** reading url: " + url);
			/* index page no matter whether it has been indexed before */
			index.indexPage(url, paras);
			queueInternalLinks(paras);
		} else {  // testing == false
			Elements paras = wf.fetchWikipedia(url);
			System.out.println("**** fetching url: " + url);
			/* only index page if not yet indexed */
		}
		/* return URL of page just indexed */
		return url;
	}

	/**
	 * Parses paragraphs and adds internal links to the queue.
	 *
	 * @param paragraphs
	 */
	// NOTE: absence of access level modifier means package-level
	void queueInternalLinks(Elements paragraphs) {
    // FILL THIS IN!
		for (Element para: paragraphs) {
			Iterable<Node> iter = new WikiNodeIterable(para);
			for (Node node: iter) {
				// process elements to find links
				if (node instanceof Element) {
					// Element link = processElement((Element) node);
	        processElement((Element) node);
	        // if (link != null) {
					// 	return link;
					// }
				}
			}
			// return null;
		}
	}

	/* the following are with help from the Wiki Philosophy lab
	 * solutions: https://github.com/learn-co-students/cs-wikipedia-philosophy-lab-codeU/blob/solution/javacs-lab05/src/com/flatironschool/javacs/WikiParser.java */

	/**
	 * Returns the element if it is a valid link, null otherwise.
	 */
	private void processElement(Element elt) {
		if (validLink(elt)) {
			System.out.println("offering link: " + "https://en.wikipedia.org" + elt.attr("href"));
			queue.offer("https://en.wikipedia.org" + elt.attr("href"));
			// return elt;
		}
		// return null;
	}

	/**
	 * Checks whether a link is valid.
	 */
	private boolean validLink(Element elt) {
		// it's no good if it's not a link
		if (!elt.tagName().equals("a")) {
			return false;
		}
		System.out.println(elt.tagName());
		// an external link
		if (!startsWith(elt, "/wiki/")) {
			return false;
		}
		System.out.println("href: " + elt.attr("href"));
		return true;
	}

	/**
	 * Checks whether a link starts with a given String.
	 */
	private boolean startsWith(Element elt, String s) {
		return (elt.attr("href").startsWith(s));
	}

	public static void main(String[] args) throws IOException {
		// make a WikiCrawler
		Jedis jedis = JedisMaker.make();
		JedisIndex index = new JedisIndex(jedis);
		String source = "https://en.wikipedia.org/wiki/Java_(programming_language)";
		WikiCrawler wc = new WikiCrawler(source, index);

		// for testing purposes, load up the queue
		Elements paragraphs = wf.fetchWikipedia(source);
		wc.queueInternalLinks(paragraphs);

		// loop until we index a new page
		String res;
		do {
			res = wc.crawl(false);

            // REMOVE THIS BREAK STATEMENT WHEN crawl() IS WORKING
            break;
		} while (res == null);

		Map<String, Integer> map = index.getCounts("the");
		for (Entry<String, Integer> entry: map.entrySet()) {
			System.out.println(entry);
		}
	}
}
