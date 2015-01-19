package com.javaprophet.javawebserver.specialplugin.crypto;

import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author lucamasira
 * This class can handle encryption and decryption of class files.<br>
 * Can also be used for other purposes.<br>
 * Default crypto method:  AES-128<br>
 * Default cipher padding: AES/CBC/PKCS5Padding
 * <br>
 * This method isn't safe at all because if you have little knowlegde of ClassLoaders you will be able to retrieve the original bytecode.<br>
 * I just though that will be a cool idea to implement.
 */
public class CryptoManager implements ICryptoManager{
	
	/**
	 * Method of encryption used.
	 */
	public static final String CRYPTO_METHOD = "AES";
	
	/**
	 * Padding used by the cipher.
	 */
	public static final String CIPHER_PADDING = "AES/CBC/PKCS5Padding";
	
	/**
	 * The cipher used for crypto.
	 */
	private Cipher cipher;
	
	/**
	 * Public key, used for encryption.
	 */
	private IvParameterSpec publicKey;
	
	/**
	 * The private key, used for decryption and encryption.
	 */
	private SecretKeySpec privateKey;
	
	/**
	 * Constructor that does nothing.
	 */
	public CryptoManager() {
	}
	
	/**
	 * Constructor which sets the keys used to crypto.
	 * @param publicKey key used for encryption
	 * @param privateKey key used for decryption
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 */
	public CryptoManager(final IvParameterSpec publicKey, final SecretKeySpec privateKey) throws NoSuchAlgorithmException, NoSuchPaddingException {
		setKeys(publicKey, privateKey);
	}
	
	/**
	 * Constructor to set the public and private key with the raw bytes.<br>
	 * This method doesn't check if the key length is correct. It should be 16 bytes.<br>
	 * Java doesn't allow encryption higher than 128bit because some governments wont allow it.
	 * @param publicKey the public key used for encryption
	 * @param privateKey the private key used for decryption
	 * @throws NoSuchPaddingException exception that can occur when the padding isn't correct/invalid
	 * @throws NoSuchAlgorithmException when a algorith name isn't correct/valid
	 */
	public CryptoManager(final byte[] publicKey, final byte[] privateKey) throws NoSuchAlgorithmException, NoSuchPaddingException {
		setKeys(publicKey, privateKey);
	}
	
	/**
	 * Encrypt a byte array.
	 * @param bytes the bytes to encrypt
	 * @return the encrypted byte array
	 * @throws Exception gets thrown when the cipher couldn't encrypt correctly
	 */
	public byte[] encrypt(final byte[] bytes) throws Exception{
		getCipher().init(Cipher.ENCRYPT_MODE, getPrivateKey(), getPublicKey());
		return getCipher().doFinal(bytes);
	}
	
	/**
	 * Decrypt a byte array.
	 * @param bytes the bytes to decrypt
	 * @return the decrypted byte array
	 * @throws Exception gets thrown when couldn't decrypt correctly
	 */
	public byte[] decrypt(final byte[] bytes) throws Exception {
		getCipher().init(Cipher.DECRYPT_MODE, getPrivateKey(), getPublicKey());
		return getCipher().doFinal(bytes);
	}
	
	/**
	 * Get the public key.
	 * @return the public key
	 */
	public IvParameterSpec getPublicKey() {
		return publicKey;
	}
	
	/**
	 * Get the public key in bytes.
	 * @return the byte array which is the public key
	 */
	public byte[] getPublicKeyBytes() {
		return getPublicKey().getIV();
	}
	
	/**
	 * Get the private key used for decryption.
	 * @return the private key
	 */
	public SecretKeySpec getPrivateKey() {
		return privateKey;
	}
	
	/**
	 * Get the private key in a byte array.
	 * @return the private key in bytes
	 */
	public byte[] getPrivateKeyBytes() {
		return getPrivateKey().getEncoded();
	}
	
	/**
	 * Get the cipher used for crypto.
	 * @return the cipher
	 */
	public Cipher getCipher() {
		return cipher;
	}
	
	/**
	 * Set the keys used for crypto.
	 * @param publicKey the public key used for encryption
	 * @param privateKey the private key used for decryption
	 * @throws NoSuchPaddingException exception that can occur when the padding isn't correct/invalid
	 * @throws NoSuchAlgorithmException when a algorith name isn't correct/valid
	 */
	public void setKeys(final IvParameterSpec publicKey, final SecretKeySpec privateKey) throws NoSuchAlgorithmException, NoSuchPaddingException {
		this.publicKey = publicKey;
		this.privateKey = privateKey;
		
		//will call this here since this method will always be called.
		cipher = Cipher.getInstance(CIPHER_PADDING);
	}
	
	/**
	 * Set the keys used for crypto.
	 * @param publicKey the key used for encryption
	 * @param privateKey the key used for decryption
	 */
	public void setKeys(byte[]... keys) {
		//try since we're implementing this method
		try {
			setKeys(new IvParameterSpec(keys[0]), new SecretKeySpec(keys[1], CRYPTO_METHOD));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		}
	}

}
