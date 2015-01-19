package com.javaprophet.javawebserver.specialutil;

/**
 * Custom string utils, looks like Apache Commons Lang but is different/lightweight.
 * @author Luca
 *
 */
public class StringUtil {
	
	/**
	 * Emptry string
	 */
	public static final String EMPTY = "";
	
	/**
	 * Private constructor.
	 */
	private StringUtil() {
		throw new IllegalStateException("Dun goofed m8, this is not allowed.");
	}
	
	/**
	 * Simple null check for a string.
	 * @param string the string to be checked
	 * @return true if the string is null.
	 */
	public static boolean isNull(String string) {
		return string == null;
	}
	
	/**
	 * Check if a string is empty.<br>
	 * Will also return true if the string is null
	 * @param string the string to check
	 * @return the check if the string is empty
	 */
	public static boolean isEmpty(String string) {
		if(!isNull(string)) {
			return string.equals(EMPTY);
		}
		
		return true;
	}
	
	/**
	 * Reverse the order of a string.
	 * @param string the string to be reversed.
	 * @return the reversed stirng.
	 */
	public static String reverse(String string) {
		if(!isNull(string)) {
			return new StringBuilder(string).reverse().toString();
		}
		return null;
	}
	
	/**
	 * Check if a string is a palindrome.<br>
	 * Will be false if string is null.<br>
	 * WARNING: THIS METHOD IS CASE SENSITIVE
	 * @param string the string to check
	 * @return true if the string is a palindrome.
	 */
	public static boolean isPalindrome(String string) {
		if(!isNull(string)) {
			return reverse(string).equals(string);
		}
		
		return false;
	}
	
	/**
	 * Uppercase the first letter in the word or sentence.
	 * @param word the word or sentence to uppercase
	 * @return the string with the first letter being uppercase.
	 */
	public static String ucWord(String word) {
		if(!isNull(word)) {
			if(isEmpty(word)) {
				return EMPTY;
			}
			String l1 = word.substring(0, 1).toUpperCase();
			String rest = word.substring(1, word.length());
			return l1 + rest;
			
		}
		
		return null;
	}
	
	/**
	 * Uppercase all first letters of the sentence.
	 * @param sentence
	 * @return
	 */
	public static String ucWords(String sentence, char spacer) {
		if(!isNull(sentence)) {
			if(isEmpty(sentence)) {
				return EMPTY;
			}
			if(spacer == '\0') {
				throw new IllegalStateException("spacer can not be null");
			}
			
			String[] splitted = sentence.split(EMPTY + spacer);//messy af, I know
			StringBuilder uced = new StringBuilder();
			for(int x = 0; x < splitted.length; x++) {
				uced.append(ucWord(splitted[x]));
				if(x <= splitted.length - 1) 
					uced.append(spacer);
			}
			
			return uced.toString();
			
		}
		
		return null;
	}
	
	/**
	 * Uppercase all first letters of the sentence splitted by a space.
	 * @param sentence
	 * @return
	 */
	public static String ucWords(String sentence) {
		return ucWords(sentence, ' ');
	}
	
	/**
	 * Remove a specific char from a string. This will replace all chars that match.
	 * @param string
	 * @param ch
	 * @return
	 */
	public static String removeChar(String string, char ch) {
		if(!isNull(string)) {
			if(isEmpty(string)) {
				return EMPTY;
			}
			
			if(ch == '\0') {
				throw new IllegalStateException("ch cant be null nub");
			}
			
			char[] chrz = new char[string.length()];
			int count = 0;
			for (int x = 0; x < chrz.length; x++) {
				if (string.charAt(x) != ch) {
					chrz[count++] = string.charAt(x);
				}
			}
			if (count == chrz.length) {
				return string;
			}
			return new String(chrz, 0, count);
			
		}
		
		return null;
	}
	
	/**
	 * Remove spaces from a string.
	 * @param str
	 * @return
	 */
	public static String removeWhiteSpace(String str) {
		return removeChar(str, ' ');
	}

}
