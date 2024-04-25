package android.serialport;

import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SerialPort {
    private static final String TAG = "SerialPort";

    static {
        System.loadLibrary("serial_port");
    }

    private final FileInputStream mFileInputStream;
    private final FileOutputStream mFileOutputStream;

    public SerialPort(File device, int baudrate, int flags) throws SecurityException, IOException {
        FileDescriptor mFd = open(device.getAbsolutePath(), baudrate, flags);
        if (mFd == null) {
            Log.e(TAG, "native open returns null");
            throw new IOException(device.getAbsolutePath());
        }
        this.mFileInputStream = new FileInputStream(mFd);
        this.mFileOutputStream = new FileOutputStream(mFd);
    }

    public SerialPort(String devicePath, int baudrate, int flags) throws SecurityException, IOException {
        this(new File(devicePath), baudrate, flags);
    }

    public SerialPort(File device, int baudrate) throws SecurityException, IOException {
        this(device, baudrate, 0);
    }

    public SerialPort(String devicePath, int baudrate) throws SecurityException, IOException {
        this(new File(devicePath), baudrate, 0);
    }

    public SerialPort(String devicePath) throws IOException {
        this(devicePath, 115200);
    }

    private static native FileDescriptor open(String path, int baudrate, int flags);

    public native void close();

    public InputStream getInputStream() {
        return this.mFileInputStream;
    }

    public OutputStream getOutputStream() {
        return this.mFileOutputStream;
    }
}
