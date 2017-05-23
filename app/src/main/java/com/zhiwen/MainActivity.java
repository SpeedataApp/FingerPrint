package com.zhiwen;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.serialport.DeviceControl;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.digitalpersona.uareu.Fmd;
import com.mylibrary.FingerManger;
import com.mylibrary.inf.IFingerPrint;
import com.mylibrary.inf.MsgCallBack;
import com.mylibrary.ulits.Data;

import java.io.IOException;


public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    Button btnOpen, btnGetImage, btnGetQuality, btnColse, btnCompare,
            btnCreateTemplate, btnEnroll, btnSearch;
    ImageView fingerImage = null;
    TextView tvMsg, tvNum;
    private IFingerPrint iFingerPrint = null;
    DeviceControl deviceControl;
    DeviceControl deviceContro2;
    private String sss = "";
    private String ssss = "";
    Fmd fmd1 = null;
    Fmd fmd2 = null;
    private byte[] template1;
    private byte[] template2;
    //    int template = 1;
    boolean template = true;
    String TAG = "finger";
    int flg = 0;
    String s1 = "";
    String s2 = "";
    private Dialog dialog;
    int ii = 0;

    Context mContext;
    Activity mActivity;
    private long now;
    private long start;
    Bitmap bitmap = null;
    private Data datas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initGUI();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.hall.success");
        registerReceiver(receiver, intentFilter);
        try {
            deviceControl = new DeviceControl(DeviceControl.PowerType.MAIN_AND_EXPAND, 63, 5, 6);
//            deviceContro2 = new DeviceControl(DeviceControl.PowerType.MAIN, 128);
            deviceControl.PowerOnDevice();
//            deviceContro2.PowerOnDevice();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void initGUI() {
        tvMsg = (TextView) findViewById(R.id.tv_msg);
        tvNum = (TextView) findViewById(R.id.tv_num);
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
        btnEnroll = (Button) findViewById(R.id.btn_Enroll);
        btnEnroll.setOnClickListener(this);
        btnSearch = (Button) findViewById(R.id.btn_search);
        btnSearch.setOnClickListener(this);
        setBtnState(false);

    }

    ProgressDialog mProgressDialog;

    @Override
    protected void onResume() {
        super.onResume();
        start = System.currentTimeMillis();
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(getString(R.string.search_finger));
        mProgressDialog.setMessage(getString(R.string.init_finger));
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                SystemClock.sleep(2000);
                while (iFingerPrint == null) {
                    now = System.currentTimeMillis();
                    iFingerPrint = FingerManger.getIFingerPrintIntance
                            (MainActivity.this, MainActivity.this, handler, new MsgCallBack() {
                                @Override
                                public void callBackInfo(Data data) {
                            handler.sendMessage(handler.obtainMessage(22, data));
                                }
                            });
                    if (now - start > 6000) {
                        finish();
                        break;
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (iFingerPrint != null) {
                            mProgressDialog.cancel();
                        } else {
                            finish();
                            Toast.makeText(MainActivity.this, getString(R.string.init_finger_failed), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).start();

    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 22:
                    datas = (Data) msg.obj;
                    if (datas.isOpenFlag()) {
                        setBtnState(true);
                        btnOpen.setEnabled(false);
                    } else {
                        setBtnState(false);
                        btnOpen.setEnabled(true);
                        tvMsg.setText(datas.getInfoMsg());
                    }
                    if (datas.getTcs1gFmd() != null) {

                        if (template) {
//                            template +=1;
                            template = false;
                            fmd1 = datas.getTcs1gFmd();
                            Toast.makeText(MainActivity.this, "fmd1", Toast.LENGTH_SHORT).show();
                        } else {
                            template = true;
                            fmd2 = datas.getTcs1gFmd();
                            Toast.makeText(MainActivity.this, "fmd2", Toast.LENGTH_SHORT).show();
                        }
                    }
                    if (datas.getTemplateBytes() != null) {
                        if (template) {
                            template = false;
                            template1 = new byte[1024];
                            template1 = datas.getTemplateBytes();
//                    for (int i = 0; i < LAPI.FPINFO_STD_MAX_SIZE; i++) {
//                        sss += String.format("%02x", template1[i]);
//                    }
//                    Log.i(TAG, "handleMessage: " + flg + "\n" + sss);
                            Toast.makeText(MainActivity.this, "template1", Toast.LENGTH_SHORT).show();


                        } else {
                            template = true;
                            template2 = new byte[1024];
                            template2 = datas.getTemplateBytes();
//                    for (int i = 0; i < LAPI.FPINFO_STD_MAX_SIZE; i++) {
//                        ssss += String.format("%02x", template2[i]);
//                    }
//                    Log.i(TAG, "handleMessage: " + flg + "\n" + ssss);
                            Toast.makeText(MainActivity.this, "template2", Toast.LENGTH_SHORT).show();
                        }
                    }
                    tvMsg.setText(datas.getInfoMsg());
                    fingerImage.setImageBitmap(datas.getFingerBitmap());
                    break;
                default:
                    break;
            }
        }
    };
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
        btnGetImage.setEnabled(btnState);
        btnGetQuality.setEnabled(btnState);
        btnColse.setEnabled(btnState);
        btnCompare.setEnabled(btnState);
        btnCreateTemplate.setEnabled(btnState);
        btnEnroll.setEnabled(btnState);
        btnSearch.setEnabled(btnState);
    }

    @Override
    public void onClick(View view) {
        if (view == btnOpen) {
            iFingerPrint.openReader();
        } else if (view == btnColse) {
            iFingerPrint.closeReader();
        } else if (view == btnGetImage) {
            iFingerPrint.getImage();
        } else if (view == btnGetQuality) {
            iFingerPrint.getImageQuality();
            tvMsg.setText(datas.getInfoMsg());
            tvNum.setText(String.format("Quality = %d", datas.getFinferQualitys()));

        } else if (view == btnCreateTemplate) {
            iFingerPrint.createTemplate();

//            tvMsg.setText(datas.getInfoMsg());
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
            iFingerPrint.comparisonFinger();
            tvMsg.setText(datas.getInfoMsg());
            tvNum.setText(getString(R.string.comparison_finger) +":"+ datas.getComparisonNum() + "");
        } else if (view == btnEnroll) {
            iFingerPrint.enrollment();
        } else if (view == btnSearch) {
            iFingerPrint.searchFinger();
        }
    }


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
        if (handler != null) {
            handler = null;
        }
        if (datas != null) {
            datas = null;
        }
        if (mProgressDialog != null) {
            mProgressDialog.cancel();
        }
        fingerImage.refreshDrawableState();
        if (iFingerPrint != null) {
            iFingerPrint.unObject();
            iFingerPrint = null;
        }
        unregisterReceiver(receiver);
        try {
            deviceControl.PowerOffDevice();
//            deviceContro2.PowerOffDevice();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
