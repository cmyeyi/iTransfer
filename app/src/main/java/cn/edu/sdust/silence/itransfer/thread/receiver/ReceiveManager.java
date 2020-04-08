package cn.edu.sdust.silence.itransfer.thread.receiver;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import cn.edu.sdust.silence.itransfer.handler.ReceiveActivityHandler;
import cn.edu.sdust.silence.itransfer.common.Constant;

/**
 * 接收文件子线程管理线程
 */
public class ReceiveManager extends Thread {

    private ReceiveActivityHandler receiveActivityHandler;
    private ServerSocket serverSocket;
    private Socket socket = null;
    private Handler managerHandler;
    private long length;
    private String fileName;

    public static int FINISH = 1;
    public static int RETRY = 2;

    public ReceiveManager(ReceiveActivityHandler handler) {
        this.receiveActivityHandler = handler;
        try {
            serverSocket = new ServerSocket(Constant.PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        Looper.prepare();
        managerHandler = new Handler() {

            public void handleMessage(Message msg) {

                if(msg.what == RETRY){
                    try {
                        socket = serverSocket.accept();
                        DataReceiveThread thread = new DataReceiveThread();
                        thread.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (msg.what == FINISH) {
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    managerHandler.getLooper().quit();
                    Thread.interrupted();
                }
            }
        };

        if(serverSocket != null) {
            try {
                socket = serverSocket.accept();
                DataReceiveThread thread = new DataReceiveThread();
                thread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Looper.loop();
    }

    @Override
    public void destroy() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                sendErroeMessage();
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
            String name = fileName.substring(0, fileName.indexOf("."));
            String ext = fileName.substring(fileName.indexOf("."));
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

        private void sendErroeMessage() {
            Message msg = new Message();
            msg.what = ReceiveManager.RETRY;
            managerHandler.sendMessage(msg);
        }

        /**
         * 发送结束信息
         */
        private void sendFinishMessage() {
            Message msg = new Message();
            msg.what = ReceiveManager.FINISH;
            managerHandler.sendMessage(msg);
        }
    }
}
