package com.jetlang.remote.core;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class StreamReader {

    private byte[] readBuffer = new byte[1024 * 64];
    private final DataInputStream stream;
    private final Charset charset;
    private final ObjectByteReader reader;

    public StreamReader(InputStream stream, Charset charset, ObjectByteReader reader) {
        this.reader = reader;
        this.stream = new DataInputStream(stream);
        this.charset = charset;
    }

    public int readByteAsInt() throws IOException {
        int read = stream.read();
        if (read < 0) {
            throw new IOException("Read End");
        }
        return read;
    }

    public String readString(int topicSizeInBytes) throws IOException {
        byte[] buffer = fillBuffer(topicSizeInBytes);
        return new String(buffer, 0, topicSizeInBytes, charset);
    }

    private byte[] fillBuffer(int totalReadSize) throws IOException {
        if (readBuffer.length < totalReadSize) {
            readBuffer = new byte[totalReadSize];
        }
        int read = 0;
        do {
            read += read(readBuffer, read, totalReadSize - read);
        } while (totalReadSize - read > 0);
        return readBuffer;
    }

    private int read(byte[] buffer, int offset, int length) throws IOException {
        int read = stream.read(buffer, offset, length);
        if (read < 0) {
            throw new IOException("End Read");
        }
        return read;
    }

    public int readInt() throws IOException {
        return stream.readInt();
    }

    public Object readObject(int msgSize) throws IOException {
        byte[] buffer = fillBuffer(msgSize);
        return reader.readObject(buffer, 0, msgSize);
    }
}