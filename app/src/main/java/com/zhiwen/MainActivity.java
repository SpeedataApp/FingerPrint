package com.zhiwen;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.serialport.DeviceControl;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.IDWORLD.LAPI;
import com.digitalpersona.uareu.Fmd;
import com.mylibrary.FingerManger;
import com.mylibrary.inf.IFingerPrint;

import java.io.IOException;
import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    Button btnOpen, btnGetImage, btnGetQuality, btnColse, btnCompare, btnCreateTemplate;
    ImageView fingerImage = null;
    TextView tvMsg;
    private IFingerPrint iFingerPrint;
    DeviceControl deviceControl;
    private String sss = "";
    private String ssss = "";
    Fmd fmd1 = null;
    Fmd fmd2 = null;
    private byte[] template1;
    private byte[] template2;
    boolean template = true;
    String TAG = "finger";
    int flg = 0;
    String s1 = "";
    String s2 = "";
    private Dialog dialog;

    Context mContext;
    Activity mActivity;
    private long now;
    private long start;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.hall.success");
        registerReceiver(receiver, intentFilter);
        try {
            deviceControl = new DeviceControl(DeviceControl.PowerType.MAIN_AND_EXPAND, 63, 6, 5);
            deviceControl.PowerOnDevice();
        } catch (IOException e) {
            e.printStackTrace();
        }
        tvMsg = (TextView) findViewById(R.id.tv_msg);
        fingerImage = (ImageView) findViewById(R.id.btn_imageView);
        btnOpen = (Button) findViewById(R.id.btn_open);
        btnOpen.setOnClickListener(this);
        btnGetImage = (Button) findViewById(R.id.btn_getimage);
        btnGetImage.setOnClickListener(this);
        btnGetQuality = (Button) findViewById(R.id.btn_quality);
        btnGetQuality.setOnClickListener(this);
        btnCreateTemplate = (Button) findViewById(R.id.btn_getTemplate);
        btnCreateTemplate.setOnClickListener(this);
        btnCompare = (Button) findViewById(R.id.btn_compare);
        btnCompare.setOnClickListener(this);
        btnColse = (Button) findViewById(R.id.btn_colse);
        btnColse.setOnClickListener(this);
        setBtnState(false);
    }

    int ii = 0;

    @Override
    protected void onResume() {
        super.onResume();
        start = System.currentTimeMillis();
        dialog = CreateDialog.showLoadingDialog(MainActivity.this, "搜索指纹模板");
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (iFingerPrint == null) {
                    now = System.currentTimeMillis();
                    iFingerPrint = FingerManger.getIFingerPrintIntance(MainActivity.this, MainActivity.this, handler);
                    if (now - start > 4000) {
                        CreateDialog.closeDialog(dialog);
                        return;
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (iFingerPrint == null) {
                            Toast.makeText(MainActivity.this, "请链接指纹模板", Toast.LENGTH_SHORT).show();
//                            finish();
                        } else {
                            setBtnState(true);
                            CreateDialog.closeDialog(dialog);
                        }
                    }
                });
            }
        }).start();

    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("com.hall.success")) {
                try {
                    deviceControl.PowerOnDevice();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (intent.getAction().equals("com.geomobile.hallremove")) {
            }
        }
    };

    private void setBtnState(boolean btnState) {

        btnOpen.setEnabled(btnState);
        btnGetImage.setEnabled(btnState);
        btnGetQuality.setEnabled(btnState);
        btnColse.setEnabled(btnState);
        btnCompare.setEnabled(btnState);
        btnCreateTemplate.setEnabled(btnState);
    }

    @Override
    public void onClick(View view) {
        if (view == btnOpen) {
            if (iFingerPrint.openReader()) {
                tvMsg.setText("Open reader success");
            } else {
                tvMsg.setText("Open reader fail");
            }
        } else if (view == btnColse) {
            if (iFingerPrint.closeReader()) {
                tvMsg.setText("Colse reader success");

            } else {
                tvMsg.setText("Colse reader fail");
            }
        } else if (view == btnGetImage) {
            iFingerPrint.getImage();
        } else if (view == btnGetQuality) {
            iFingerPrint.getImageQuality();
        } else if (view == btnCreateTemplate) {
            iFingerPrint.createTemplate();

        } else if (view == btnCompare) {
//            for (int i = 0; i < template1.length; i++) {
//                s1 += String.format("%02x", template1[i]);
//            }
//            for (int i = 0; i < template2.length; i++) {
//                s2 += String.format("%02x", template2[i]);
//            }
//            Log.i(TAG, "MessageS1: " + "\n" + s1);
//            Log.i(TAG, "MessageS2: " + "\n" + s2);
            iFingerPrint.comparisonFinger(template1, template2);
            iFingerPrint.comparisonFinger(fmd1, fmd2);
        }
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {

                case 1://显示图片  黑色指纹
                    ShowFingerBitmap((byte[]) msg.obj, msg.arg1, msg.arg2);
//                    tvMsg.setText((String) msg.obj);
                    break;
                case 2://获取质量  黑色指纹
                    tvMsg.setText(String.format("CompareTemplates() = %d", (Integer) msg.obj));
                    break;
                case 3://获取模板特征   黑色指纹
                    if (flg == 0) {
//                        template = false;
                        flg = 1;
                        template1 = new byte[1024];
                        template1 = (byte[]) msg.obj;
                        for (int i = 0; i < LAPI.FPINFO_STD_MAX_SIZE; i++) {
                            sss += String.format("%02x", template1[i]);
                        }
                        Log.i(TAG, "handleMessage: " + flg + "\n" + sss);
                        Toast.makeText(MainActivity.this, "template1", Toast.LENGTH_SHORT).show();


                    } else {
//                        template = true;
                        flg = 0;
                        template2 = new byte[1024];
                        template2 = (byte[]) msg.obj;
                        for (int i = 0; i < LAPI.FPINFO_STD_MAX_SIZE; i++) {
                            ssss += String.format("%02x", template2[i]);
                        }
                        Log.i(TAG, "handleMessage: " + flg + "\n" + ssss);
                        Toast.makeText(MainActivity.this, "template2", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case 4://比较指纹模板特征  黑色指纹
                    tvMsg.setText(String.format("CompareTemplates() = %d", (Integer) msg.obj));
                    break;
                case 5:  //显示图片  金色指纹
                    fingerImage.setImageBitmap((Bitmap) msg.obj);
                    break;
                case 6:// 创建指纹模板 金色指纹
                    if (template) {
                        template = false;
                        fmd1 = (Fmd) msg.obj;
                        Toast.makeText(MainActivity.this, "fmd1", Toast.LENGTH_SHORT).show();
                    } else {
                        template = true;
                        fmd2 = (Fmd) msg.obj;
                        Toast.makeText(MainActivity.this, "fmd2", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case 7:
                    int m_score = (int) msg.obj;
                    DecimalFormat formatting = new DecimalFormat("##.######");
                    String conclusionString = "Dissimilarity Score: " + String.valueOf(m_score)
                            + ", False match rate: " +
                            Double.valueOf(formatting.format((double) m_score / 0x7FFFFFFF))
                            + " (" + (m_score < (0x7FFFFFFF / 100000) ? "match" : "no match") + ")";
                    tvMsg.setText(conclusionString);
                    break;
                case 8:
                    tvMsg.setText((String) msg.obj);
                    break;
                case 9:
                    tvMsg.setText((String) msg.obj);
                    break;
                case 10:
//                    fingerImage.setImageBitmap(null);
                    fingerImage.setImageBitmap((Bitmap) msg.obj);
                    break;
                case 11:
                    String temp = charToHexString((byte[]) msg.obj);
                    tvMsg.setText(temp);
                    break;
                default:
                    break;
            }
        }
    };


    private static String charToHexString(byte[] val) {
        String temp = "";
        for (int i = 0; i < val.length; i++) {
            String hex = Integer.toHexString(0xff & val[i]);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            temp += hex.toUpperCase();
        }
        return temp;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (iFingerPrint != null) {
            iFingerPrint.unObject();
        }
        unregisterReceiver(receiver);
//        try {
//            deviceControl.PowerOffDevice();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private void ShowFingerBitmap(byte[] image, int width, int height) {
        if (width == 0) return;
        if (height == 0) return;

        int[] RGBbits = new int[width * height];
        fingerImage.invalidate();
        for (int i = 0; i < width * height; i++) {
            int v;
            if (image != null) v = image[i] & 0xff;
            else v = 0;
            RGBbits[i] = Color.rgb(v, v, v);
        }
        Bitmap bmp = Bitmap.createBitmap(RGBbits, width, height, Bitmap.Config.RGB_565);
        fingerImage.setImageBitmap(bmp);
    }

}
