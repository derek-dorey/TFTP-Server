package com.sysc.tftp.utils;

public class BlockUtil {

	public static byte[] intToByte(int i) {
		
		byte[] b = new byte[2];
		b[0] = (byte) (i >> 8);
		b[1] = (byte) (i);
		
	    return b;
	}

	
	public static int byteToInt(byte[] bytes) {
		return ((bytes[0] << 8) & 0xFF00) | (bytes[1] & 0xFF);
	}

	
	public static void main(String[] args) {

		int test = 60000;
		System.out.println("Integer: " + test);
		byte[] b = intToByte(test);
		for (byte b2 : b) {
		    System.out.println(Integer.toBinaryString(b2 & 255 | 256).substring(1));
		}
		System.out.println("To Bytes: " + b);
		System.out.println("Back to integer " + byteToInt(b));
		
	}
}
