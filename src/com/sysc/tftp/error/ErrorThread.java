package com.sysc.tftp.error;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.sysc.tftp.utils.BlockUtil;

public abstract class ErrorThread implements Runnable {

	protected byte[] data = null; // holds the original request

	// client information: port, IP, length of data
	protected int len = 0, clientPort = 0;
	protected InetAddress clientIP = null;
	protected InetAddress serverIP = null;
	
	public void setInfo(byte[] data, int len, InetAddress ip, int port) {
		this.data = data;
		this.len = len;
		this.clientIP = ip;
		this.clientPort = port;
		try {
			this.serverIP = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public boolean isRequest(int packet, byte[] data) {
		return data[1] == packet;
	}
	
	public boolean isPosition(int position, byte[] data) {
		byte[] block = new byte[2];
		block[0] = data[2];
		block[1] = data[3];
		int num = BlockUtil.byteToInt(block);
		return position == num;
	}

}
