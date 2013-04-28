package com.michoelchaikin.hebrewbooks.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class HebrewBooksUtils {
	private static final String TAG = "HebrewBooksUtils";
	
    // If URL is already saved in cache folder, will locate it and return it. If not, will download and save to cache
	
    public static File getFileFromCacheOrURL(File cacheDir, URL url) throws IOException {
    	Log.i(TAG, "getFileFromCacheOrURL(): url = " + url.toExternalForm());
    	
    	String filename = url.getFile();
		int lastSlashPos = filename.lastIndexOf('/');
		String fileNameNoPath = new String(lastSlashPos == -1
									? filename
									: filename.substring(lastSlashPos+1));
		
    	File file = new File(cacheDir, fileNameNoPath);
    	
    	if(file.exists()) {
    		if(file.length() > 0) {
	    		Log.i(TAG, "File exists in cache as: " + file.getAbsolutePath());
	    		return file;
    		} else {
    			Log.i(TAG, "Deleting zero length file " + file.getAbsolutePath());
    			file.delete();
    		}
    	}
    	
    	Log.i(TAG, "File " + file.getAbsolutePath() + " does not exists.");

    	URLConnection ucon = url.openConnection();
    	ucon.setReadTimeout(5000);
    	ucon.setConnectTimeout(30000);

    	InputStream is = ucon.getInputStream();
    	BufferedInputStream inStream = new BufferedInputStream(is, 1024 * 5);
    	FileOutputStream outStream = new FileOutputStream(file);
    	byte[] buff = new byte[5 * 1024];

    	// Read bytes (and store them) until there is nothing more to read(-1)
    	int len;
    	while ((len = inStream.read(buff)) != -1) {
    		outStream.write(buff, 0, len);
    	}

    	// Clean up
    	outStream.flush();
    	outStream.close();
    	inStream.close();
    	return file;
    }

    // Simple helper function to read a file into a string
    
    public static String readFileAsString(File file) throws IOException {
		FileInputStream fin = new FileInputStream(file);
		BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
	    StringBuilder sb = new StringBuilder();
	    String line = null;
	    while ((line = reader.readLine()) != null) {
	      sb.append(line).append("\n");
	    }
	    fin.close();
	    return sb.toString();
    }
    
    // Decode a bitmap from a file in required size
    
    public static Bitmap decodeBitmap(File file, int reqHeight, int reqWidth) {
    	// Get image size of file
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(file.getAbsolutePath(), options);
		final int height = options.outHeight;
		final int width = options.outWidth;

		// Calculate sample size
		if (height > reqHeight || width > reqWidth) {
			final int heightRatio = Math.round((float) height / (float) reqHeight);
			final int widthRatio = Math.round((float) width / (float) reqWidth);
			options.inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}

		// Decode bitmap
		options.inJustDecodeBounds = false;
	    options.inPreferredConfig = Bitmap.Config.RGB_565;
		options.inPurgeable = true;
		options.inDither = false;
		return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    }
}
