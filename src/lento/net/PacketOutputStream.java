package lento.net;

import java.net.*;
import java.util.*;
import java.io.*;

class PacketOutputStream extends ByteArrayOutputStream {

	PacketOutputStream(int size) {
		super(size);
	}
	byte[] getData() {
		return buf;
	}
};
