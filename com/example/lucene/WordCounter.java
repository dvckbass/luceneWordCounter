package com.example.lucene;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;

public class WordCounter {

	public static final String PROPERTY_INDEX_DIRECTORY = "indexDir";
	public static final String PROPERTY_FILE = "config.properties";

	private String indexDir;

	public static void main(String[] args) throws IOException, org.apache.lucene.queryparser.classic.ParseException, ParseException {
		// TODO Auto-generated method stub
		WordCounter wordCounter = new WordCounter();
		wordCounter.loadProp();
		
		StandardAnalyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig config = new IndexWriterConfig(analyzer);

		Directory dir = FSDirectory.open(Paths.get(wordCounter.indexDir));
		IndexReader reader = DirectoryReader.open(dir);
		final Fields fields = MultiFields.getFields(reader);
		final Iterator<String> iterator = fields.iterator();
		IndexSearcher searcher = new IndexSearcher(reader);
		
		

		wordCounter.parseCL(args, iterator, reader, analyzer, searcher);
		
		
		
		
		
		
		
		
		
		
	}

	// Konfigurationsdatei aufrufen
	private void loadProp() throws IOException {
		Properties prop = new Properties();
		InputStream input = null;

		input = new FileInputStream(PROPERTY_FILE);
		prop.load(input);

		indexDir = prop.getProperty(PROPERTY_INDEX_DIRECTORY);
	}

	// Kommandozeilenparameter parsen
	private void parseCL(String[] args, Iterator<String> iterator, IndexReader reader, StandardAnalyzer analyzer,
			IndexSearcher searcher) throws IOException, ParseException, org.apache.lucene.queryparser.classic.ParseException {
		Options options = new Options();
		options.addOption("c", false, "CSV-Datei erstellen");
		options.addOption("m", false, "Metainformationen ausgeben");
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);

		if (cmd.hasOption("c")) {
			createCSV(iterator, reader, analyzer, searcher);
		} else if(cmd.hasOption("m")) {
			printMetaInfo(reader);
		}
	}

	private void printMetaInfo(IndexReader reader) throws IOException {
		String fileName, title, author, size, date, encoding, terms;
		Bits liveDocs = MultiFields.getLiveDocs(reader);
		IndexableField body;
		// List<IndexableField> fields;
		Fields field;

		Scanner sc = new Scanner(System.in);
		int i = 0;
		char input = 0;
		do {
			if (liveDocs != null && !liveDocs.get(i))
				continue;

			Document doc = reader.document(i);
			/*
			 * fields = doc.getFields(); for(int c=0;c<fields.size();c++) {
			 * System.out.println(fields.get(i).name()); }
			 */
			// field = MultiFields.getFields(reader);

			// body = doc.getField("body");
			// System.out.println("Body: " + body.stringValue());
			fileName = doc.get("fileName");
			date = doc.get("date");
			size = doc.get("size");
			author = doc.get("author");
			title = doc.get("title");
			encoding = doc.get("encoding");
			terms = doc.get("terms");
			System.out.println("Number: " + (i + 1));
			System.out.println("file name: " + fileName);
			System.out.println("date: " + date);
			System.out.println("size: " + size);
			if (author != null) {
				System.out.println("author: " + author);
			}
			System.out.println("title: " + title);
			System.out.println("encoding: " + encoding);
			System.out.println("terms: " + terms);
			System.out.println("\n");

			if ((i + 1) % 10 != 0) {
				i++;
			} else {
				input = sc.next().charAt(0);
				if (input == 'n') {
					i++;
				}
				if (input == 'q') {
					break;
				}
			}

		} while (i < reader.maxDoc());

	}

	static private void createCSV(Iterator<String> iterator, IndexReader reader, StandardAnalyzer analyzer,
			IndexSearcher searcher) throws IOException, org.apache.lucene.queryparser.classic.ParseException {
		BufferedWriter writer = new BufferedWriter(new FileWriter("word_counter.csv"));
		String freqTerm = "";
		String tmp;
		String test = "";

		String querystr = "";
		ScoreDoc[] hits;
		List<String> fileNames;
		/*
		 * List<String> blacklist = new ArrayList<>(); List<String> whitelist = new
		 * ArrayList<>(); List<String> list = new ArrayList<>();
		 * 
		 * File blacklistFile = new
		 * File("E:\\DevTools\\Eclipse\\workspace\\LuceneWordCounter\\blacklist.txt");
		 * if(blacklistFile.exists()) { getBlacklistFromText(blacklistFile,blacklist);
		 * //list = blacklist; }
		 * 
		 * File whitelistFile = new
		 * File("E:\\DevTools\\Eclipse\\workspace\\LuceneWordCounter\\whitelist.txt");
		 * if(whitelistFile.exists()) { getWhitelistFromText(whitelistFile,whitelist);
		 * //list = whitelist; }
		 * 
		 * System.out.println("Blacklist size: " + blacklist.size());
		 * System.out.println("Whitelist size: " + whitelist.size());
		 * 
		 */
		final String field = "contents";
		final Terms terms = MultiFields.getTerms(reader, field);
		final TermsEnum it = terms.iterator();
		BytesRef term = it.next();
		while (term != null) {
			final long freq = it.totalTermFreq();
			freqTerm = term.utf8ToString();
			freqTerm = validate(freqTerm);

			if (freqTerm != "") {
				tmp = freqTerm + ";" + freq + ";";
				// System.out.println(tmp);
				hits = searchTerm(analyzer, reader, searcher, freqTerm);

				fileNames = getTitle(hits, searcher, freqTerm);
				for (String fileName : fileNames) {
					tmp += fileName + " ";
				}
				/*
				 * tmp += ";";
				 * 
				 * // add blacklist flag to word if(blacklist.size() > 0) { for(String bl :
				 * blacklist) { if(freqTerm.equals(bl)) { tmp += "blacklist";
				 * System.out.println("Blacklist found"); System.out.println("FreqTerm: " +
				 * freqTerm); System.out.println(tmp); } } }
				 * 
				 * // add blacklist flag to word if(whitelist.size() > 0) { for(String wh :
				 * whitelist) { if(freqTerm.equals(wh)) { tmp += "whitelist";
				 * System.out.println("Whitelist found"); System.out.println("FreqTerm: " +
				 * freqTerm); System.out.println(tmp); } } }
				 * 
				 * /* for (String s : list) { if (freqTerm.equals(s)) { if(blacklist.size() > 0)
				 * {
				 * 
				 * } if(whitelist.size() > 0) { tmp += "whitelist";
				 * System.out.println("Whitelist found"); System.out.println("FreqTerm: " +
				 * freqTerm); System.out.println(tmp); } } }
				 */

				writer.write(tmp);
				writer.newLine();
			}
			term = it.next();
		}
		System.out.println("CSV Created");

	}

	static private String validate(String string) throws IOException {
		String tmp = "";
		String[] splitString = (string.split("\\s+"));
		for (String s : splitString) {
			if (s.matches("^[a-zA-Z]+.*")) {
				tmp = s;
			}
		}
		return tmp;
	}

	static private ScoreDoc[] searchTerm(StandardAnalyzer analyzer, IndexReader reader, IndexSearcher searcher,
			String term) throws IOException, org.apache.lucene.queryparser.classic.ParseException {
		// Query q = new QueryParser("terms", analyzer).parse(term);
		Query q = new QueryParser("contents", analyzer).parse(term);

		int hitsPerPage = 100;

		TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage);
		searcher.search(q, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;
		return hits;
	}

	/*
	 * static private void printTerm(ScoreDoc[] hits,IndexSearcher searcher, String
	 * term) throws IOException { for(int i=0;i<hits.length;++i) { int docId =
	 * hits[i].doc; Document d = searcher.doc(docId); System.out.println("Term: " +
	 * term + "\t" + "File name: " + d.get("fileName")); } }
	 */

	static private List<String> getTitle(ScoreDoc[] hits, IndexSearcher searcher, String term) throws IOException {
		List<String> fileNames = new ArrayList();
		String fileName;
		for (int i = 0; i < hits.length; ++i) {
			int docId = hits[i].doc;
			Document d = searcher.doc(docId);
			fileName = d.get("fileName");
			fileNames.add(fileName);
		}
		return fileNames;
	}

	/*
	 * static private void getBlacklistFromText(File file,List<String> blacklist)
	 * throws IOException {
	 * 
	 * List<String> lineString = new ArrayList<>();
	 * 
	 * BufferedReader br = new BufferedReader(new FileReader(file)); String line;
	 * while ((line = br.readLine()) != null) { lineString.add(line); //
	 * System.out.println("Line: " + line); }
	 * 
	 * for (String str : lineString) {
	 * blacklist.addAll(Arrays.asList(str.split(";"))); }
	 * 
	 * }
	 * 
	 * static private void getWhitelistFromText(File file,List<String> whitelist)
	 * throws IOException { List<String> lineString = new ArrayList<>();
	 * 
	 * BufferedReader br = new BufferedReader(new FileReader(file)); String line;
	 * while ((line = br.readLine()) != null) { lineString.add(line); }
	 * 
	 * for (String str : lineString) {
	 * whitelist.addAll(Arrays.asList(str.split(";"))); }
	 * 
	 * }
	 */

}
