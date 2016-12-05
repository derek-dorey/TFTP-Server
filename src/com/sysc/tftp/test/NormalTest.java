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
		
		//save reference to client and server directory
		clientDirectory = Variables.CLIENT_FILES_DIR;
		serverDirectory = Variables.SERVER_FILES_DIR;
		
		//place holder for bytes to be compared at end of tests
		clientBytes = null;
		serverBytes = null;
		
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
	}
	
	@Test
	public void testWriteAllFiles() {
		
		//Check writting all files to server
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
	
	@Test
	public void testReadAllFiles() {
		
		
		//First put all the files on the server
		testWrite("1-block.txt");
		System.out.println(testClient.getLogger().getClientSendLog().toString());
		System.out.println(testServer.getLogger().getServerReceiveLog().toString());
		System.out.println(testServer.getLogger().getServerSendLog().toString());
		System.out.println(testClient.getLogger().getClientReceiveLog().toString());
		
		testWrite("12-block.txt");
		testWrite("16-block.txt");
		testWrite("2-block.txt");
		testWrite("4-block.txt");
		testWrite("8-block.txt");
		testWrite("long.txt");
		testWrite("test.txt");
		testWrite("text.txt");
		
		
		//Delete all files from Client
		try {
			deleteAllClientFiles();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//test reading all the files from server to client
		assertTrue(testRead("1-block.txt"));
		assertTrue(testRead("12-block.txt"));
		assertTrue(testRead("16-block.txt"));
		assertTrue(testRead("2-block.txt"));
		assertTrue(testRead("4-block.txt"));
		assertTrue(testRead("8-block.txt"));
		assertTrue(testRead("long.txt"));
		assertTrue(testRead("test.txt"));
		assertTrue(testRead("text.txt"));
	}
	
	
	public boolean testWrite(String filename) {
		
		//save reference to client path
		clientPath = Paths.get(clientDirectory + filename);
		
		//print to console
		System.out.println("Writing: " + filename);
		
		//send file to from client to server
		testClient.sendFile(filename);
		
		//save reference to server path
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
	
public boolean testRead(String filename) {
		
			//save reference to client path
			clientPath = Paths.get(clientDirectory + filename);
			
			//print to console
			System.out.println("Reading: " + filename);
			
			//print to console
			System.out.println("Writing: " + filename);
			
			//read file to from server to client
			testClient.receiveFile(filename);
			
			//save reference to server path
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
			Files.delete(Paths.get(serverDirectory + filename));
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
	
	public void deleteClientFile(String filename) {
		try {
			// delete file from client
			Files.delete(Paths.get(clientDirectory + filename));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void deleteAllClientFiles() throws IOException {
		
		//delete all the files from client
		deleteClientFile("1-block.txt");
		deleteClientFile("12-block.txt");
		deleteClientFile("16-block.txt");
		deleteClientFile("2-block.txt");
		deleteClientFile("4-block.txt");
		deleteClientFile("8-block.txt");
		deleteClientFile("long.txt");
		deleteClientFile("test.txt");
		deleteClientFile("text.txt");
	}
}
