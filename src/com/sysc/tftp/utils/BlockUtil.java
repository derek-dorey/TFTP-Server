package com.sysc.tftp.utils;

import java.math.BigInteger;

public class BlockUtil {

	public static byte[] intToByte(int i) {
	    BigInteger bigInt = BigInteger.valueOf(i);      
	    byte[] b = bigInt.toByteArray();
	    
	    if (b.length == 1) {
	    	byte[] b2 = new byte[2];
	    	b2[0] = 0;
	    	b2[1] = b[0];
	    	return b2;
	    }
	    return b;
	}
	
	public static int byteToInt(byte[] bytes) {
		return new BigInteger(1, bytes).intValue();
	}

	@Deprecated
	public static int oldConversion(byte[] data) {
		return ((data[2] & 0xff) << 8) | (data[3] & 0xff);
	}
	
	@Deprecated
	public static int oldConversion2(byte[] received) {
		return ((received[2] << 8) & 0xFF00) | (received[3] & 0xFF);
	}
	
}
