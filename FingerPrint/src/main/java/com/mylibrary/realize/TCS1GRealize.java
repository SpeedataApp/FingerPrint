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
import com.digitalpersona.uareu.dpfj.ImporterImpl;
import com.digitalpersona.uareu.dpfpddusbhost.DPFPDDUsbException;
import com.digitalpersona.uareu.dpfpddusbhost.DPFPDDUsbHost;
import com.mylibrary.R;
import com.mylibrary.inf.IFingerPrint;
import com.mylibrary.ulits.Globals;

import java.util.ArrayList;

/**
 * Created by suntianwei on 2017/4/5.
 */

public class TCS1GRealize implements IFingerPrint {
    private String TAG = "Finger_TCS1G";
    private Reader m_reader = null;
    private int m_DPI = 0;
    private Reader.CaptureResult cap_result = null;
    private Context mContext;
    private Activity mActivity;
    private ReaderCollection readers;
    private String deviceName = "";
    private boolean m_reset = false;
    private Bitmap m_bitmap = null;
    private static final String ACTION_USB_PERMISSION = "com.digitalpersona.uareu.dpfpddusbhost.USB_PERMISSION";
    EnrollmentCallback enrollThread = null;
    private Engine m_engine = null;
    private Fmd mFmd = null;
    private boolean m_success = false;
    private int m_templateSize = 0;
    private int m_current_fmds_count = 0;
    private String m_enginError;
    private boolean m_first = true;
    private String m_textString;
    private int m_score = -1;
    Handler mHandler;
    private byte[] fmdbytes = null;
    private ImporterImpl importer;

    public TCS1GRealize(Context context, Activity activity, Handler handler) {
        mContext = context;
        mActivity = activity;
        mHandler = handler;
    }

    @Override
    public void openReader() {
        openReaders();
    }

    @Override
    public void closeReader() {
        if (!deviceName.equals("")) {
            mContext.unregisterReceiver(mUsbReceiver);
            deviceName = "";
        }
        try {
            m_reset = true;
            try {
                m_reader.CancelCapture();
            } catch (Exception e) {
            }
            m_reader.Close();
        } catch (Exception e) {
            Log.w(TAG, "error during reader shutdown");
            mHandler.sendMessage(mHandler.obtainMessage(2, false));
        }
        mHandler.sendMessage(mHandler.obtainMessage(2, true));
    }

    @Override
    public void getImage() {
        cancelReader();
        initReader();
        CaptureImages();
    }


    private void CaptureImages() {
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
                            mHandler.sendMessage(mHandler.obtainMessage(3, m_bitmap));

                        } else {
                            mHandler.sendMessage(mHandler.obtainMessage(0, m_text_conclusionString));
                        }
                    }
                } catch (Exception e) {
                    if (!m_reset) {
                        Log.w(TAG, "error during capture: " + e.toString());
                        mHandler.sendMessage(mHandler.obtainMessage(0, mContext.getString(R.string.erro_captrue)));
                        cancelReader();
                    }
                }
            }
        }).start();
    }


    private void cancelReader() {
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


    @Override
    public void enrollment() {
        cancelReader();
        initReader();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    m_current_fmds_count = 0;
                    m_reset = false;
                    enrollThread = new EnrollmentCallback(m_reader, m_engine);
                    while (!m_reset) {
                        try {
                            //创建并返回一个注册的fmd
                            mFmd = m_engine.CreateEnrollmentFmd(Fmd.Format.ANSI_378_2004, enrollThread);
                            if (m_success = (mFmd != null)) {
                                //获取Fmd的size
                                // 返回FMD的完整二进制数据，包括记录头和所有视图
                                fmdbytes = mFmd.getData();
                                m_current_fmds_count = 0;    // reset count on success
                            }
                        } catch (Exception e) {
                            // template creation failed, reset count
                            m_current_fmds_count = 0;
                            mHandler.handleMessage(mHandler.obtainMessage(0, mContext.getString(R.string.template_fail_again)));
                            m_reset = true;
                        }
                    }
                } catch (Exception e) {
                }
            }
        }).start();
    }

    /*
       获取并返回fmd将添加到预注册
       获取并返回PreEnrollmentFmd以创建注册FMD。
       在注册期间，CreateEnrollmentFmd（）重复调用EnrollmentCallback.GetFmd（）
       来获取FMD以进行注册。
     */
    public class EnrollmentCallback extends Thread implements Engine.EnrollmentCallback {
        public int m_current_index = 0;

        private Reader m_reader = null;
        private Engine m_engine = null;

        public EnrollmentCallback(Reader reader, Engine engine) {
            m_reader = reader;
            m_engine = engine;
        }

        // callback function is called by dp sdk to retrieve fmds until a null is returned
        @Override
        public Engine.PreEnrollmentFmd GetFmd(Fmd.Format format) {
            Engine.PreEnrollmentFmd result = null;
            while (!m_reset) {
                try {
                    cap_result = m_reader.Capture(Fid.Format.ANSI_381_2004, Globals.DefaultImageProcessing, m_DPI, -1);
                } catch (Exception e) {
                    Log.w(TAG, "error during capture: " + e.toString());
                    mHandler.sendMessage(mHandler.obtainMessage(0, mContext.getString(R.string.erro_captrue)));
                    cancelReader();
                }

                // an error occurred
                if (cap_result == null || cap_result.image == null) continue;

                try {
                    m_enginError = "";
                    // save bitmap image locally
                    m_bitmap = Globals.GetBitmapFromRaw(cap_result.image.getViews()[0].getImageData(), cap_result.image.getViews()[0].getWidth(), cap_result.image.getViews()[0].getHeight());
                    mHandler.sendMessage(mHandler.obtainMessage(3, m_bitmap));
                    Engine.PreEnrollmentFmd prefmd = new Engine.PreEnrollmentFmd();
                    prefmd.fmd = m_engine.CreateFmd(cap_result.image, Fmd.Format.ANSI_378_2004);
                    prefmd.view_index = 0;
                    m_current_fmds_count++;

                    result = prefmd;
                    break;
                } catch (Exception e) {
                    m_enginError = e.toString();
                    Log.w(TAG, "Engine error: " + e.toString());
                }
            }

            String m_text_conclusionString = Globals.QualityToString(cap_result);

            if (!m_enginError.isEmpty()) {
                m_text_conclusionString = "Engine: " + m_enginError;
            }

            if (mFmd != null || m_current_fmds_count == 0) {
                if (!m_first) {
                    if (m_text_conclusionString.length() == 0) {
                        if (m_success) {
                            mHandler.sendMessage(mHandler.obtainMessage(4, fmdbytes));
                            mHandler.sendMessage(mHandler.obtainMessage(0, mContext.getString(R.string.enrollment_success)));
                            m_reset = true;
                        } else {
                            mHandler.sendMessage(mHandler.obtainMessage(0, mContext.getString(R.string.same_finger)));
                        }
                    }
                }
//                m_textString = "Place any finger on the reader";
                mFmd = null;
            } else {
                m_first = false;
                m_success = false;
//                mHandler.sendMessage(mHandler.obtainMessage(0, mContext.getString(R.string.same_finger)));
            }
            return result;
        }

    }


    @Override
    public void comparisonFinger(byte[] bytes, ArrayList<byte[]> array) {
        importer = new ImporterImpl();
        Fmd fmds = null;
        if (bytes != null) {
            try {
                fmds = importer.ImportFmd(bytes, Fmd.Format.ANSI_378_2004, Fmd.Format.ANSI_378_2004);
            } catch (UareUException e) {
                e.printStackTrace();
            }

            if (array.size() > 0) {
                Fmd[] fmdBytes = new Fmd[array.size()];
                for (int i = 0; i < array.size(); i++) {
                    //转回指纹FMD特征
                    try {
                        fmdBytes[i] = importer.ImportFmd(array.get(i), Fmd.Format.ANSI_378_2004, Fmd.Format.ANSI_378_2004);
                    } catch (UareUException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    Engine.Candidate[] results = m_engine.Identify(fmds, 0, fmdBytes, 100000, 2);
                    if (results.length != 0) {
                        m_score = m_engine.Compare(fmdBytes[results[0].fmd_index], 0, fmds, 0);
                    } else {
                        m_score = -1;
                    }
                    if (results.length > 0) {
                        mHandler.sendMessage(mHandler.obtainMessage(0, "有匹配：" + results[0].fmd_index));
                    } else {
                        mHandler.sendMessage(mHandler.obtainMessage(0, "无匹配："));

                    }
                    if (m_score != -1) {
                        mHandler.sendMessage(mHandler.obtainMessage(5, m_score));
                    }
                } catch (UareUException e) {
                    m_enginError = e.toString();
                    Log.w("UareUSampleJava", "Engine error: " + e.toString());
                }
            }
        } else {
            mHandler.sendMessage(mHandler.obtainMessage(0, "请指定指纹组的大小"));
        }
    }

    @Override
    public void comparisonFinger(byte[] bytes1, byte[] bytes2) {
        Fmd fmd1 = null;
        Fmd fmd2 = null;
        if (bytes1 != null && bytes2 != null) {
            //转回指纹FMD特征
            importer = new ImporterImpl();
            try {
                fmd1 = importer.ImportFmd(bytes1, Fmd.Format.ANSI_378_2004, Fmd.Format.ANSI_378_2004);
                fmd2 = importer.ImportFmd(bytes2, Fmd.Format.ANSI_378_2004, Fmd.Format.ANSI_378_2004);
            } catch (UareUException e) {
                e.printStackTrace();
            }

            try {
                if (fmd1 != null && fmd2 != null) {
                    m_score = m_engine.Compare(fmd1, 0, fmd2, 0);
                    mHandler.sendMessage(mHandler.obtainMessage(5, m_score));
                }
            } catch (Exception e) {
                Log.w(TAG, "Engine error: " + e.toString());
            }
        } else {
            mHandler.sendMessage(mHandler.obtainMessage(0, mContext.getString(R.string.template)));
            return;
        }

    }

    @Override
    public void comparisonFinger() {

    }

    @Override
    public int getImageQuality() {
        return 0;
    }

    @Override
    public boolean clearFinger() {

        return false;
    }

    @Override
    public void searchFinger() {

    }

    @Override
    public void unObject() {
        if (!deviceName.equals("")) {
            mContext.unregisterReceiver(mUsbReceiver);
            cancelReader();
        }
    }

    @Override
    public void createTemplate() {
        cancelReader();
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
                            mHandler.sendMessage(mHandler.obtainMessage(3, m_bitmap));
                            mFmd = m_engine.CreateFmd(cap_result.image, Fmd.Format.ANSI_378_2004);
                            if (mFmd != null) {
                                //将特征fmd转为特征byte
                                fmdbytes = mFmd.getData();
                                mHandler.sendMessage(mHandler.obtainMessage(4, fmdbytes));
                                m_reset = true;
                            }
                        } else {
                            mHandler.sendMessage(mHandler.obtainMessage(0, m_text_conclusionString));
                            continue;
                        }
                    }
                } catch (Exception e) {
                    if (!m_reset) {
                        cancelReader();
                        mHandler.sendMessage(mHandler.obtainMessage(0, mContext.getString(R.string.erro_captrue)));
                        Log.w(TAG, "error during capture: " + e.toString());
                    }
                }
            }
        }).start();
    }


    public void initReader() {
        try {
            m_reader = Globals.getInstance().getReader(deviceName, mContext);
            m_reader.Open(Reader.Priority.EXCLUSIVE);
            m_DPI = Globals.GetFirstDPI(m_reader);
            m_engine = UareUGlobal.GetEngine();
        } catch (Exception e) {
            Log.w(TAG, "error during init of reader");
            mHandler.sendMessage(mHandler.obtainMessage(0, mContext.getString(R.string.init_fingerReader)));
            return;
        }

    }

    private void openReaders() {
        try {
            readers = Globals.getInstance().getReaders(mContext);
//            deviceName = readers.get(0).GetDescription().name;
        } catch (UareUException e) {
            displayReaderNotFound();
        }
        int nSize = readers.size();
        if (nSize > 0) {
            for (int nCount = 0; nCount < nSize; nCount++) {
                deviceName = readers.get(nCount).GetDescription().name;
            }
            if ((deviceName != null) && !deviceName.isEmpty()) {
                Log.w(TAG, deviceName);
                try {
                    m_reader = Globals.getInstance().getReader(deviceName, mContext);
                    m_reader.Open(Reader.Priority.EXCLUSIVE);
                    m_DPI = Globals.GetFirstDPI(m_reader);
                    m_engine = UareUGlobal.GetEngine();
                    m_reader.Close();
                } catch (UareUException e) {
                    e.printStackTrace();
                    Log.w(TAG, "reader.open faild");
                    displayReaderNotFound();
                }
                try {
                    m_reader = Globals.getInstance().getReader(deviceName, mContext);
                    PendingIntent mPermissionIntent;
                    mPermissionIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
                    mContext.registerReceiver(mUsbReceiver, filter);
                    if (DPFPDDUsbHost.DPFPDDUsbCheckAndRequestPermissions(mContext, mPermissionIntent, deviceName)) {
                        CheckDevice();
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

    }


    public void CheckDevice() {
        mHandler.sendMessage(mHandler.obtainMessage(1, true));
    }

    private void displayReaderNotFound() {
        mHandler.sendMessage(mHandler.obtainMessage(1, false));
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
                        displayReaderNotFound();
                    }
                }
            }
        }
    };
}
