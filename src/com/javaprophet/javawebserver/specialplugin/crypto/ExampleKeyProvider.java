package com.javaprophet.javawebserver.specialplugin.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.javaprophet.javawebserver.specialutil.Configuration;


/**
 * @author lucamasira
 * 
 * THIS SHOULD NOT BE USED! WRITE YOUR OWN OR USE ANOTHER ONE. THIS ONE IS SIMPLY THERE FOR TESTING PURPOSES!
 *
 * This implementation of the IKeyProvider uses Events to get the private key.<br>
 * It is recommended to create your own KeyProvider, this one is for testing.
 * <br>
 * In this library the private key is the key which is given to everybody.<br>
 * <br>
 * The public key should not be given because it's used for the encryption.<br>
 * If you think that this will fix all your code stealing, it won't.<br>
 * Just a view methods and you have all the bytecode of the encrypted classes.
 */
public class ExampleKeyProvider implements IKeyProvider {
	
	/**
	 * File containing all the keys.
	 */
	private final File KEYFILE = new File("keys");
	
	/**
	 * Configuration containing all keys.
	 */
	private final Configuration KEYS = new Configuration();
	
	/**
	 * Constructor
	 */
	public ExampleKeyProvider() {
		try {
			if(!KEYFILE.exists())
				KEYFILE.createNewFile();
			
			KEYS.load(new FileInputStream(KEYFILE));
		
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}

	/**
	 * Get the keys used for decryption
	 */
	@Override
	public byte[][] getKeys(String uid) {
		if(!KEYS.containsKey(uid))
			return new byte[][]{};
			
		byte[][] cryptoKeys = new byte[2][16];
		String[] keyList = KEYS.getList(uid);
		cryptoKeys[0] = keyList[0].getBytes();
		cryptoKeys[1] = keyList[1].getBytes();
		return cryptoKeys;
	}

}
