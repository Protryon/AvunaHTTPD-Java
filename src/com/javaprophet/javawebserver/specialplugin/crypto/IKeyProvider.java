package com.javaprophet.javawebserver.specialplugin.crypto;

/**
 * @author lucamasira
 *
 * Interface for getting keys used for the decryption of plugins.<br>
 * I should probably improve on this.
 */
public interface IKeyProvider {
	
	/**
	 * Get the plugin decrypt key(private key) by the unique id.
	 * @param uid the unique id of the plugin
	 * @return the private key used for decryption.
	 */
	public byte[][] getKeys(String uid);

}
