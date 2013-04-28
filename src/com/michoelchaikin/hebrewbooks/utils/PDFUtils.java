package com.michoelchaikin.hebrewbooks.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.util.Log;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.ImageRenderInfo;
import com.itextpdf.text.pdf.parser.PdfImageObject;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.RenderListener;
import com.itextpdf.text.pdf.parser.TextRenderInfo;

public class PDFUtils {
	private static final String TAG = "PDFUtils";
	
	// Expects a single page PDF with one image. Extracts the image, and returns the file with the image
	public static File extractImage(final File pdf, final File dir)  {
		Log.i(TAG, "extractImage(): PDF file " + pdf.getAbsolutePath());

		// Prepare the output file	
		
		String infile = pdf.getName();
		int lcp = infile.lastIndexOf('.');
		String outfile = new String(lcp == -1 ? infile : infile.substring(0, lcp));
		final File output = new File(dir, outfile + ".png");
		
		if(output.exists()) {
			if(output.length() > 0) {
				Log.i(TAG, "extractImage(): image already exists " + output.getAbsolutePath() + ". Returning it");
				return output;
			} else {
				Log.i(TAG, "extractImage(): deleting zero size file " + output.getAbsolutePath());
				output.delete();
			}
		}
				
		// Open PDF
		
		PdfReader reader;
		try {
			reader = new PdfReader(pdf.getAbsolutePath());
		} catch (IOException e) {
			Log.e(TAG, "extractImage(): Could not open PDF file " + e.getMessage());
			return null;
		}
		
		// Parsing Code
		
		PdfReaderContentParser parser = new PdfReaderContentParser(reader);
		RenderListener listener = new RenderListener () {
			public void renderImage(ImageRenderInfo renderInfo) {
				try {
					// Get image
					PdfImageObject image = renderInfo.getImage();
					if (image != null) {
						
						// Check image type
						String imageType = image.getFileType();
						if (imageType == "png" || imageType == "gif" || imageType == "jpg") {

							// Only expecting there to be one image, so just overwrite if there is more
							if(output.exists()) {
								Log.w(TAG, "imageExtract(): overwriting image - PDF contains more than one image");
								output.delete();
							}
							
							// Write the file
							BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(output));
							bos.write(image.getImageAsBytes());
							bos.flush();
							bos.close();
						}
					}
					
				} catch (IOException e) {
					Log.e(TAG, "imageExtract(): Failed to extract image " + e.getMessage());
				} catch (OutOfMemoryError e) {
					Log.e(TAG, "imageExtract(): Out of memory in image extraction " + e.getMessage());
				}
			}

			// Nothing to do, just required methods
			public void renderText(TextRenderInfo renderInfo) {}
			public void beginTextBlock() {}
			public void endTextBlock() {}
			
		};
		
		// Parse the PDF
		try {
			parser.processContent(1, listener);
		} catch (IOException e) {
			Log.e(TAG, "imageExtract(): Error tyring to parse PDF " + e.getMessage());
		}
		
		
		// If everything went well the output file will exist, return it, otherwise return null to indicate error
		
		return output.exists()
			? output
			: null;
		
	}

}

