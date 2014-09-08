/**
 * Copyright (C) 2014 Tampere University of Technology 
 */

package com.wantedbug.cuesense;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import android.util.Log;

/**
 * Utility class to perform compression and decompression of data
 * @author vikasprabhu
 * Uses java.util.zip.Deflater's DEFLATE algorithm
 */
public class CompressionUtils {
	// Debugging
	private static final String TAG = "CompressionUtils";

	public static byte[] compress(byte[] data) throws IOException {
		Deflater deflater = new Deflater();
		deflater.setInput(data);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);

		deflater.finish();
		byte[] buffer = new byte[1024];
		while (!deflater.finished()) {
			int count = deflater.deflate(buffer);
			outputStream.write(buffer, 0, count);
		}
		outputStream.close();
		byte[] output = outputStream.toByteArray();

		Log.d(TAG, "Original: " + data.length / 1024 + " Kb");
		Log.d(TAG, "Compressed: " + output.length / 1024 + " Kb");
		return output;
	}

	public static byte[] decompress(byte[] data) throws IOException, DataFormatException {
		Inflater inflater = new Inflater();
		inflater.setInput(data);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
		byte[] buffer = new byte[1024];
		while (!inflater.finished()) {
			int count = inflater.inflate(buffer);
			outputStream.write(buffer, 0, count);
		}
		outputStream.close();
		byte[] output = outputStream.toByteArray();

		Log.d(TAG, "Original: " + data.length);
		Log.d(TAG, "Compressed: " + output.length);
		return output;
	}
}