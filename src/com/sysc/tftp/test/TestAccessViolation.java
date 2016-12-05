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

public class TestAccessViolation extends TestCase {
	
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
		
		//save reference to client and server directory
		clientDirectory = Variables.CLIENT_FILES_DIR;
		Variables.SERVER_FILES_DIR = "C:\\software";
		
		//create/start new server instance
		testServer = new Server();
		testServer.start();
		
		//get/set the server ip
		try {
			Variables.serverIP = InetAddress.getLocalHost();
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
			return;
		}
		
		//start a client instance
		testClient = new Client();
	}
	
	@After
	public void tearDown() {
		
		//Delete all the files on the server
		try {
			deleteAllServerFiles();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//Close all server threads
		testServer.closeThreads();
		
		//restore default server directory
		Variables.SERVER_FILES_DIR = System.getProperty("user.dir") + "/serverFiles/";
	}
	
	@Test
	public void testWritePermission() {
		
		//Check writing all files to server
		assertFalse(testWrite("1-block.txt"));
		assertEquals(testClient.getLogger().getClientReceiveLog().get(1), "ERROR2");
		
		testClient.getLogger().clearAll();
		assertFalse(testWrite("12-block.txt"));
		assertEquals(testClient.getLogger().getClientReceiveLog().get(1), "ERROR2");
		
		testClient.getLogger().clearAll();
		assertFalse(testWrite("12-block.txt"));
		assertEquals(testClient.getLogger().getClientReceiveLog().get(1), "ERROR2");
		
		testClient.getLogger().clearAll();
		assertFalse(testWrite("16-block.txt"));
		assertEquals(testClient.getLogger().getClientReceiveLog().get(1), "ERROR2");
		
		testClient.getLogger().clearAll();
		assertFalse(testWrite("2-block.txt"));
		assertEquals(testClient.getLogger().getClientReceiveLog().get(1), "ERROR2");
		
		testClient.getLogger().clearAll();
		assertFalse(testWrite("4-block.txt"));
		assertEquals(testClient.getLogger().getClientReceiveLog().get(1), "ERROR2");
		
		testClient.getLogger().clearAll();
		assertFalse(testWrite("8-block.txt"));
		assertEquals(testClient.getLogger().getClientReceiveLog().get(1), "ERROR2");
		
		testClient.getLogger().clearAll();
		assertFalse(testWrite("long.txt"));
		assertEquals(testClient.getLogger().getClientReceiveLog().get(1), "ERROR2");
		
		testClient.getLogger().clearAll();
		assertFalse(testWrite("test.txt"));
		assertEquals(testClient.getLogger().getClientReceiveLog().get(1), "ERROR2");
		
		testClient.getLogger().clearAll();
		assertFalse(testWrite("text.txt"));
		assertEquals(testClient.getLogger().getClientReceiveLog().get(1), "ERROR2");
	}
	
	public boolean testWrite(String filename) {
		
		//place holder for bytes to be compared at end of tests
		clientBytes = null;
		serverBytes = null;
		
		//client path
		clientPath = Paths.get(clientDirectory + filename);
		
		//print to console
		System.out.println("Writing: " + filename);
		
		//send file to from client to server
		testClient.sendFile(filename);
		
		//server path
		serverPath = Paths.get(serverDirectory + filename);
		
		//get bytes from client file
		try {
			clientBytes = Files.readAllBytes(clientPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//get bytes from server file
		try {
			serverBytes = Files.readAllBytes(serverPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//compare client/server files
		if(!(clientBytes == null && serverBytes == null)) {
			return Arrays.equals(clientBytes, serverBytes);
		} else {
			return false;
		}
	}
	
	public void deleteServerFile(String filename) {
			
		try {
			// delete file from server
			Files.deleteIfExists(Paths.get(serverDirectory + filename));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void deleteAllServerFiles() throws IOException {
		
		//delete all the files from server
		deleteServerFile("1-block.txt");
		deleteServerFile("12-block.txt");
		deleteServerFile("16-block.txt");
		deleteServerFile("2-block.txt");
		deleteServerFile("4-block.txt");
		deleteServerFile("8-block.txt");
		deleteServerFile("long.txt");
		deleteServerFile("test.txt");
		deleteServerFile("text.txt");
	}
}