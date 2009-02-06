package lento.net;

import java.net.*;
import java.util.*;
import java.io.*;

class PacketOutputStream extends ByteArrayOutputStream {

	PacketOutputStream(byte[] buf) {
		super(0);
		this.buf = buf;
	}
	byte[] getData() {
		return buf;
	}
};
