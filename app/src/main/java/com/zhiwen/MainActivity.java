package com.zhiwen;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.serialport.DeviceControl;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.digitalpersona.uareu.Fmd;
import com.digitalpersona.uareu.UareUException;
import com.digitalpersona.uareu.dpfj.ImporterImpl;
import com.mylibrary.FingerManger;
import com.mylibrary.inf.IFingerPrint;
import com.mylibrary.ulits.FingerTypes;

import java.io.IOException;
import java.text.DecimalFormat;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    Button btnOpen, btnGetImage, btnGetQuality, btnColse, btnCompare,
            btnCreateTemplate, btnEnroll, btnSearch;
    ImageView fingerImage = null;
    TextView tvMsg;
    private IFingerPrint iFingerPrint = null;
    DeviceControl deviceControl;
    DeviceControl deviceContro2;
    Fmd fmd1 = null;
    Fmd fmd2 = null;
    private byte[] template1;
    private byte[] template2;
    boolean template = true;
    String TAG = "finger";
    private long now;
    private long start;
    Button btnClear;
    private int fingerStata = 0;
    private Fmd jieguo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initGUI();
        TextView textView = (TextView) findViewById(R.id.vesion);
        textView.setText("版本号" + getVersion());
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.hall.success");
        registerReceiver(receiver, intentFilter);
        try {
            deviceControl = new DeviceControl(DeviceControl.PowerType.MAIN_AND_EXPAND, 63);
            deviceContro2 = new DeviceControl(DeviceControl.PowerType.MAIN, 93);
//            deviceControl.PowerOnDevice();
//            deviceContro2.PowerOnDevice();
            showDialog();
            SystemClock.sleep(1000);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void initGUI() {
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
        btnEnroll = (Button) findViewById(R.id.btn_Enroll);
        btnEnroll.setOnClickListener(this);
        btnSearch = (Button) findViewById(R.id.btn_search);
        btnSearch.setOnClickListener(this);

        btnClear = (Button) findViewById(R.id.btn_clear);
        btnClear.setOnClickListener(this);
        setBtnState(false);

    }

    ProgressDialog mProgressDialog;

    public void showDialog() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(getString(R.string.search_finger));
        mProgressDialog.setMessage(getString(R.string.init_finger));
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        start = System.currentTimeMillis();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (iFingerPrint == null) {
                    now = System.currentTimeMillis();
                    iFingerPrint = FingerManger.InitPrintIntance(MainActivity.this, MainActivity.this, handler);
                    fingerStata = FingerTypes.getrwusbdevices(MainActivity.this);

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
                            iFingerPrint.openReader();
                            switch (fingerStata) {
                                case 1:
                                    btnSearch.setVisibility(View.GONE);
                                    btnEnroll.setVisibility(View.GONE);
                                    btnClear.setVisibility(View.GONE);
                                    break;
                                case 2:
                                    btnGetQuality.setVisibility(View.GONE);
                                    break;
                                case 3:
                                    btnGetQuality.setVisibility(View.GONE);
                                    btnSearch.setVisibility(View.GONE);
                                    btnClear.setVisibility(View.GONE);
                                    break;
                            }
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
                case 0:
                    tvMsg.setText((String) msg.obj);
                    break;
                case 1:
                    if ((Boolean) msg.obj) {
                        setBtnState(true);
                        btnOpen.setEnabled(false);
                        tvMsg.setText(getString(R.string.opne_success));
                    } else {
                        setBtnState(false);
                        btnOpen.setEnabled(true);
                        tvMsg.setText(getString(R.string.opne_fail));
                    }
                    break;
                case 2:
                    if ((Boolean) msg.obj) {
                        setBtnState(false);
                        btnOpen.setEnabled(true);
                        fingerImage.setImageBitmap(null);
                        tvMsg.setText(getString(R.string.close_success) + "\n");

                    } else {
                        setBtnState(true);
                        btnOpen.setEnabled(false);
                        tvMsg.setText(getString(R.string.close_fail));
                    }
                    break;
                case 3:
                    Bitmap bitmap = (Bitmap) msg.obj;
                    if (bitmap != null) {
                        fingerImage.setImageBitmap(bitmap);
//                        tvMsg.setText(getString(R.string.get_image_success));
                    } else {

                        tvMsg.setText(getString(R.string.get_image_fail));
                    }

                    break;
                case 4:
                    if (template) {
                        template = false;
                        template1 = new byte[1024];
                        template1 = (byte[]) msg.obj;
//                        for (int i = 0; i < LAPI.FPINFO_STD_MAX_SIZE; i++) {
//                           msg += String.format("%02x", template1[i]);
//                        }
                        Toast.makeText(MainActivity.this, "template1", Toast.LENGTH_SHORT).show();
                    } else {
                        template = true;
                        template2 = new byte[1024];
                        template2 = (byte[]) msg.obj;
                        Toast.makeText(MainActivity.this, "template2", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case 5:
                    if (template) {
                        template = false;
                        fmd1 = (Fmd) msg.obj;
                        byte data[] = fmd1.getData();
//                        Importer.ImportFmd();
                        ImporterImpl importer=new ImporterImpl();
                        try {
                          jieguo=  importer.ImportFmd(data, Fmd.Format.ANSI_378_2004, Fmd.Format.ANSI_378_2004);
                        } catch (UareUException e) {
                            e.printStackTrace();
                        }
//                        try {
//                            ByteArrayInputStream _byteArrInputStream = new ByteArrayInputStream(data);
//                            ObjectInputStream _objectInputStream = new ObjectInputStream(_byteArrInputStream);
//                            jieguo = (Fmd) _objectInputStream.readObject();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        } catch (ClassNotFoundException e) {
//                            e.printStackTrace();
//                        }
//                        for (int i = 0; i < data.length; i++) {
//
//                            Log.i(TAG, "byte:" + data[i]);
//                        }
                        fmdDstas fmDstas = new fmdDstas();
//                        fmDstas.setFmd(fmd1);
                        fmDstas.setFmdByte(fmd1.getData());
                        ObjectAndByte objectAndByte = new ObjectAndByte();
                        byte ddd[] = objectAndByte.toByteArray(fmDstas);
                        Log.i(TAG, "handleMessage: " + new String(ddd));
                        Log.i(TAG, "handleMessage: %%%%%%%%%%%%%%%%%%%%%%%%%");

                        fmdDstas fmdbytes = (fmdDstas) objectAndByte.toObject(ddd);
//                        byte[] fmdbytes = fmdbytes.getFmdByte();
//                        jieguo = fmdbytes.getFmd();
                        Toast.makeText(MainActivity.this, "fmd1", Toast.LENGTH_SHORT).show();
                    } else {
                        template = true;
                        fmd2 = (Fmd) msg.obj;
                        Toast.makeText(MainActivity.this, "fmd2", Toast.LENGTH_SHORT).show();
                    }

                    break;
                case 6:
                    int mScore = (Integer) msg.obj;
                    String comparison = "";
                    if (fingerStata == 3) {
                        DecimalFormat formatting = new DecimalFormat("##.######");
                        comparison = "Dissimilarity Score: " + String.valueOf(mScore) + ", False match rate: "
                                + Double.valueOf(formatting.format((double) mScore / 0x7FFFFFFF)) + " (" + (mScore < (0x7FFFFFFF / 100000) ? "match" : "no match") + ")";
                    } else if (fingerStata == 1) {
                        comparison = String.format(getString(R.string.comparison_finger) + "%d", mScore);
                    } else if (fingerStata == 2) {
                        comparison = getString(R.string.comparison_finger) + mScore;
                    }
                    tvMsg.setText(comparison);
                    break;
                case 7://注册
                    int zhuce = (Integer) msg.obj;
                    String mTextString = null;
                    if (fingerStata == 3) {
                        mTextString = "Enrollment template created, size: " + zhuce;
                    } else {
                        mTextString = getString(R.string.enroll_id) + zhuce;
                    }
                    tvMsg.setText(mTextString);
                    break;
                case 8://搜索
                    tvMsg.setText(getString(R.string.search_id) + msg.obj + "");
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
        btnClear.setEnabled(btnState);
    }

    @Override
    public void onClick(View view) {
        if (view == btnOpen) {
            iFingerPrint.openReader();
        } else if (view == btnColse) {
            iFingerPrint.closeReader();
        } else if (view == btnGetImage) {
            tvMsg.setText(getString(R.string.any_finger));
            iFingerPrint.getImage();
        } else if (view == btnGetQuality) {
            int queality = iFingerPrint.getImageQuality();
            if (queality != 0) {
                tvMsg.setText(queality + "");
            }
        } else if (view == btnCreateTemplate) {
            tvMsg.setText(getString(R.string.any_finger));
            iFingerPrint.createTemplate();
        } else if (view == btnCompare) {
                iFingerPrint.comparisonFinger(template1, template2);
            iFingerPrint.comparisonFinger(jieguo, fmd2);
            iFingerPrint.comparisonFinger();
        } else if (view == btnEnroll) {
            tvMsg.setText(getString(R.string.any_finger));
            iFingerPrint.enrollment();
        } else if (view == btnSearch) {
            tvMsg.setText(getString(R.string.any_finger));
            iFingerPrint.searchFinger();
        } else if (view == btnClear) {
            iFingerPrint.clearFinger();
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
        if (mProgressDialog != null) {
            mProgressDialog.cancel();
        }
        fingerImage.refreshDrawableState();
        if (iFingerPrint != null) {
            iFingerPrint.unObject();
            iFingerPrint = null;
        }
        unregisterReceiver(receiver);
//        try {
//            deviceControl.PowerOffDevice();
////            deviceContro2.PowerOffDevice();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    }

    /**
     * 获取当前应用程序的版本号
     */
    private String getVersion() {
        PackageManager pm = getPackageManager();
        try {
            PackageInfo packinfo = pm.getPackageInfo(getPackageName(), 0);
            String version = packinfo.versionName;
            return version;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "版本号错误";
        }
    }
}
