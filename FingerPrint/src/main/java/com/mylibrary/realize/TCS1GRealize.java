package com.mylibrary.realize;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.util.Log;

import com.digitalpersona.uareu.Engine;
import com.digitalpersona.uareu.Fid;
import com.digitalpersona.uareu.Fmd;
import com.digitalpersona.uareu.Reader;
import com.digitalpersona.uareu.ReaderCollection;
import com.digitalpersona.uareu.UareUException;
import com.digitalpersona.uareu.UareUGlobal;
import com.digitalpersona.uareu.dpfpddusbhost.DPFPDDUsbException;
import com.digitalpersona.uareu.dpfpddusbhost.DPFPDDUsbHost;
import com.mylibrary.inf.IFingerPrint;
import com.mylibrary.ulits.Globals;

/**
 * Created by suntianwei on 2017/4/6.
 */

public class TCS1GRealize implements IFingerPrint {
    Handler handler;

    public TCS1GRealize(Context context, Activity activity ,Handler handler) {
        mContext = context;
        mActivity=activity;
        this.handler = handler;
    }

    private Reader m_reader = null;
    private int m_DPI = 0;
    private Reader.CaptureResult cap_result = null;
    private Context mContext;
    private Activity   mActivity;
    private ReaderCollection readers;
    private String deviceName = "";
    private boolean m_reset = false;
    private Bitmap m_bitmap = null;
    private static final String ACTION_USB_PERMISSION = "com.digitalpersona.uareu.dpfpddusbhost.USB_PERMISSION";
    //    EnrollmentCallback enrollThread = null;
    private Engine m_engine = null;
    private Fmd mFmd = null;

    @Override
    public boolean openReader() {
        openReaders();
        return state;
    }

    @Override
    public boolean closeReader() {
        try {
            m_reader.Close();
            deviceName="";
        } catch (UareUException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public void getImage() {
        creatImage();

    }

    private void creatImage() {
        onBackPressed();
        initReader();
        // loop capture on a separate thread to avoid freezing the UI
        //循环捕获在一个单独的线程来避免冻结UI
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    m_reset = false;
                    while (!m_reset) {
                        cap_result = m_reader.Capture(Fid.Format.ANSI_381_2004, Globals.DefaultImageProcessing, m_DPI, -1);
                        // an error occurred
                        if (cap_result == null || cap_result.image == null)
                            continue;
                        // save bitmap image locally
                        m_bitmap = Globals.GetBitmapFromRaw(cap_result.image.getViews()[0].getImageData(), cap_result.image.getViews()[0].getWidth(), cap_result.image.getViews()[0].getHeight());
                        String m_text_conclusionString = Globals.QualityToString(cap_result);
                        if (m_text_conclusionString.length() == 0) {
                            handler.sendMessage(handler.obtainMessage(5, m_bitmap));
                        } else {
                            handler.sendMessage(handler.obtainMessage(9, m_text_conclusionString));
                        }
                    }
                } catch (Exception e) {
                    if (!m_reset) {
                        Log.w("UareUSampleJava", "error during capture: " + e.toString());
                        deviceName = "";
                        onBackPressed();
                    }
                }
            }
        }).start();

    }

    private Fmd m_enrollment_fmd = null;
    private boolean m_success = false;
    private int m_templateSize = 0;
    private byte[] fid;

    @Override
    public void enrollment() {
//        try {
//            m_reader = Globals.getInstance().getReader(deviceName, mContext);
//            m_reader.Open(Reader.Priority.EXCLUSIVE);//优先
//            m_DPI = Globals.GetFirstDPI(m_reader);
//            m_engine = UareUGlobal.GetEngine();
//        } catch (UareUException e) {
//            e.printStackTrace();
//        }
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    m_current_fmds_count = 0;
//                    m_reset = false;
//                    enrollThread = new EnrollmentCallback(m_reader, m_engine);
//                    while (!m_reset) {
//                        try {
//                            //创建并返回一个注册的fmd
//                            m_enrollment_fmd = m_engine.CreateEnrollmentFmd(Fmd.Format.ANSI_378_2004, enrollThread);
//                            if (m_success = (m_enrollment_fmd != null)) {
//                                //获取Fmd的size
//                                // 返回FMD的完整二进制数据，包括记录头和所有视图
//                                m_templateSize = m_enrollment_fmd.getData().length;
//                                m_current_fmds_count = 0;    // reset count on success
//                            }
//                        } catch (Exception e) {
//                            // template creation failed, reset count
//                            m_current_fmds_count = 0;
//                        }
//                    }
//                } catch (Exception e) {
//                    if (!m_reset) {
//
//                    }
//                }
//            }
//        }).start();
    }

    private String m_text_conclusionString;

    /*
            获取并返回fmd将添加到预注册
           获取并返回PreEnrollmentFmd以创建注册FMD。
           在注册期间，CreateEnrollmentFmd（）重复调用EnrollmentCallback.GetFmd（）
           来获取FMD以进行注册。
        */
//    public class EnrollmentCallback extends Thread implements Engine.EnrollmentCallback {
//        public int m_current_index = 0;
//        private Reader m_reader = null;
//        private Engine m_engine = null;
//
//        public EnrollmentCallback(Reader reader, Engine engine) {
//            m_reader = reader;
//            m_engine = engine;
//        }
//
//        // callback function is called by dp sdk to retrieve fmds until a null is returned
//        @Override
//        public Engine.PreEnrollmentFmd GetFmd(Fmd.Format format) {
//            Engine.PreEnrollmentFmd result = null;
//            while (!m_reset) {
//                try {
//                    cap_result = m_reader.Capture(Fid.Format.ANSI_381_2004, Globals.DefaultImageProcessing, m_DPI, -1);
//                } catch (Exception e) {
//                    Log.w("UareUSampleJava", "error during capture: " + e.toString());
////                    m_deviceName = "";
////                    onBackPressed();
//                }
//                //  ，cap_result.image=fid
//                if (cap_result == null || cap_result.image == null) {
//                    continue;//跳出本次循环
//                }
//                try {
////                    m_enginError = "";
//                    // 本地保存位图图像  ，cap_result.image 获取fid，getImageData()返回视图图像数据，getViews()[0]返回图像视图
//                    //返回一个bitmap
//                    fid = cap_result.image.getViews()[0].getImageData();
//                    Fid fids = cap_result.image;
//                    m_bitmap = Globals.GetBitmapFromRaw(fid, fids.getViews()[0].getWidth(), fids.getViews()[0].getHeight());
//                    Engine.PreEnrollmentFmd prefmd = new Engine.PreEnrollmentFmd();
//                    //提取功能并从ANSI或ISO映像创建FMD。
//                    //此函数与具有每像素8位 - 无填充 - 正方形像素（对于水平和垂直的dpi相同）
//                    // 的FID一起工作。所得FMD的大小将根据特定指纹中的细节而变化。
//                    prefmd.fmd = m_engine.CreateFmd(cap_result.image, Fmd.Format.ANSI_378_2004);
//                    prefmd.view_index = 0;
//                    m_current_fmds_count++;
//
//                    result = prefmd;
//                    break;
//                } catch (Exception e) {
//                    m_enginError = e.toString();
//                    Log.w("UareUSampleJava", "Engine error: " + e.toString());
//                }
//            }
//            //提示录入指纹时信息
//            m_text_conclusionString = Globals.QualityToString(cap_result);
//
//            if (!m_enginError.isEmpty()) {
//                m_text_conclusionString = "Engine: " + m_enginError;
//            }
//
//            if (m_enrollment_fmd != null || m_current_fmds_count == 0) {
//                if (!m_first) {
//                    if (m_text_conclusionString.length() == 0) {
//                        m_text_conclusionString = m_success ? "Enrollment template created, size: " + m_templateSize : "Enrollment template failed. Please try again";
////                        Compressions();压缩 wsq
////                        Toast.makeText(EnrollmentActivity.this,"sucsess",Toast.LENGTH_LONG).show();
//                    }
//                }
//                m_textString = "Place any finger on the reader";
//                m_enrollment_fmd = null;
//            } else {
//                m_first = false;
//                m_success = false;
//                m_textString = "Continue to place the same finger on the reader";
//            }
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    UpdateGUI();
//                }
//            });
//            return result;
//        }
//    }
    private int m_score = -1;

    @Override
    public void comparisonFinger(final Fmd fmd1, final Fmd fmd2) {
        onBackPressed();
        // loop capture on a separate thread to avoid freezing the UI
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (fmd1 != null && fmd2 != null) {
                        m_score = m_engine.Compare(fmd1, 0, fmd2, 0);
                    } else {
                        return;
                    }
                } catch (Exception e) {
                    Log.w("UareUSampleJava", "Engine error: " + e.toString());
                }
                handler.sendMessage(handler.obtainMessage(7, m_score));
            }
        }).start();

    }

    @Override
    public void comparisonFinger(byte[] bytes1, byte[] bytes2) {

    }

    @Override
    public void getImageQuality() {
    }

    @Override
    public boolean clearFinger() {
        return false;
    }

    @Override
    public void unObject() {
        if (!deviceName.equals("")){
            mContext.unregisterReceiver(mUsbReceiver);
        }
    }

    @Override
    public void createTemplate() {
        onBackPressed();
        initReader();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    m_reset = false;
                    while (!m_reset) {
                        cap_result = m_reader.Capture(Fid.Format.ANSI_381_2004, Globals.DefaultImageProcessing, m_DPI, -1);
                        // an error occurred
                        if (cap_result == null || cap_result.image == null)
                            continue;
                        // save bitmap image locally
                        m_bitmap = Globals.GetBitmapFromRaw(cap_result.image.getViews()[0].getImageData(), cap_result.image.getViews()[0].getWidth(), cap_result.image.getViews()[0].getHeight());
                        String m_text_conclusionString = Globals.QualityToString(cap_result);
                        if (m_text_conclusionString.length() == 0) {
                            handler.sendMessage(handler.obtainMessage(5, m_bitmap));
                            mFmd = m_engine.CreateFmd(cap_result.image, Fmd.Format.ANSI_378_2004);
                            if (mFmd != null) {
                                handler.sendMessage(handler.obtainMessage(6, mFmd));
                            }
                        } else {
                            handler.sendMessage(handler.obtainMessage(9, m_text_conclusionString));
                            continue;
                        }
                    }
                } catch (Exception e) {
                    if (!m_reset) {
                        Log.w("UareUSampleJava", "error during capture: " + e.toString());
                        onBackPressed();
                    }
                }
            }
        }).start();
    }

    public void onBackPressed() {
        try {
            m_reset = true;
            try {
                m_reader.CancelCapture();
            } catch (Exception e) {
            }
            m_reader.Close();
        } catch (Exception e) {
            Log.w("UareUSampleJava", "error during reader shutdown");
        }
    }

    public void initReader() {
        try {
            m_reader = Globals.getInstance().getReader(deviceName, mContext);
            m_reader.Open(Reader.Priority.EXCLUSIVE);
            m_DPI = Globals.GetFirstDPI(m_reader);
            m_engine = UareUGlobal.GetEngine();
        } catch (Exception e) {
            Log.w("UareUSampleJava", "error during init of reader");
            return;
        }
    }

    public void openReaders() {
        try {
            readers = Globals.getInstance().getReaders(mContext);
        } catch (UareUException e) {
            e.printStackTrace();
        }
        int nSize = readers.size();
        if (nSize > 0) {
            String[] values = null;
            values = new String[nSize];
            for (int nCount = 0; nCount < nSize; nCount++) {
                deviceName = readers.get(nCount).GetDescription().name;

            }

        } else {
//            return deviceName;
        }
        if ((deviceName != null) && !deviceName.isEmpty()) {
            try {
                m_reader = Globals.getInstance().getReader(deviceName, mContext);

                {
                    PendingIntent mPermissionIntent;
                    mPermissionIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
                    mContext.registerReceiver(mUsbReceiver, filter);

                    if (DPFPDDUsbHost.DPFPDDUsbCheckAndRequestPermissions(mContext, mPermissionIntent, deviceName)) {
                        CheckDevice();
                    }
                }
            } catch (UareUException e1) {
                displayReaderNotFound();
            } catch (DPFPDDUsbException e) {
                displayReaderNotFound();
            }

        } else {
            displayReaderNotFound();
        }

    }

    boolean state = false;

    public void CheckDevice() {
        try {
            m_reader.Open(Reader.Priority.EXCLUSIVE);
            Reader.Capabilities cap = m_reader.GetCapabilities();
            m_reader.Close();
            state = true;
        } catch (UareUException e1) {
            displayReaderNotFound();
//
        }
    }

    private void displayReaderNotFound() {
//        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mActivity);
//        alertDialogBuilder.setTitle("Reader Not Found");
//        alertDialogBuilder.setMessage("Plug in a reader and try again.").setCancelable(false).setPositiveButton("Ok",
//                new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                    }
//                });
//        AlertDialog alertDialog = alertDialogBuilder.create();
//        alertDialog.show();
        state = false;
    }


    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            //call method to set up device communication
                            CheckDevice();
                        }
                    } else {
                    }
                }
            }
        }
    };
}
