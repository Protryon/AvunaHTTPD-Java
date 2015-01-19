package com.javaprophet.javawebserver.specialplugin.crypto;


/**
 * @author Luca
 * 
 * Interface for classes which handle encryption and decryption.
 */
public interface ICryptoManager {
	
	/**
	 * Encrypt a byte array
	 * @param bytes bytes to be encrypted
	 * @return the encrypted bytes
	 * @throws Exception and exception that can occur when the decryption fails.
	 * 		This can either be a bad padding, bad crypto method and or a wrong set of keys.
	 */
	public byte[] encrypt(byte[] bytes) throws Exception;
	
	/**
	 * Decrypt a byte array.
	 * @param bytes the bytes to be decrypted
	 * @return the decrypted bytes
	 * @throws Exception and exception that can occur when the decryption fails.
	 * 		This can either be a bad padding, bad crypto method and or a wrong set of keys.
	 */
	public byte[] decrypt(byte[] bytes) throws Exception;

	/**
	 * Set the keys
	 * @param keys key array since it may be different in any kind of crypto method.
	 */
	public void setKeys(byte[]... keys);
}
