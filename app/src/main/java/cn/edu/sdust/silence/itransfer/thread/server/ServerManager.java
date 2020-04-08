package cn.edu.sdust.silence.itransfer.thread.server;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import cn.edu.sdust.silence.itransfer.activity.SendActivity;
import cn.edu.sdust.silence.itransfer.common.Constant;

/**
 * 发送文件子线程管理线程
 */
public class ServerManager extends Thread {

    private ServerSocket serverSocket;
    private Socket socket;
    private long length;
    private String filePath;
    private String fileName;
    private Handler managerHandler;
    private SendActivityHandler sendActivityHandler;

    public ServerManager(SendActivityHandler sendActivityHandler, String filePath) {
        this.sendActivityHandler = sendActivityHandler;
        this.filePath = filePath;

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
            @Override
            public void handleMessage(Message msg) {

                if (msg.what == Constant.RETRY) {
                    try {
                        socket = serverSocket.accept();
                        DateServerThread thread = new DateServerThread();
                        thread.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (msg.what == Constant.FINISH) {
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
                if(socket != null) {
                    DateServerThread thread = new DateServerThread();
                    thread.start();
                    return;
                }
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


    class DateServerThread extends Thread {

        @Override
        public void run() {

            try {


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
                sendFinishMessage();
            } catch (IOException e) {
                sendErrorMessage();
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
            msg.what = Constant.RETRY;
            managerHandler.sendMessage(msg);
        }

        private void sendFinishMessage() {
            Message msg = new Message();
            msg.what = Constant.FINISH;
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
