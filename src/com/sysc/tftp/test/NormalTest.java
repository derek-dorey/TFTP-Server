package com.sysc.tftp.test;

import org.junit.Assert;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sysc.tftp.client.*;
import com.sysc.tftp.server.*;
import com.sysc.tftp.utils.*;

import junit.framework.TestCase;

public class NormalTest extends TestCase {
	
	private Server testServer;
	private Client testClient;
	
	private String clientDirectory;
	private Path clientPath;
	private byte[] clientBytes;
	
	private String serverDirectory;
	private Path serverPath;
	private byte[] serverBytes;

	@Before
	public void setUp() throws Exception {
		
		clientDirectory = Variables.CLIENT_FILES_DIR;
		serverDirectory = Variables.SERVER_FILES_DIR;
		
		clientBytes = null;
		serverBytes = null;
		
		testServer = new Server();
		testServer.start();
		
		try {
			Variables.serverIP = InetAddress.getLocalHost();
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
			return;
		}
		
		testClient = new Client();
	}
	
	@After
	public void tearDown() {
		
		deleteFile("1-block.txt");
		deleteFile("12-block.txt");
		deleteFile("16-block.txt");
		deleteFile("2-block.txt");
		deleteFile("4-block.txt");
		deleteFile("8-block.txt");
		deleteFile("long.txt");
		deleteFile("test.txt");
		deleteFile("text.txt");
		testServer.closeThreads();
	}
	
	@Test
	public void testWriteAllFiles() {
		
		assertTrue(testWrite("1-block.txt"));
		assertTrue(testWrite("12-block.txt"));
		assertTrue(testWrite("16-block.txt"));
		assertTrue(testWrite("2-block.txt"));
		assertTrue(testWrite("4-block.txt"));
		assertTrue(testWrite("8-block.txt"));
		assertTrue(testWrite("long.txt"));
		assertTrue(testWrite("test.txt"));
		assertTrue(testWrite("text.txt"));
	}
	
	
	public boolean testWrite(String filename) {
		
		clientPath = Paths.get(clientDirectory + filename);
		
		System.out.println("Writing: " + filename);
		testClient.sendFile(filename);
		serverPath = Paths.get(serverDirectory + filename);
		
		try {
			clientBytes = Files.readAllBytes(clientPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			serverBytes = Files.readAllBytes(serverPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(!(clientBytes == null && serverBytes == null)) {
			return Arrays.equals(clientBytes, serverBytes);
		} else {
			return false;
		}
	}
	
	public void deleteFile(String filename) {
		try {
			Files.delete(Paths.get(serverDirectory + filename));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
