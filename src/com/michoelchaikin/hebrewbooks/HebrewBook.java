package com.michoelchaikin.hebrewbooks;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.michoelchaikin.hebrewbooks.utils.HebrewBooksUtils;
import com.michoelchaikin.hebrewbooks.utils.PDFUtils;

public class HebrewBook {

	private static final String TAG = "HebrewBook";

	private int bookID;
	private int numPages;
	private String nameHebrew;
	private String nameEnglish;
	private String authorHebrew;
	private String authorEnglish;
	private String publicationPlaceHebrew;
	private String publicationPlaceEnglish;
	private String publicationDateHebrew;
	private String publicationDateEnglish;
	private String oclcID;
	private String uliEntry;
	private String source;
	private String catalogInfo;
	private String description;
	private String thumbnail;
	
	private Context context;
	private File mCacheDir;
	
	public HebrewBook(Context _context, int _bookID) {
		Log.i(TAG, "Creating new HebrewBook object. bookID = " + _bookID);
		
		this.context = _context;
		this.bookID = _bookID;
		
		String storageState = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(storageState)) {
			mCacheDir = context.getExternalCacheDir();
		} else {
			mCacheDir = context.getCacheDir();
		}
		
		Log.i(TAG, "Cache Directory " + mCacheDir);
	}
		
	public void init() throws IOException {
		Log.i(TAG, "Initializing HebrewBook..");
		
		URL url = new URL("http://www.hebrewbooks.org/" + bookID);
		File file = HebrewBooksUtils.getFileFromCacheOrURL(mCacheDir, url);
		String text = HebrewBooksUtils.readFileAsString(file);
		
		Document doc = Jsoup.parse(text);
		nameHebrew = doc.getElementById("ctl00_cpMstr_lblHebSefername").text();
		nameEnglish = doc.getElementById("ctl00_cpMstr_lblSefername").text();
		authorHebrew = doc.getElementById("ctl00_cpMstr_lblHebAuth").text();
		authorEnglish = doc.getElementById("ctl00_cpMstr_lblAuth").text();
		publicationPlaceHebrew = doc.getElementById("ctl00_cpMstr_lblHebPlace").text();
		publicationPlaceEnglish = doc.getElementById("ctl00_cpMstr_lblPlace").text();
		publicationDateHebrew = doc.getElementById("ctl00_cpMstr_lblHebDate").text();
		publicationDateEnglish = doc.getElementById("ctl00_cpMstr_lblDate").text();
		oclcID = doc.getElementById("ctl00_cpMstr_hlOCLC").text();
		uliEntry = doc.getElementById("ctl00_cpMstr_hlULI").text();
		source = doc.getElementById("ctl00_cpMstr_lblSrc").text();
		catalogInfo = doc.getElementById("ctl00_cpMstr_lblCat").text();
		description = doc.getElementById("ctl00_cpMstr_lblDesc").text();
		numPages = Integer.parseInt(doc.getElementById("ctl00_cpMstr_lblPages").text());
		thumbnail = doc.select("img[src^=thumbs]").first().attr("src");
	}
	
	public File getPage(int page) throws IOException {
		Log.i(TAG, "Retrieving page: " + page);

		URL url = getPageURL(page);
		File pdf = HebrewBooksUtils.getFileFromCacheOrURL(mCacheDir, url);
		
		return pdf;
	}
	
	public File renderPage(File pdf) throws Exception {	
		Log.i(TAG, "Rendering page: " + pdf.getAbsolutePath());

		String filename = pdf.getName();
		int lastColonPos = filename.lastIndexOf('.');
		String filenameNoColon = new String(lastColonPos == -1
									? filename
									: filename.substring(0, lastColonPos));
		File png = new File(mCacheDir, filenameNoColon + ".png");
		
		if(! png.exists()) {
			Log.i(TAG, "File does not exist, rendering");

			png = PDFUtils.extractImage(pdf, mCacheDir);
		} else {
			Log.i(TAG, "Rendered file already exists");
		}
		
		return png;
	}
	
	public File findRenderedFile(int page) {
		return new File(mCacheDir, "hebrewbooks_org_" + bookID + "_" + page + ".png");
	}

	public URL getPageURL(int page) throws MalformedURLException {
		return new URL("http://www.hebrewbooks.org/pagefeed/hebrewbooks_org_" + bookID + "_" + page + ".pdf#toolbar=1&navpanes=0&statusbar=0&view=FitH");
	}
	
	public URL getBookThumbnail() throws MalformedURLException {
		return new URL("http://www.hebrewbooks.org/" + thumbnail);
	}

	public int getBookID() {
		return bookID;
	}

	public int getNumPages() {
		return numPages;
	}

	public String getNameHebrew() {
		return nameHebrew;
	}

	public String getNameEnglish() {
		return nameEnglish;
	}

	public String getAuthorHebrew() {
		return authorHebrew;
	}

	public String getAuthorEnglish() {
		return authorEnglish;
	}

	public String getPublicationPlaceHebrew() {
		return publicationPlaceHebrew;
	}

	public String getPublicationPlaceEnglish() {
		return publicationPlaceEnglish;
	}

	public String getPublicationDateHebrew() {
		return publicationDateHebrew;
	}

	public String getPublicationDateEnglish() {
		return publicationDateEnglish;
	}

	public String getOclcID() {
		return oclcID;
	}

	public String getUliEntry() {
		return uliEntry;
	}

	public String getSource() {
		return source;
	}

	public String getCatalogInfo() {
		return catalogInfo;
	}

	public String getDescription() {
		return description;
	}
}
