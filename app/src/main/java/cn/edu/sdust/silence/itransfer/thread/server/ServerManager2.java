package cn.edu.sdust.silence.itransfer.thread.server;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import cn.edu.sdust.silence.itransfer.activity.SendActivity;
import cn.edu.sdust.silence.itransfer.handler.SendActivityHandler;
import cn.edu.sdust.silence.itransfer.common.Constant;

/**
 * 发送文件子线程管理线程
 */
public class ServerManager2 extends Thread {

    public static int RETRY = 1;
    public static int FINISH = 2;
    private String ip;
    private long length;
    private String fileName;
    private String filePath;
    private Handler managerHandler;
    private SendActivityHandler sendActivityHandler;


    public ServerManager2(SendActivityHandler sendActivityHandler, String ip, String filePath) {
        this.sendActivityHandler = sendActivityHandler;
        this.ip = ip;
        this.filePath = filePath;
    }

    @Override
    public void run() {
        Looper.prepare();
        managerHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == RETRY) {
                    DataServerThread server = new DataServerThread();
                    server.start();
                } else if (msg.what == FINISH) {
                    managerHandler.getLooper().quit();
                    Thread.interrupted();
                }
            }
        };
        DataServerThread server = new DataServerThread();
        server.start();
        Looper.loop();

    }


    class DataServerThread extends Thread {

        @Override
        public void run() {

            Socket socket = new Socket();

            try {
                socket.connect((new InetSocketAddress(ip, Constant.PORT)),1000);

                OutputStream os = socket.getOutputStream();
                InputStream is = socket.getInputStream();
                File file = new File(filePath);

                fileName = file.getName();
                os.write(fileName.getBytes());

                String serverInfo = servInfoBack(is);
                if (serverInfo.equals("FileLengthSendNow")) {
                    length = file.length();
                    os.write(("" + length).getBytes());
                }

                String serverInfo2 = servInfoBack(is);
                if (serverInfo2.equals("FileSendNow")) {
                    FileInputStream inputStream = new FileInputStream(file);
                    copyFile(inputStream, os);
                    inputStream.close();
                }

                is.close();
                os.close();
                socket.close();
                sendFinishMessage();
            } catch (Exception e) {
                sendErrorMessage();
                e.printStackTrace();
            }
            super.run();
        }


        public String servInfoBack(InputStream is) throws Exception {
            byte[] bufIs = new byte[1024];
            int lenIn = is.read(bufIs);
            String info = new String(bufIs, 0, lenIn);
            return info;
        }

        public boolean copyFile(InputStream inputStream, OutputStream out) {
            byte buf[] = new byte[1024];
            int len;
            long sendLength = 0;
            try {
                while ((len = inputStream.read(buf)) != -1) {
                    out.write(buf, 0, len);
                    sendLength += len;
                    sendProgress((int) (sendLength * 100 / length));
                }
            } catch (IOException e) {
                return false;
            }
            return true;
        }

        private void sendErrorMessage() {
            Message msg = new Message();
            msg.what = ServerManager2.RETRY;
            managerHandler.sendMessage(msg);
        }

        private void sendFinishMessage() {
            Message msg = new Message();
            msg.what = ServerManager2.FINISH;
            managerHandler.sendMessage(msg);
        }

        private void sendProgress(int progress) {
            Message msg = new Message();
            msg.what = SendActivity.PROGRESS;
            msg.arg1 = progress;
            sendActivityHandler.sendMessage(msg);
        }
    }
}
