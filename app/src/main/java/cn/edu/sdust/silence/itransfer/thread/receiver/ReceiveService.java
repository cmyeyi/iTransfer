package cn.edu.sdust.silence.itransfer.thread.receiver;

import android.app.IntentService;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
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

import static cn.edu.sdust.silence.itransfer.common.Constant.TIMEOUT;

/**
 * @Description: <p>
 * <p>
 * @Project: iTransfer
 * @Files: ${CLASS_NAME}
 * @Author: HQ
 * @Version: 0.0.1
 * @Date: 2020/4/9 11:14
 * @Copyright:
 */
public class ReceiveService extends IntentService {

    public final static String IS_GROUPOWNER = "isGroupOwner";
    public final static String P2P_MAC = "P2P_MAC";
    private String ip;
    private ServerSocket serverSocket;
    private Socket socket;
    private String fileName;
    private long length;
    private OnTransferListener onTransferListener;

    public class MyBinder extends Binder {
        public ReceiveService getService() {
            return ReceiveService.this;
        }
    }

    public ReceiveService() {
        super("ReceiveService");
    }

    public void setOnTransferListener(OnTransferListener onTransferListener) {
        this.onTransferListener = onTransferListener;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        try {
            createSocket(intent);
            InputStream is = socket.getInputStream();

            File file = getClientFileName(is);
            length = getFileLength(is);

            FileOutputStream os = new FileOutputStream(file);
            copyFile(is, os);

            is.close();
            os.close();
            onTransferFinished();
        } catch (IOException e) {
            onTransferError();
            e.printStackTrace();
        } catch (Exception e) {
            onTransferError();
            e.printStackTrace();
        } finally {
            clean();
        }
    }

    private void createSocket(@Nullable Intent intent) {
        clean();
        boolean isGroupOwner = intent.getBooleanExtra(IS_GROUPOWNER, false);
        ip = intent.getStringExtra(P2P_MAC);
        Log.e("######", "createSocket,isGroupOwner=" + isGroupOwner + ",mac=" + ip);
        if (isGroupOwner) {
            try {
                serverSocket = new ServerSocket(Constant.PORT);
                socket = serverSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                socket = new Socket();
                socket.connect((new InetSocketAddress(ip, Constant.PORT)), TIMEOUT);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void onTransferProgress(int progress) {
        Log.i("#####", "文件接收进度: " + progress);
        if (onTransferListener != null) {
            onTransferListener.onProgressChanged(progress);
        }
    }

    private void onTransferFinished() {
        Log.i("#####", "onTransferFinished");
        if (onTransferListener != null) {
            onTransferListener.onTransferFinished(null);
        }
    }

    private void onTransferError() {
        Log.i("#####", "onTransferError");
        if (onTransferListener != null) {
            onTransferListener.onError();
        }
    }

    public boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        long total = 0;
        int progress;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);
                total += len;
                progress = (int) (total * 100 / length);
                onTransferProgress(progress);
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

    private void clean() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                serverSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
                socket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void unbindService(ServiceConnection conn) {
        clean();
        super.unbindService(conn);
    }

    @Override
    public void onDestroy() {
        clean();
        super.onDestroy();
    }
}
