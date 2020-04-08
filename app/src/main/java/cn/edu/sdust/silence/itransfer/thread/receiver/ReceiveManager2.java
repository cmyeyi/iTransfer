package cn.edu.sdust.silence.itransfer.thread.receiver;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import cn.edu.sdust.silence.itransfer.handler.ReceiveActivityHandler;
import cn.edu.sdust.silence.itransfer.common.Constant;

/**
 * 接收文件子线程管理线程
 */
public class ReceiveManager2 extends Thread {

    public static int RETRY = 01;
    public static int FINISH = 02;
    private Handler managerHandler;
    private ReceiveActivityHandler receiveActivityHandler;
    private String ip;

    public ReceiveManager2(ReceiveActivityHandler receiveActivityHandler, String ip) {
        this.receiveActivityHandler = receiveActivityHandler;
        this.ip = ip;
    }

    @Override
    public void run() {

        Looper.prepare();
        managerHandler = new Handler() {

            public void handleMessage(Message msg) {

                if (msg.what == RETRY) {
                    DataReceiveThread thread = new DataReceiveThread(managerHandler, receiveActivityHandler, ip);
                    thread.start();
                } else if (msg.what == FINISH) {
                    managerHandler.getLooper().quit();
                    Thread.interrupted();
                }
            }
        };

        DataReceiveThread thread = new DataReceiveThread(managerHandler, receiveActivityHandler, ip);
        thread.start();
        Looper.loop();
    }

    static class DataReceiveThread extends Thread {

        private Socket socket;
        private String ip;
        private String fileName;
        private long length; //文件大小
        private ReceiveActivityHandler receiveActivityHandler;
        private Handler managerHandler;

        public DataReceiveThread(Handler managerHandler, ReceiveActivityHandler receiveActivityHandler, String ip) {
            this.ip = ip.trim();
            fileName = "";
            length = 0;
            this.receiveActivityHandler = receiveActivityHandler;
            this.managerHandler = managerHandler;
        }

        @Override
        public void run() {
            socket = new Socket();
            try {
                socket.connect((new InetSocketAddress(ip, Constant.PORT)), 1000);
                InputStream is = socket.getInputStream();

                File file = getClientFileName(is);
                length = getFileLength(is);
                FileOutputStream os = new FileOutputStream(file);
                copyFile(is, os);

                is.close();
                os.close();
                socket.close();
                sendFinishMessage();
            } catch (Exception e) {
                sendErrorMessage();
                e.printStackTrace();
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

        /**
         * 发送错误信息
         */
        private void sendErrorMessage() {
            Message msg = new Message();
            msg.what = ReceiveManager2.RETRY;
            managerHandler.sendMessage(msg);
        }

        /**
         * 发送结束信息
         */
        private void sendFinishMessage() {
            Message msg = new Message();
            msg.what = ReceiveManager2.FINISH;
            managerHandler.sendMessage(msg);
        }
    }
}
