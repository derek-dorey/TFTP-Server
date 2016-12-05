package com.sysc.tftp.test;

import org.junit.Assert;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sysc.tftp.client.*;
import com.sysc.tftp.error.ErrorSimulator;
import com.sysc.tftp.server.*;
import com.sysc.tftp.utils.*;
import com.sysc.tftp.utils.Variables.Mode;

import junit.framework.TestCase;

public class LostTest extends TestCase {
	
	private Server testServer;
	private Client testClient;
	private ErrorSimulator testSim;
	
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
		
		//create/start errorSim
		testSim = new ErrorSimulator();
		testSim.start();
		
		//get/set the server ip
		try {
			Variables.serverIP = InetAddress.getLocalHost();
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
			return;
		}
				
		//start a client instance
		testClient = new Client();
		
		//put client into test mode
		Variables.CLIENT_MODE = Variables.Mode.TEST;
		
		//clear logs client and server logs
		testServer.getLogger().clearAll();
		testClient.getLogger().clearAll();
		
	
				
	}
	
	@After
	public void tearDown() {
		
		//Delete the files on the server
		try {
			deleteAllServerFiles();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//clear logs client and server logs
		testServer.getLogger().clearAll();
		testClient.getLogger().clearAll();
		
		//Close all server and ErrorSim threads
		testServer.closeThreads();
		
		//close errorSim's recieve socket
		testSim.getReceiveSocket().close();
		
	}
	
	@Test
	public void testWrqAck() {
		
		
		//add dlost thread to error sim
		testSim.handle("lose ack 1");
		
		//read file from server
		assertTrue(testWrite("4-block.txt"));
		
		//test that server received two ACK1 packets
		assertEquals(testClient.getLogger().count(testClient.getLogger().getClientReceiveLog(), "ACK1"),1);
		
		//test that server only sent one DATA2 packets
		assertEquals(testServer.getLogger().count(testServer.getLogger().getServerSendLog(), "ACK1"),2);
			
	}
	
	@Test
	public void testWrqData() {
		
		
		//add dlost thread to error sim
		testSim.handle("lose data 1");
		
		//read file from server
		assertTrue(testWrite("4-block.txt"));
		
		//System.out.println(testClient.getLogger().getClientSendLog().toString());
		//test that server received two ACK1 packets
		assertEquals(testClient.getLogger().count(testClient.getLogger().getClientSendLog(), "DATA1"),2);
		
		//test that server only sent one DATA2 packets
		assertEquals(testServer.getLogger().count(testServer.getLogger().getServerReceiveLog(), "DATA1"),1);
			
	}
	
	@Test
	public void testRrqAck() {
		
		//put  file on the server
		testClient.sendFile("4-block.txt");
			
		//delete the file from client so you can read it from server
		deleteClientFile("4-block.txt");
		
		//clear the logs
		testServer.getLogger().clearAll();
		testClient.getLogger().clearAll();
		
		//add dup thread to error sim
		testSim.handle("lose ack 1");
		
		//read file from server
		assertTrue(testRead("4-block.txt"));
		
		//test that server received two ACK1 packets
		assertEquals(testClient.getLogger().count(testClient.getLogger().getClientSendLog(), "ACK1"),2);
		
		//test that server only sent one DATA2 packets
		assertEquals(testServer.getLogger().count(testServer.getLogger().getServerReceiveLog(), "ACK1"),1);		
	
	}
	
	@Test
	public void testRrqData() {
		
		//put  file on the server
		testClient.sendFile("4-block.txt");
			
		//delete the file from client so you can read it from server
		deleteClientFile("4-block.txt");
		
		//clear the logs
		testServer.getLogger().clearAll();
		testClient.getLogger().clearAll();
		
		//add dup thread to error sim
		testSim.handle("lose data 2");
		
		//read file from server
		assertTrue(testRead("4-block.txt"));
		
		//System.out.println(testServer.getLogger().getServerReceiveLog());
		//System.out.println(testServer.getLogger().getServerSendLog());
		
		//test that server only sent two DATA2 packets
		assertEquals(testServer.getLogger().count(testServer.getLogger().getServerReceiveLog(), "ACK1"),2);
		
		
		//test that server only sent one DATA2 packets
		assertEquals(testServer.getLogger().count(testServer.getLogger().getServerReceiveLog(), "DATA2"),1);		
	
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
			System.out.println("reading: " + filename);
			
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
	
	public void deleteClientFile(String filename) {
		try {
			// delete file from client
			Files.deleteIfExists(Paths.get(clientDirectory + filename));
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
