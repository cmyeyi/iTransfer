package cn.edu.sdust.silence.itransfer.activity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import cn.edu.sdust.silence.itransfer.R;
import cn.edu.sdust.silence.itransfer.qrcode.utils.ZXingUtil;

public class QRCodeActivity extends AppCompatActivity {
    private int QR_WIDTH = 300;
    private int QR_HEIGHT = 300;
    private ImageView qrcodeView;
    private Button createButton;
    private EditText connectView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_send_layout);

        initView();
    }

    private void initView() {
        qrcodeView = findViewById(R.id.id_qrcode_view);

        Bitmap bitmap = ZXingUtil.createQRCode(getAddress("ca:3d:dc:a3:7b:0e").toString(),QR_WIDTH,QR_WIDTH);
        qrcodeView.setImageBitmap(bitmap);
    }

    private String getAddress(String address) {
        return address;
    }

}
