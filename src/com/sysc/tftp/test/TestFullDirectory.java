package com.sysc.tftp.test;

import org.junit.Assert;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sysc.tftp.client.*;
import com.sysc.tftp.server.*;
import com.sysc.tftp.utils.*;

import junit.framework.TestCase;

public class TestFullDirectory extends TestCase {
	
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
		Variables.CLIENT_FILES_DIR = System.getProperty("user.dir") + "/clientFiles/";
		Variables.SERVER_FILES_DIR = System.getProperty("user.dir") + "/serverFiles/";
		
		clientDirectory = Variables.CLIENT_FILES_DIR;
		serverDirectory = Variables.SERVER_FILES_DIR;
	}
	
	@After
	public void tearDown() {
	
		testServer.getReceiveSocket().close();
		
	}
	
	@Test
	public void testFullServer() {
		
		Variables.SERVER_FILES_DIR = "E:\\";
		
		serverDirectory = "E:\\";
		
		testServer = new Server();
		testServer.start();
		//get/set the server ip
		try {
			Variables.serverIP = InetAddress.getLocalHost();
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
			return;
		}
		
		testClient = new Client();
		
		//Check writing all files to server
		assertFalse(testWrite("1-block.txt"));
		assertEquals(testClient.getLogger().getClientReceiveLog().get(1), "ERROR3");
		
		testClient.getLogger().clearAll();
		assertFalse(testWrite("12-block.txt"));
		assertEquals(testClient.getLogger().getClientReceiveLog().get(1), "ERROR3");
		
		testClient.getLogger().clearAll();
		assertFalse(testWrite("16-block.txt"));
		assertEquals(testClient.getLogger().getClientReceiveLog().get(1), "ERROR3");
		
		testClient.getLogger().clearAll();
		assertFalse(testWrite("2-block.txt"));
		assertEquals(testClient.getLogger().getClientReceiveLog().get(1), "ERROR3");
		
		testClient.getLogger().clearAll();
		assertFalse(testWrite("4-block.txt"));
		assertEquals(testClient.getLogger().getClientReceiveLog().get(1), "ERROR3");
		
		testClient.getLogger().clearAll();
		assertFalse(testWrite("8-block.txt"));
		assertEquals(testClient.getLogger().getClientReceiveLog().get(1), "ERROR3");
		
		testClient.getLogger().clearAll();
		assertFalse(testWrite("long.txt"));
		assertEquals(testClient.getLogger().getClientReceiveLog().get(1), "ERROR3");
		
		testClient.getLogger().clearAll();
		assertFalse(testWrite("test.txt"));
		assertEquals(testClient.getLogger().getClientReceiveLog().get(1), "ERROR3");
		
		testClient.getLogger().clearAll();
		assertFalse(testWrite("text.txt"));
		assertEquals(testClient.getLogger().getClientReceiveLog().get(1), "ERROR3");
	}
	
	@Test
	public void testFullClient() {
		
		Variables.CLIENT_FILES_DIR = "E:\\";
		clientDirectory = "E:\\";
		
		testServer = new Server();
		testServer.start();
		//get/set the server ip
		try {
			Variables.serverIP = InetAddress.getLocalHost();
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
			return;
		}
		
		testClient = new Client();
		
		assertFalse(testRead("long2.txt"));  //client does not respond with error packet, do no check (?)
		
		assertFalse(testRead("test2.txt"));
	
		assertFalse(testRead("text2.txt"));
		
	}
	
	public boolean testWrite(String filename) {
		
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
			return false;
		}
		
		//get bytes from server file
		try {
			serverBytes = Files.readAllBytes(serverPath);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		//compare client/server files
		if(!(clientBytes == null && serverBytes == null)) {
			return Arrays.equals(clientBytes, serverBytes);
		} else {
			return false;
		}
	}
	
	public boolean testRead(String filename) {
		
		clientBytes = null;
		serverBytes = null;
		//client path
		clientPath = Paths.get(clientDirectory + filename);
		
		//print to console
		System.out.println("Reading: " + filename);
		
		//read file from server
		testClient.receiveFile(filename);
		
		//server path
		serverPath = Paths.get(serverDirectory + filename);
		
		//get bytes from client file
		try {
			clientBytes = Files.readAllBytes(clientPath);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} 
		
		//get bytes from server file
		try {
			serverBytes = Files.readAllBytes(serverPath);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		//compare client/server files
		if(!(clientBytes == null && serverBytes == null)) {
			return Arrays.equals(clientBytes, serverBytes);
		} else {
			return false;
		}
	}
}