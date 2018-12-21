package com.example.lucene;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

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
import org.apache.lucene.queryparser.classic.ParseException;
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

	public static void main(String[] args) throws IOException, ParseException {
		// TODO Auto-generated method stub
		String indexDir = "";
		int auswahl;

		///////////////////////////////////////////////////////////
		// load the properties for index and data directories//////

		Properties prop = new Properties();
		InputStream input = null;

		try {

			input = new FileInputStream("config.properties");

			// load a properties file
			prop.load(input);

			// get the property value and print it out
			indexDir = prop.getProperty("indexDir");

		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		//////////////////////////////////////////////////////
		StandardAnalyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig config = new IndexWriterConfig(analyzer);

		Directory dir = FSDirectory.open(Paths.get(indexDir));
		IndexReader reader = DirectoryReader.open(dir);
		final Fields fields = MultiFields.getFields(reader);
		final Iterator<String> iterator = fields.iterator();
		IndexSearcher searcher = new IndexSearcher(reader);

		System.out.println("Geben Sie Ihr Auswahl ein: ");
		System.out.println("1. CSV Datei erstellen ");
		System.out.println("2. Meta Information ausgeben ");
		Scanner sc = new Scanner(System.in);
		auswahl = sc.nextInt();
		switch (auswahl) {
		case 1:
			createCSV(iterator, reader, analyzer, searcher);
			break;
		case 2:
			printMetaInfo(reader);
		}

	}

	static private void printMetaInfo(IndexReader reader) throws IOException {
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
			IndexSearcher searcher) throws IOException, ParseException {
		BufferedWriter writer = new BufferedWriter(new FileWriter("word_counter.csv"));
		String freqTerm = "";
		String tmp;
		String test = "";

		String querystr = "";
		ScoreDoc[] hits;
		List<String> fileNames;
		/*
		List<String> blacklist = new ArrayList<>();
		List<String> whitelist = new ArrayList<>();
		List<String> list = new ArrayList<>();
		
		File blacklistFile = new File("E:\\DevTools\\Eclipse\\workspace\\LuceneWordCounter\\blacklist.txt");
		if(blacklistFile.exists()) {
			getBlacklistFromText(blacklistFile,blacklist);
			//list = blacklist;
		}
		
		File whitelistFile = new File("E:\\DevTools\\Eclipse\\workspace\\LuceneWordCounter\\whitelist.txt");
		if(whitelistFile.exists()) {
			getWhitelistFromText(whitelistFile,whitelist);
			//list = whitelist;
		}
		
		System.out.println("Blacklist size: " + blacklist.size());
		System.out.println("Whitelist size: " + whitelist.size());

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
				tmp += ";";

				// add blacklist flag to word
				if(blacklist.size() > 0) {
					for(String bl : blacklist) {
						if(freqTerm.equals(bl)) {
							 tmp += "blacklist";
							    System.out.println("Blacklist found");
								System.out.println("FreqTerm: " + freqTerm);
								System.out.println(tmp);
						}
					}
				}
				
				// add blacklist flag to word
				if(whitelist.size() > 0) {
					for(String wh : whitelist) {
						if(freqTerm.equals(wh)) {
							 tmp += "whitelist";
							    System.out.println("Whitelist found");
								System.out.println("FreqTerm: " + freqTerm);
								System.out.println(tmp);
						}
					}
				}
				
				/*
				for (String s : list) {
					if (freqTerm.equals(s)) {
						if(blacklist.size() > 0) {
						   
						}
						if(whitelist.size() > 0) {
						    tmp += "whitelist";
						    System.out.println("Whitelist found");
							System.out.println("FreqTerm: " + freqTerm);
							System.out.println(tmp);
						}
					}
				}
				*/

				writer.write(tmp);
				writer.newLine();
			}
			term = it.next();
		}
		
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
			String term) throws ParseException, IOException {
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
	static private void getBlacklistFromText(File file,List<String> blacklist) throws IOException {

		List<String> lineString = new ArrayList<>();

		BufferedReader br = new BufferedReader(new FileReader(file));
		String line;
		while ((line = br.readLine()) != null) {
			lineString.add(line);
			// System.out.println("Line: " + line);
		}

		for (String str : lineString) {
			blacklist.addAll(Arrays.asList(str.split(";")));
		}

	}
	
	static private void getWhitelistFromText(File file,List<String> whitelist) throws IOException {
		List<String> lineString = new ArrayList<>();

		BufferedReader br = new BufferedReader(new FileReader(file));
		String line;
		while ((line = br.readLine()) != null) {
			lineString.add(line);
		}

		for (String str : lineString) {
			whitelist.addAll(Arrays.asList(str.split(";")));
		}

	}
	*/

}
