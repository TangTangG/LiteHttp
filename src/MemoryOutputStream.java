import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MemoryOutputStream extends OutputStream {

    private int writePos = 0;
    private int maxSize = 0;
    private byte[] dataBuf;
    private volatile int inStreamNum = 0;

    public MemoryOutputStream() {
        this(0);
    }

    public MemoryOutputStream(int initialCapacity) {
        this.maxSize = initialCapacity;
    }

    public void reset(int capacity) {
        assert inStreamNum == 0;
        writePos = 0;
        _enlargeBuffer(capacity);
    }

    public void expand(int capacity) {
        _enlargeBuffer(capacity);
    }

    public byte[] getDataBuffer() {
        return dataBuf;
    }

    public int getDataSize() {
        return writePos;
    }

    public void setDataSize(int size) {
        writePos = size;
    }

    private void _enlargeBuffer(int sz) {
        int newMaxSize = (sz + 0x3FFF) & ~0x3FFF;
        if (newMaxSize > maxSize) {
            maxSize = newMaxSize;
        } else {
            newMaxSize = maxSize;
            if (dataBuf != null) {
                return;
            }
        }
        byte[] newBuf = new byte[newMaxSize];
        if (dataBuf != null) {
            System.arraycopy(dataBuf, 0, newBuf, 0, writePos);
        }
        dataBuf = newBuf;
    }

    @Override
    public void write(int oneByte) throws IOException {
        int writePos = this.writePos;
        int maxSize = this.maxSize;
        if (dataBuf == null || writePos + 1 >= maxSize) {
            _enlargeBuffer(maxSize + 128);
        }
        dataBuf[writePos] = (byte) oneByte;
        ++writePos;
        this.writePos = writePos;
    }

    @Override
    public void write(byte[] buffer, int offset, int count) throws IOException {
        int writePos = this.writePos;
        int maxSize = this.maxSize;
        int bufferLen = count;
        if (dataBuf == null || writePos + bufferLen >= maxSize) {
            _enlargeBuffer(maxSize + bufferLen + 32);
        }
        System.arraycopy(buffer, offset, dataBuf, writePos, bufferLen);
        writePos += bufferLen;
        this.writePos = writePos;
    }

    public InputStream toInputStream() {
        return toInputStream(0, writePos);
    }

    public InputStream toInputStream(int len) {
        return toInputStream(0, len);
    }

    public InputStream toInputStream(int offset, int len) {
        if (offset > writePos) {
            offset = writePos;
        }
        if (offset + len > writePos) {
            len = writePos - offset;
        }
        final int finalOffset = offset;
        final int finalLen = len;
        InputStream inputStream = new InputStream() {
            @Override
            protected void finalize() throws Throwable {
                close();
                super.finalize();
            }

            int _maxSize = finalOffset + finalLen;
            int _readPos = finalOffset;
            byte[] _dataBuf = MemoryOutputStream.this.dataBuf;
            int _markPos = finalOffset;

            @Override
            public int read() throws IOException {
                if (_readPos >= _maxSize) {
                    return -1;
                }
                return _dataBuf[_readPos++];
            }

            @Override
            public int read(byte[] buffer, int offset, int length) throws IOException {
                int readPos = _readPos;
                int availableBytes = _maxSize - readPos;
                if (availableBytes <= 0) {
                    return -1;
                }
                int readBytes = Math.min(availableBytes, length);
                System.arraycopy(_dataBuf, readPos, buffer, offset, readBytes);
                readPos += readBytes;
                _readPos = readPos;
                return readBytes;
            }

            @Override
            public int available() throws IOException {
                return _maxSize - _readPos;
            }

            @Override
            public synchronized void reset() throws IOException {
                _readPos = _markPos;
                _markPos = finalOffset;
            }

            @Override
            public boolean markSupported() {
                return true;
            }

            @Override
            public void mark(int readlimit) {
                _markPos = _readPos;
            }

            @Override
            public long skip(long byteCount) throws IOException {
                int availableBytes = _maxSize - _readPos;
                byteCount = Math.min(availableBytes, byteCount);
                _readPos += byteCount;
                return byteCount;
            }

            @Override
            public void close() throws IOException {
                if (_dataBuf == null) {
                    return;
                }
                _dataBuf = null;
                _readPos = 0;
                _maxSize = 0;
                releaseRef();
            }
        };
        addRef();
        return inputStream;
    }

    private synchronized void addRef() {
        ++inStreamNum;
    }

    private synchronized void releaseRef() {
        if (--inStreamNum == 0) {
            onClose();
        }
    }

    public void onClose() {

    }
}
