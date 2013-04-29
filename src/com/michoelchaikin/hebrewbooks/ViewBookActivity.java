package com.michoelchaikin.hebrewbooks;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.michoelchaikin.hebrewbooks.ui.PageView;
import com.michoelchaikin.hebrewbooks.utils.HebrewBooksUtils;


public class ViewBookActivity extends Activity {

	private static final String TAG = "ViewBookActivity";
	private static final int TEST_BOOK_ID = 15860;

	private HebrewBook mBook = null;
	private volatile PageCacheManager mCacheManager = null;

	private int mCurrentPage = 0;
	private Button mButPrev;
	private Button mButNext;
	private PageView mPageView;
	private TextView mTextPage;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Make sure we have Internet connection

		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo == null || netInfo.isConnected() == false) {
			Toast.makeText(this, "Internet connection not available", Toast.LENGTH_LONG).show();
			finish();
		}

		createBook();

		// UI Stuff

		setContentView(R.layout.activity_view_book);

		new InitBook().execute();

		mButPrev = (Button) findViewById(R.id.butPrev);
		mButNext = (Button) findViewById(R.id.butNext);
		mPageView = (PageView) findViewById(R.id.pageView);
		mTextPage = (TextView) findViewById(R.id.textPage);

		mButNext.setEnabled(false);
		mButPrev.setEnabled(false);
	}

	private void createBook() {

		/* The application can handle two types of URLS:
		 * 
		 *  1) http://www.hebrewbooks.org/pdfpager.aspx?req=[BOOK_ID]&pgnum=[BOOK_ID]
		 *  2) http://www.hebrewbooks.org/pagefeed/hebrewbooks_org_[BOOK_ID]_[BOOK_ID].pdf
		 * 
		 * The first is the usual address used for viewing books on the site
		 * The seconds is the address of the actual PDF file - it is usefull to handle such
		 * links because they can come up in search results.
		 * 
		 */

		int bookID = 0, page = 0;
		final Intent intent = getIntent();
		final String action = intent.getAction();

		if (Intent.ACTION_VIEW.equals(action)) {
			Uri data = intent.getData();
			String path = data.getPath();

			// Try first type of URL
			if(path.startsWith("/pdfpager.aspx")) {        	
				bookID = HebrewBooksUtils.parseIntNoException(data.getQueryParameter("req"));
				page = HebrewBooksUtils.parseIntNoException(data.getQueryParameter("pgnum"));
			}

			// Try the second type of URL
			else if (path.startsWith("/pagefeed/")) {
				Pattern pattern = Pattern.compile("^/pagefeed/hebrewbooks_org_([\\d]*)_([\\d])*\\.pdf.*$");
				Matcher matcher = pattern.matcher(path);
				if(! matcher.matches()) {
					Toast.makeText(this, "Could not open book: URL not understood", Toast.LENGTH_LONG).show();
					finish();
				}
				bookID = HebrewBooksUtils.parseIntNoException(matcher.group(1));
				page = HebrewBooksUtils.parseIntNoException(matcher.group(2)); 
			} 

			// Unrecognized URL type
			else {
				Toast.makeText(this, "Could not open book: Unrecognized URL", Toast.LENGTH_LONG).show();
				finish();
			}
		} else if (Intent.ACTION_MAIN.equals(action)) {
			// Started from app launcher
			bookID = TEST_BOOK_ID;
			page = 25;
		}

		if(bookID == 0) {
			Toast.makeText(this, "Could not open book: No book request found", Toast.LENGTH_LONG).show();
			finish();
		}
		if(page == 0) page = 1;

		mCurrentPage = page;
		mBook = new HebrewBook(this, bookID);
	}

	class InitBook extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Void... params) {
			try {
				mBook.init();
			} catch (IOException e) {
				Log.e(TAG, "InitBook error: " + e.toString());
				return false;
			}
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);

			if(result != true) {
				Toast.makeText(ViewBookActivity.this, "Error reading book information. Please ensure book is valid and Internet connection available", Toast.LENGTH_LONG).show();
				finish();
			}

			setTitle(mBook.getNameHebrew() + " (" + mBook.getAuthorHebrew() + ")");

			mCacheManager = new PageCacheManager(mBook);
			mCacheManager.init();

			loadPage(mCurrentPage);
		}
	}

	private void loadPage(int page) {

		if(mBook == null || page < 1 || page > mBook.getNumPages()) {
			return;
		}

		mCurrentPage = page;
		mPageView.loadPage(mCacheManager, page);
		mButPrev.setEnabled(page > 1);
		mButNext.setEnabled(page < mBook.getNumPages());
		mTextPage.setText(page + "/" + mBook.getNumPages());
	}

	public void butPrev_onClick(View v) {
		loadPage(mCurrentPage-1);
	}

	public void butNext_onClick(View v) {
		loadPage(mCurrentPage+1);
	}

	public void textPage_onClick(View v) {			 
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Go to page");
		final EditText input = new EditText(this);
		input.setText(mCurrentPage + "");
		input.setInputType(InputType.TYPE_CLASS_NUMBER);
		builder.setView(input);
		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString();
				int page = Integer.parseInt(value);
				loadPage(page);
				return;
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				return;
			}
		});
		Dialog dialog = builder.create();
		dialog.show();
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.view_book, menu);
		return true;
	}

}
