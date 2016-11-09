package com.sysc.tftp.utils;

public class Variables {

	public static final int MAX_PACKET_SIZE = 512; // Maximum packet size in
													// bytes

	public static enum Mode {
		NORMAL("NORMAL"), TEST("TEST");
		
		String type;
		
		Mode(String s) {
			type = s;
		}
		
		public String getType() {
			return type;
		}
		
	}; // Normal mode sends directly to the server, test mode sends to simulator

	public static enum Request {
		RRQ, WRQ, DATA, ACK, ERROR
	}; // All request types

	// responses for valid requests
	public static final byte[] readResp = { 0, 1 };
	public static final byte[] writeResp = { 0, 2 };

	public static final byte[] READ = { 0, 3, 0, 1 };
	public static final byte[] WRITE = { 0, 4, 0, 0 };
	public static final byte[] DATA = { 0, 3, 0, 1 };
	public static final byte[] ACK = { 0, 4, 0, 0 };

	public static final byte[] ERROR_1 = { 0, 5, 0, 1 }; // file not found
	public static final byte[] ERROR_2 = { 0, 5, 0, 2 }; // access violation
	public static final byte[] ERROR_3 = { 0, 5, 0, 3 }; // disk full
	public static final byte[] ERROR_6 = { 0, 5, 0, 6 }; // file already exists
	
	// port numbers
	public static int SERVER_PORT = 69;
	public static int ERROR_PORT = 23;

	public static int NORMAL_PORT = 69;
	public static int TEST_PORT = 23;

	// server files location
	//public static String SERVER_FILES_DIR = System.getProperty("user.dir") + "/serverFiles/";
	
	public static String SERVER_FILES_DIR = "C:\\software";    //directory without write permissions (on lab machines) for testing access violations
	
	// client files location
	public static String CLIENT_FILES_DIR = System.getProperty("user.dir") + "/files/";

	public static String TRANSFER_MODE = "octet";

	// Size of the header section of a data packet (op code + block #)
	public static final int DATA_PACKET_HEADER_SIZE = 4;
	// Size of the ack packet
	public static final int ACK_DATA_SIZE = 4;

	public static boolean VERBOSE = false;
	public static final String VERBOSE_FLAG = "--v";

	public static Mode CLIENT_MODE = Mode.NORMAL;
	public static final String TEST_MODE_FLAG = "-t";

	// settings commands
	public static final String SET_VERBOSE_ON = "set verbose on";
	public static final String SET_VERBOSE_OFF = "set verbose off";
	
	public static final String SET_MODE_TEST = "set mode test";
	public static final String SET_MODE_NORMAL = "set mode normal";
	
}
