package cn.edu.sdust.silence.itransfer.thread.server;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import cn.edu.sdust.silence.itransfer.common.Constant;
import cn.edu.sdust.silence.itransfer.thread.receiver.OnTransferListener;

public class SendTask extends AsyncTask<String, Integer, Boolean> {

    private static final String TAG = "WifiClientTask";
    private String ip;
    private ServerSocket serverSocket;
    private Socket socket;
    private long length;
    private String filePath;
    private String fileName;
    OnTransferListener onTransferListener;
    public SendTask(boolean isGroupOwner, String ip,String filePath) {
        this.ip = ip;
        this.filePath  = filePath;
        createSocket(isGroupOwner);
    }

    public void setOnTransferListener(OnTransferListener onTransferListener) {
        this.onTransferListener = onTransferListener;
    }

    private void createSocket(boolean isGroupOwner) {
        if(isGroupOwner) {
            try {
                serverSocket = new ServerSocket(Constant.PORT);
                socket = serverSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                socket = new Socket();
                socket.connect((new InetSocketAddress(ip, Constant.PORT)), 30000);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onPreExecute() {
        onTransferListener.onProgressChanged(1);
    }

    @Override
    protected Boolean doInBackground(String... strings) {
        Log.w("#####", "doInBackground");
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
        return false;
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
    protected void onProgressUpdate(Integer... values) {
        Log.i(TAG, "onProgressUpdate values: " + values.toString());
//        onTransferProgress(values[0]);
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        onTransferFinished();
        Log.e(TAG, "onPostExecute: " + aBoolean);
    }

    private void onTransferProgress(int progress) {
        Log.i("#####", "文件发送进度: " + progress);
        if (onTransferListener != null) {
            onTransferListener.onProgressChanged(progress);
        }
    }

    private void onTransferFinished(){
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
                onTransferProgress((int) (sendLength * 100 / length));
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

}
