// Copyright (c) 2000  Dustin Sallings <dustin@spy.net>

package net.spy.photo.util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import net.spy.util.CloseUtil;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * Restore image backups.
 */
public class PhotoRestore extends Object {

	/**
	 * Create a BackupEntry from an input stream.
	 */
	public BackupEntry restore(InputStream input) throws Exception {
		// Our result
		BackupEntry ret=null;

		// Input source
		InputSource is=new InputSource(input);

		// Get the parser
		DOMParser dp=new DOMParser();
		dp.setFeature("http://xml.org/sax/features/validation", true);

		// Parse the stream
		dp.parse(is);

		// Get the document.
		Document d=dp.getDocument();
		
		// Figure out what we're parsing here.
		Node n=d.getFirstChild();
		if(n==null) {
			throw new Exception("No child in document!");
		}
		String type=n.getNodeName();
		if(type.equals("photo_album_object")) {
			ret=new AlbumBackupEntry(n);
		}

		if(ret!=null) {
			ret.restore();
		}

		return(ret);
	}

	/**
	 * Restore an image or a series of images.
	 */
	public static void main(String args[]) throws Exception {
		PhotoRestore pr=new PhotoRestore();

		for(String fn : args) {
			// Get the stream

			FileInputStream fis=null;
			GZIPInputStream gis=null;
			try {
				fis=new FileInputStream(fn);
				gis=new GZIPInputStream(fis);

				BackupEntry be = pr.restore(gis);
				System.out.println("Restored " + be);
			} finally {
				CloseUtil.close(gis);
				CloseUtil.close(fis);
			}
		}
	}
}
