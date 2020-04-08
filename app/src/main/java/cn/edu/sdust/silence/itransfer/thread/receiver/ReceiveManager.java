package cn.edu.sdust.silence.itransfer.thread.receiver;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import cn.edu.sdust.silence.itransfer.common.Constant;

/**
 * 接收文件子线程管理线程
 */
public class ReceiveManager extends Thread {

    private String ip;
    private ServerSocket serverSocket;
    private Socket socket;
    private String fileName;
    private long length;
    private Handler managerHandler;
    private ReceiveActivityHandler receiveActivityHandler;

    public ReceiveManager(ReceiveActivityHandler handler, String ip) {
        this.receiveActivityHandler = handler;
        if(!TextUtils.isEmpty(ip)) {
            this.ip = ip;
        } else {
            try {
                serverSocket = new ServerSocket(Constant.PORT);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {

        Looper.prepare();
        managerHandler = new Handler() {

            public void handleMessage(Message msg) {

                if (msg.what == Constant.RETRY) {
                    startDataReceiveThread();
                }
                if (msg.what == Constant.FINISH) {
                    closeServerSocket();
                    managerHandler.getLooper().quit();
                    Thread.interrupted();
                }
            }
        };

        startDataReceiveThread();
        Looper.loop();
    }

    private void closeServerSocket() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void startDataReceiveThread() {
        createSocket();
        DataReceiveThread thread = new DataReceiveThread();
        thread.start();
    }

    private void createSocket() {
        if (serverSocket != null) {
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                socket = new Socket();
                socket.connect((new InetSocketAddress(ip, Constant.PORT)), 1000);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void destroy() {
        closeServerSocket();
        super.destroy();
    }

    class DataReceiveThread extends Thread {

        @Override
        public void run() {

            try {
                InputStream is = socket.getInputStream();

                File file = getClientFileName(is);
                length = getFileLength(is);

                FileOutputStream os = new FileOutputStream(file);
                copyFile(is, os);

                is.close();
                os.close();
                sendFinishMessage();
            } catch (IOException e) {
                sendErrorMessage();
                Log.e("xyz", e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public boolean copyFile(InputStream inputStream, OutputStream out) {
            byte buf[] = new byte[1024];
            long process = 0;
            int len;
            try {
                while ((len = inputStream.read(buf)) != -1) {
                    out.write(buf, 0, len);
                    Message msg = new Message();
                    process += len;

                    msg.what = ReceiveActivityHandler.TYPE_PROCESS;
                    msg.arg1 = (int) (process * 100 / length);
                    receiveActivityHandler.sendMessage(msg);
                }
            } catch (IOException e) {
                return false;
            }
            return true;
        }

        public File getClientFileName(InputStream is) throws Exception {
            byte[] buf = new byte[1024];
            int len = 0;
            len = is.read(buf); // 获取文件名
            fileName = new String(buf, 0, len);

            //如果文件存在，重命名
            File file = new File(Environment.getExternalStorageDirectory() + "/iTransfer/files/" + fileName);
            String name = fileName;
            String ext = "";
            if (fileName.contains(".")) {
                name = fileName.substring(0, fileName.indexOf("."));
                ext = fileName.substring(fileName.indexOf("."));
            }
            for (int i = 1; !file.createNewFile(); i++) {
                file = new File(Environment.getExternalStorageDirectory() + "/iTransfer/files/" + name + "(" + i + ")" + ext);
            }

            writeOutInfo(socket, "FileLengthSendNow");
            return file;
        }

        public int getFileLength(InputStream is) throws Exception {
            byte[] buf = new byte[1024];
            int len = 0;
            len = is.read(buf); // get file length
            String length = new String(buf, 0, len);
            writeOutInfo(socket, "FileSendNow");
            return Integer.parseInt(length);
        }

        public void writeOutInfo(Socket socket, String infoStr) throws Exception {
            OutputStream sockOut = socket.getOutputStream();
            sockOut.write(infoStr.getBytes());
        }

        /**
         * 发送错误信息
         */
        private void sendErrorMessage() {
            Message msg = new Message();
            msg.what = Constant.RETRY;
            managerHandler.sendMessage(msg);
        }

        /**
         * 发送结束信息
         */
        private void sendFinishMessage() {
            Message msg = new Message();
            msg.what = Constant.FINISH;
            managerHandler.sendMessage(msg);
        }
    }
}
