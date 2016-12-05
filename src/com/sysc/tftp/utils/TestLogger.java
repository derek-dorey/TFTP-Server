package com.sysc.tftp.utils;

import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.Arrays;

import com.sysc.tftp.client.Client;
import com.sysc.tftp.server.Server;

public class TestLogger {
	
	private ArrayList<String> clientSendLog;
	private ArrayList<String> clientReceiveLog;
	private ArrayList<String> serverSendLog;
	private ArrayList<String> serverReceiveLog;
	private boolean isClientLog;
	
	
	public TestLogger(Client user) {
		clientSendLog = new ArrayList<String>();
		clientReceiveLog = new ArrayList<String>();
		isClientLog = true;
	}
	
	public TestLogger(Server server) {
		
		serverSendLog = new ArrayList<String>();
		serverReceiveLog = new ArrayList<String>();
		isClientLog = false;
	}
	
	public void archive(boolean sending, DatagramPacket packet) {
		
		byte[] data = new byte[Variables.MAX_PACKET_SIZE];
		byte[] blockNumber = new byte[2];
		
		data = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());
		blockNumber[0] = data[2];
		blockNumber[1] = data[3];
		
		String type;
		
		if(data[0]!=0) {
			type = "INVALID";
		}
		
		if(data[1]==1) {
			type = "RRQ";
		} else if(data[1]==2) {
			type = "WRQ";
		} else if(data[1]==3) {
			type = "DATA";
		} else if(data[1]==4) {
			type = "ACK";
		} else if(data[1]==5) {
			type = "ERROR";			//note: if error packet, blockNumber stores the error code
		} else {
			type = "INVALID"; 		
		}
		
		if(isClientLog && sending) { //if the caller is a Client sending a data packet
			
			clientSendLog.add(type + String.valueOf(BlockUtil.byteToInt(blockNumber)));
			
		} else if (isClientLog) {  //caller is a Client receiving a data packet
			
			clientReceiveLog.add(type + String.valueOf(BlockUtil.byteToInt(blockNumber)));
			
		} else if(!(isClientLog) && sending) { //caller is a Server thread sending a data packet
			
			serverSendLog.add(type + String.valueOf(BlockUtil.byteToInt(blockNumber)));
			
		} else { //else the caller is a Server Thread receiving a data packet
		
			serverReceiveLog.add(type + String.valueOf(BlockUtil.byteToInt(blockNumber)));
		}
	}
	
	public ArrayList<String> getClientSendLog() {
		return clientSendLog;
	}

	public ArrayList<String> getClientReceiveLog() {
		return clientReceiveLog;
	}

	public ArrayList<String> getServerSendLog() {
		return serverSendLog;
	}

	public ArrayList<String> getServerReceiveLog() {
		return serverReceiveLog;
	}
	
	public int count(ArrayList<String> a, String s) {
		int returnCount = 0;
		
		for(int i=0; i<a.size(); i++) {
			if(a.get(i).equals(s)) {
				returnCount++;
			}
		}
		return returnCount;
	}
}
