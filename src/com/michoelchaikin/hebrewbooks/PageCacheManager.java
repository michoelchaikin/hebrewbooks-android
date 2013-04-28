package com.michoelchaikin.hebrewbooks;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import android.util.Log;

enum PageStatus {
	PENDING,
	DOWNLOADED,
	RENDERED,
}

public class PageCacheManager {
	
	private static final String TAG = "PageCacheManager";
	
	private static final int DEFAULT_CACHE_SIZE_AHEAD = 5;
	private static final int DEFAULT_CACHE_SIZE_BEHIND = 3;
	
	private final HebrewBook mBook;
	private final List <PageStatus> mPagesStatus;
	private volatile Thread mCacheThread;
	
    private final BlockingDeque<Integer> mPageRequestsQueue = new LinkedBlockingDeque<Integer>();
	
    // For signaling that requested page is ready
	private final ReentrantLock mLock = new ReentrantLock();
	private final Condition mPageReadyCondition = mLock.newCondition();
	
	public PageCacheManager(HebrewBook book, int page) {
		Log.i(TAG, "PageCacheManager created. BookID = " + book.getBookID());
		
		mBook = book;
		mPageRequestsQueue.offerFirst(page);
		
		List<PageStatus> list = new ArrayList<PageStatus>();
		for(int i = 0; i < mBook.getNumPages() + 1; i++) {
			list.add(PageStatus.PENDING);
		}
		mPagesStatus = Collections.synchronizedList(list);
	}
	
	public PageCacheManager(HebrewBook book) {
		this(book, 0);
	}
	
	public void init() {
		// Start the background thread
		mCacheThread = new Thread(doCaching, "PageCacheManager");
		mCacheThread.start();
	}
	
	public File getPage(int page) {
		Log.i(TAG, "getPage(): waiting for page " +  page);
		
		try {
			
			if(page < 1 || page > mBook.getNumPages()) {
				Log.e(TAG, "Requesting invalid page number");
				return null;
			}
			
			// Put the requested page in the queue to be rendered
			mPageRequestsQueue.putFirst(page);
			
			// Make sure the background thread is running
			if(! mCacheThread.isAlive()) {
				mCacheThread = new Thread(doCaching, "PageCacheManager");
				mCacheThread.start();
			}
					
			// Wait for file to be rendered
			Log.i(TAG, "Waiting for page to be rendered");
			mLock.lock();
			try {
				while(mPagesStatus.get(page) != PageStatus.RENDERED) {
					mPageReadyCondition.await();
				}
			} finally {
				mLock.unlock();
			}
			Log.i(TAG, "Recieved signal that page was rendered");
				
			// Find the file
			File file = mBook.findRenderedFile(page);
			return file;
			
		} catch (InterruptedException e1) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Unexpected interruption");
		}
		
	}
	
	private int getNextPageToDownload(int lastRequest) {
	
		// Is there a page we have requested but hasn't been done yet?
		if((lastRequest > 0) && (mPagesStatus.get(lastRequest) == PageStatus.PENDING)) {
			return lastRequest;
		}
		
		// Check ahead if any pages need to be cached
		
		int checkAhead = (lastRequest + 1 + DEFAULT_CACHE_SIZE_AHEAD) <= mBook.getNumPages()
				? lastRequest + 1 + DEFAULT_CACHE_SIZE_AHEAD
				: mBook.getNumPages();
				
		for(int i = lastRequest + 1; i < checkAhead; i++) {
			if(mPagesStatus.get(i) == PageStatus.PENDING) {
				return i;
			}
		}
		
		// Check behind if any pages need to be cached
		int checkBehind = (lastRequest - 1 - DEFAULT_CACHE_SIZE_BEHIND) > 0
				? lastRequest - 1 - DEFAULT_CACHE_SIZE_BEHIND
				: 1;

		for(int i = lastRequest; i > checkBehind; i--) {
			if(mPagesStatus.get(i) == PageStatus.PENDING) {
				return i;
			}
		}
		
		// Cache is up to date
		return 0;
	}
	
	private final Runnable doCaching = new Runnable() {

		public void run() {
			
			Log.i(TAG, "PageCacheThread starting");
			
			try {
			
				while(true) {
					
					// Wait until we have a page requested
					Log.i(TAG, "Waiting for page request");
					int lastRequest = mPageRequestsQueue.takeFirst();
					
					int page = getNextPageToDownload(lastRequest);
					
					while(page != 0) {
						
						// Download and render the file
						
						try {
							File pdf = mBook.getPage(page);
							mPagesStatus.set(page, PageStatus.DOWNLOADED);
							File png = mBook.renderPage(pdf);
							
							// If something went wrong, take a second try
							if(png == null || png.exists() != true) {
								pdf.delete();
								png.delete();
								pdf = mBook.getPage(page);
								png = mBook.renderPage(pdf);
							}
							mPagesStatus.set(page, PageStatus.RENDERED);
						} catch (Exception e) {
							// TODO: Better error handling
							Log.e(TAG, "Error in PageCacheThread: " + e.toString());
						}
						
						Log.i(TAG, "Signalling that we have rendered a page");
						try {
							mLock.lock();
							mPageReadyCondition.signalAll();
						} finally {
							mLock.unlock();
						}
						
						// If we have a new request, forget about the current one
						if(mPageRequestsQueue.size() > 0) {
							Log.i(TAG, "New page request on queue!");
							page = 0;
						} else {
							page = getNextPageToDownload(lastRequest);
						}
						
					}
	
				}
			} catch (InterruptedException iex) {
				// Interruption will just end the Runnable
			}
			
						
			// TODO: investigate possible performance benefits of rendering in separate thread while downloading next page
			// TODO: change code to use ExecutorService to manage threads
		}
		
	};
	
	public HebrewBook getBook() {
		return mBook;
	}
	
}
