package com.michoelchaikin.hebrewbooks.ui;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;

import java.io.File;
import java.lang.ref.WeakReference;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;

import com.michoelchaikin.hebrewbooks.PageCacheManager;
import com.michoelchaikin.hebrewbooks.R;
import com.michoelchaikin.hebrewbooks.utils.HebrewBooksUtils;

public class PageView extends ImageViewTouch {

	@SuppressWarnings("unused")
	private static final String TAG = "PageView";

	private AsyncTask<Void, Void, File> mGetPageTask = null;

	public PageView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public void loadPage(PageCacheManager cacheManager, int page) {

		// Cancel any pending load requests
		if(mGetPageTask != null) {
			mGetPageTask.cancel(true);
		}
		
		// Clear for now (TODO: change to loading indicator)
		setImageResource(android.R.color.white);

		mGetPageTask = new LoadPageAsyncTask(cacheManager, page, this);
		mGetPageTask.execute();
	}
}

class LoadPageAsyncTask extends AsyncTask<Void, Void, File> {

	private static final String TAG = "LoadPageAsyncTask";

	private final WeakReference<PageView> mPageViewReference;
	private final PageCacheManager mCacheManager;
	private final int mPage;

	public LoadPageAsyncTask(PageCacheManager cacheManager, int page, PageView pageView) {
		mPageViewReference = new WeakReference<PageView>(pageView);
		mCacheManager = cacheManager;
		mPage = page;
	}

	@Override
	protected File doInBackground(Void... params) {
		Log.i(TAG, "loadPage(), asking page manager for page");
		File file = mCacheManager.getPage(mPage);
		Log.i(TAG, "loadPage(), got page, passing to UI thread");
		return file;
	}

	@Override
	protected void onPostExecute(File file) {
		
		// Make sure that our view is still around
		if (mPageViewReference == null) return;
		final PageView pageView = mPageViewReference.get();
		if (pageView == null) return;

		if(file != null && file.exists()) {
			Log.i(TAG, "LoadPage, in UI thread, decoding bitmap");
			int reqHeight = (int) Math.round(pageView.getHeight() * 2);
			int reqWidth = (int) Math.round(pageView.getWidth() * 2);;
			Bitmap bm = HebrewBooksUtils.decodeBitmap(file, reqHeight, reqWidth);
			Log.i(TAG, "LoadPage, done decoding, setting as image bitmap");
			pageView.setImageBitmap(bm);
		} else {
			pageView.setImageResource(R.drawable.error);
		}				
	}
}
