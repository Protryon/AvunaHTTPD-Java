package com.javaprophet.javawebserver.specialutil;

import java.io.*;

/**
 * Class with util io methods. Nothing is taken from the Apache Commons IO. This is all created from scratch.<br>
 * All exceptions are caught within the method.
 * @author Luca
 *
 */
public class IOUtil {
	
	/**
	 * End-Of-File/End-Of-Stream. -1 is the most used indicater for end of stream.
	 */
	public static final int EOF = -1;
	
	/**
	 * Path separator char.
	 */
	public static final char PATH_SEPARATOR_CHAR = File.pathSeparatorChar;
	
	private IOUtil() {
		throw new RuntimeException("no b8 m8\nYou aren't allowed to call the constructor!");
	}
	
	/**
	 * Copy input to output
	 * @param is inputstream
	 * @param os outputstream
	 */
	public static void copy(InputStream is, OutputStream os) {
		try {
			int read = is.read();
			while(read != EOF) {
				os.write(read);
				read = is.read();
			}
		}catch(IOException ex) {
			ex.printStackTrace();
		}
	}
	
	/**
	 * Copy a file
	 * @param source file to copy
	 * @param dest destination to copy to
	 */
	public static void copy(File source, File dest) {
		try {
			FileInputStream fis = new FileInputStream(source);
			FileOutputStream fos = new FileOutputStream(dest);
			copy(fis, fos);
			fis.close();
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Move a file to a different destination
	 * @param source the file to move
	 * @param dest destination to move the file to
	 */
	public static void move(File source, File dest) {
		copy(source, dest);
		source.delete();
	}
	
	/**
	 * Read all bytes from an inputstream and return the byte array.
	 * @param in the inputstream to read from
	 * @return byte array containing all bytes from the inputstream.
	 */
	public static byte[] toByteArray(InputStream in) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		copy(in, baos);
		byte[] bytes = baos.toByteArray();
		try {
			baos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bytes;
	}
	
	/**
	 * Copy reader to writer.<br>
	 * I know readers and writers all extend to InputStream/OutputStream but this is usefull for noobs.
	 * @param reader to read from 
	 * @param writer to write to
	 */
	public static void copy(Reader reader, Writer writer) {
		copy(reader, writer);
	}
	
	/**
	 * Copy reader to an outputstream.<br>
	 * @param reader to read from 
	 * @param dest to write to.
	 */
	public static void copy(Reader reader, OutputStream dest) {
		copy(reader, dest);
	}
	
	/**
	 * Usefull for noobs
	 * @param source to read from
	 * @param dest to write to.
	 */
	public static void copy(InputStream source, Writer dest) {
		copy(source, dest);
	}
	
	public static byte[] readFromCurrentJar(String filename) {
		if(!StringUtil.isEmpty(filename)) {
			if(!filename.startsWith("/")) {
				filename = "/" + filename;
			}
			
			InputStream is = IOUtil.class.getResourceAsStream(filename);
			byte[] bytes = toByteArray(is);
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return bytes;
		}
		
		return null;
	}
	
	/**
	 * Add two bytes together to get an int. USES BIG ENDIAN
	 * @param b1 the first byte
	 * @param b2 the second byte
	 * @return the int created from the bytes
	 */
	public static int getIntBigEndian(byte b1, byte b2) {
		return b1 << 8 | b2;
	}

}
