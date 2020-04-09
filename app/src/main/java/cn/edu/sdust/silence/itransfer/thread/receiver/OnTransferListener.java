package cn.edu.sdust.silence.itransfer.thread.receiver;

import java.io.File;

/**
 * @Description: <p>
 * <p>
 * @Project: iTransfer
 * @Files: ${CLASS_NAME}
 * @Author: HQ
 * @Version: 0.0.1
 * @Date: 2020/4/9 13:52
 * @Copyright:
 */
public interface OnTransferListener {
    void onProgressChanged(int progress);

    void onTransferFinished(File file);

    void onError();
}
