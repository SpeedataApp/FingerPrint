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
import com.mylibrary.R;
import com.mylibrary.inf.IFingerPrint;
import com.mylibrary.ulits.Globals;

/**
 * Created by suntianwei on 2017/4/6.
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
    private Fmd m_enrollment_fmd = null;
    private boolean m_success = false;
    private int m_templateSize = 0;
    private int m_current_fmds_count = 0;
    private String m_enginError;
    private boolean m_first = true;
    private String m_textString;
    private int m_score = -1;
    Handler mHandler;

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
                            mHandler.sendMessage(mHandler.obtainMessage(3, m_bitmap));

                        } else {
                            mHandler.sendMessage(mHandler.obtainMessage(0, m_text_conclusionString));
                        }
                    }
                } catch (Exception e) {
                    if (!m_reset) {
                        Log.w(TAG, "error during capture: " + e.toString());
                        onBackPressed();
                    }
                }
            }
        }).start();
    }

    @Override
    public void enrollment() {
        onBackPressed();
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
                            m_enrollment_fmd = m_engine.CreateEnrollmentFmd(Fmd.Format.ANSI_378_2004, enrollThread);
                            if (m_success = (m_enrollment_fmd != null)) {
                                //获取Fmd的size
                                // 返回FMD的完整二进制数据，包括记录头和所有视图
                                m_templateSize = m_enrollment_fmd.getData().length;
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
                    onBackPressed();
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

            if (m_enrollment_fmd != null || m_current_fmds_count == 0) {
                if (!m_first) {
                    if (m_text_conclusionString.length() == 0) {
                        if (m_success) {
                            mHandler.sendMessage(mHandler.obtainMessage(7, m_templateSize));
                            mHandler.sendMessage(mHandler.obtainMessage(5, m_enrollment_fmd));
                            mHandler.sendMessage(mHandler.obtainMessage(0, mContext.getString(R.string.enrollment_success)));
                            m_reset = true;
                        } else {
                            mHandler.sendMessage(mHandler.obtainMessage(0, mContext.getString(R.string.same_finger)));
                        }
                    }
                }
//                m_textString = "Place any finger on the reader";
                m_enrollment_fmd = null;
            } else {
                m_first = false;
                m_success = false;
//                mHandler.sendMessage(mHandler.obtainMessage(0, mContext.getString(R.string.same_finger)));
            }
            return result;
        }

    }


    @Override
    public void comparisonFinger(final Fmd fmd1, final Fmd fmd2) {
        onBackPressed();
        // loop capture on a separate thread to avoid freezing the UI
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
        try {
            if (fmd1 != null && fmd2 != null) {

                m_score = m_engine.Compare(fmd1, 0, fmd2, 0);
                mHandler.sendMessage(mHandler.obtainMessage(6, m_score));
            } else {

                mHandler.sendMessage(mHandler.obtainMessage(0, mContext.getString(R.string.template)));
                return;
            }
        } catch (Exception e) {
            Log.w(TAG, "Engine error: " + e.toString());
        }
//            }
//        }).start();

    }

    @Override
    public void comparisonFinger(Fmd[] fmdBytes) {
// loop capture on a separate thread to avoid freezing the UI
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                m_reset = false;
//                while (!m_reset) {
//                    try {
//                        cap_result = m_reader.Capture(Fid.Format.ANSI_381_2004, Globals.DefaultImageProcessing, m_DPI, -1);
//                    } catch (Exception e) {
//                        if (!m_reset) {
//                            Log.w("UareUSampleJava", "error during capture: " + e.toString());
//                            onBackPressed();
//                        }
//                    }
//
//                    // an error occurred
//                    if (cap_result == null || cap_result.image == null) continue;
//
//                    try {
//                        m_enginError = "";
//                        // save bitmap image locally
//                        m_bitmap = Globals.GetBitmapFromRaw(cap_result.image.getViews()[0].getImageData(), cap_result.image.getViews()[0].getWidth(), cap_result.image.getViews()[0].getHeight());
//                            Fmd m_temp = m_engine.CreateFmd(cap_result.image, Fmd.Format.ANSI_378_2004);
//                            Fmd[] m_fmds_temp = new Fmd[]{m_fmd1, m_fmd2, m_fmd3, m_fmd4};
//                            results = m_engine.Identify(m_temp, 0, m_fmds_temp, 100000, 2);
//
//                            if (results.length != 0) {
//                                m_score = m_engine.Compare(m_fmds_temp[results[0].fmd_index], 0, m_temp, 0);
//                            } else {
//                                m_score = -1;
//                            }
//                            m_fmd1 = null;
//                            m_fmd2 = null;
//                            m_fmd3 = null;
//                            m_fmd4 = null;
//                    } catch (Exception e) {
//                        m_enginError = e.toString();
//                        Log.w("UareUSampleJava", "Engine error: " + e.toString());
//                    }
//
//                    m_text_conclusionString = Globals.QualityToString(cap_result);
//
//                    if (!m_enginError.isEmpty()) {
//                        m_text_conclusionString = "Engine: " + m_enginError;
//                    }
//                    if (m_fmd1 == null) {
//                        if (!m_first) {
//                            if (m_text_conclusionString.length() == 0) {
//                                String conclusion = "";
//                                if (results.length > 0) {
//                                    switch (results[0].fmd_index) {
//                                        case 0:
//                                            conclusion = "Thumb matched";
//                                            break;
//                                        case 1:
//                                            conclusion = "Index finger matched";
//                                            break;
//                                        case 2:
//                                            conclusion = "Middle finger matched";
//                                            break;
//                                        case 3:
//                                            conclusion = "Ring finger matched";
//                                            break;
//                                    }
//                                } else {
//                                    conclusion = "No match found";
//                                }
//                                m_text_conclusionString = conclusion;
//                                if (m_score != -1) {
//                                    DecimalFormat formatting = new DecimalFormat("##.######");
//                                    m_text_conclusionString = m_text_conclusionString + " (Dissimilarity Score: " + String.valueOf(m_score) + ", False match rate: " + Double.valueOf(formatting.format((double) m_score / 0x7FFFFFFF)) + ")";
//                                }
//                            }
//                        }
//
//                        m_textString = "Place your thumb on the reader";
//                    } else if (m_fmd2 == null) {
//                        m_first = false;
//                        m_textString = "Place your index finger on the reader";
//                    } else if (m_fmd3 == null) {
//                        m_first = false;
//                        m_textString = "Place your middle finger on the reader";
//                    } else if (m_fmd4 == null) {
//                        m_first = false;
//                        m_textString = "Place your ring finger on the reader";
//                    } else {
//                        m_textString = "Place any finger on the reader";
//                    }
//
//
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            UpdateGUI();
//                        }
//                    });
//                }
//            }
//        }).start();
    }

    @Override
    public void comparisonFinger(byte[] bytes1, byte[] bytes2) {

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
                            mHandler.sendMessage(mHandler.obtainMessage(3, m_bitmap));
                            mFmd = m_engine.CreateFmd(cap_result.image, Fmd.Format.ANSI_378_2004);
                            if (mFmd != null) {
                                mHandler.sendMessage(mHandler.obtainMessage(5, mFmd));
                                m_reset = true;
                            }
                        } else {
                            mHandler.sendMessage(mHandler.obtainMessage(0, m_text_conclusionString));
                            continue;
                        }
                    }
                } catch (Exception e) {
                    if (!m_reset) {
                        Log.w(TAG, "error during capture: " + e.toString());
//                        onBackPressed();
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
            Log.w(TAG, "error during reader shutdown");
        }
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
