package com.sysc.tftp.error;

import java.net.InetAddress;

public abstract class ErrorThread implements Runnable {

	protected byte[] data = null; // holds the original request

	// client information: port, IP, length of data
	protected int len = 0, clientPort = 0;
	protected InetAddress clientIP = null;
	
	public void setInfo(byte[] data, int len, InetAddress ip, int port) {
		this.data = data;
		this.len = len;
		this.clientIP = ip;
		this.clientPort = port;
	}
	
	public boolean isRequest(int packet, byte[] data) {
		return data[1] == packet;
	}
	
	public boolean isPosition(int position, byte[] data) {
		int num = ((data[2] & 0xff) << 8) | (data[3] & 0xff);
		return position == num;
	}

}
