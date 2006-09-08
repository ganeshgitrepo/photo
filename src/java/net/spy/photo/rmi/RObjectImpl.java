// Copyright (c) 1999 Dustin Sallings <dustin@spy.net>
// arch-tag: 9D2F5B50-5D6D-11D9-B80E-000A957659CC

package net.spy.photo.rmi;

import java.io.Serializable;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import net.spy.cache.DiskCache;

/**
 * Implementation for RObjectServer
 */
public class RObjectImpl extends UnicastRemoteObject implements RObject {

	private static final String DEFAULT_DIR="/tmp/rcache";

	private DiskCache diskCache=null;

	/**
	 * Get an RObjectImpl with the default directory.
	 */
	public RObjectImpl() throws RemoteException {
		this(DEFAULT_DIR);
	}

	/**
	 * Get an RObjectImpl with the given directory.
	 */
	public RObjectImpl(String basedir) throws RemoteException {
		super();
		diskCache=new DiskCache(basedir);
	}

	/**
	 * @see RObject
	 */
    public void storeObject(String name, Serializable o)
		throws RemoteException {
		diskCache.put(name, o);
	}

	/**
	 * @see RObject
	 */
    public Object getObject(String name) throws RemoteException {
		return(diskCache.get(name));
	}

	/**
	 * @see RObject
	 */
	public boolean ping() throws RemoteException {
		return(true);
	}

	/**
	 * main can be invoked to run an RObjectServer.  It takes as an
	 * argument, the path where it will be storing its objects.
	 *
	 * @param args Uh, yeah, the arguments
	 *
	 * @throws Exception if anything blows up
	 */
	public static void main(String args[]) throws Exception {
		if(args.length < 1) {
			System.err.println("RCache path not given.");
			throw new Exception("RCache path not given.");
		}
		RObjectImpl obj1 = new RObjectImpl(args[0]);
		Naming.rebind("RObjectServer", obj1);
		System.err.println("RObjectServer bound in registry");
	}
}
