package cn.edu.sdust.silence.itransfer.qrcode.utils;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description: <p>
 * <p>
 * @Project: iTransfer
 * @Files: ${CLASS_NAME}
 * @Author: HQ
 * @Version: 0.0.1
 * @Date: 2020/4/6 16:21
 * @Copyright:
 */
public class ZXingUtil {

    /**
     *
     * @param name
     * @param width
     * @param height
     * @return
     */
    public static Bitmap createQRCode(String name, int width, int height) {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, String> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8"); //记得要自定义长宽
        BitMatrix encode = null;
        try {
            encode = qrCodeWriter.encode(name, BarcodeFormat.QR_CODE, width, height, hints);
        } catch (WriterException e) {
            e.printStackTrace();
        }
        int[] colors = new int[width * height];
        //利用for循环将要表示的信息写出来
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (encode.get(i, j)) {
                    colors[i * width + j] = Color.BLACK;
                } else {
                    colors[i * width + j] = Color.WHITE;
                }
            }
        }

        return Bitmap.createBitmap(colors, width, height, Bitmap.Config.RGB_565);
    }
}
