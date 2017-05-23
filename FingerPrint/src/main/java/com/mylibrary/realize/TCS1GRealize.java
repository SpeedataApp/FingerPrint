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
import com.mylibrary.inf.MsgCallBack;
import com.mylibrary.ulits.Data;
import com.mylibrary.ulits.Globals;

/**
 * Created by suntianwei on 2017/4/6.
 */

public class TCS1GRealize implements IFingerPrint {
    private String TAG = "Finger_TCS1G";
    private MsgCallBack callBack;
    private Data data;
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

    public TCS1GRealize(Context context, Activity activity, MsgCallBack callBack) {
        mContext = context;
        mActivity = activity;
        this.callBack = callBack;
        data = new Data();
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
            data.setOpenFlag(true);
            data.setInfoMsg(mContext.getString(R.string.close_fail));
            callBack.callBackInfo(data);
        }
        data.setOpenFlag(false);
        data.setInfoMsg(mContext.getString(R.string.close_success));
        callBack.callBackInfo(data);
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
                            data.setFingerBitmap(m_bitmap);
                            data.setInfoMsg(mContext.getString(R.string.get_image_success));
                            callBack.callBackInfo(data);
                        } else {
                            data.setInfoMsg(m_text_conclusionString);
                            callBack.callBackInfo(data);
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
        data.setInfoMsg(mContext.getString(R.string.any_finger));
        callBack.callBackInfo(data);
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
                        m_textString = m_success ? "Enrollment template created, size: " + m_templateSize : "Enrollment template failed. Please try again";
                        data.setInfoMsg(m_textString);
                    }
                }
//                m_textString = "Place any finger on the reader";
                m_enrollment_fmd = null;
            } else {
                m_first = false;
                m_success = false;
                data.setInfoMsg(mContext.getString(R.string.same_finger));
            }

            //跟新主线程
            data.setFingerBitmap(m_bitmap);
            callBack.callBackInfo(data);
            return result;
        }

    }

    private void resetData() {
        data.setTemplateBytes(null);
        data.setInfoMsg("");
        data.setFingerBitmap(null);
    }

    @Override
    public void comparisonFinger(final Fmd fmd1, final Fmd fmd2) {
        onBackPressed();
        data.setTcs1gFmd(null);
        // loop capture on a separate thread to avoid freezing the UI
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
        try {
            if (fmd1 != null && fmd2 != null) {

                m_score = m_engine.Compare(fmd1, 0, fmd2, 0);
            } else {
                data.setInfoMsg("请创建模板");
                callBack.callBackInfo(data);
                return;
            }
        } catch (Exception e) {
            Log.w(TAG, "Engine error: " + e.toString());
        }
        data.setComparisonNum(m_score);
        callBack.callBackInfo(data);
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
    public void getImageQuality() {
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
        if (data != null) {
            data = null;
        }

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
//                            handler.sendMessage(handler.obtainMessage(5, m_bitmap));
                            data.setFingerBitmap(m_bitmap);
                            data.setInfoMsg(mContext.getString(R.string.get_image_success));
                            mFmd = m_engine.CreateFmd(cap_result.image, Fmd.Format.ANSI_378_2004);
                            if (mFmd != null) {
//                                handler.sendMessage(handler.obtainMessage(6, mFmd));
                                data.setTcs1gFmd(mFmd);
                                data.setInfoMsg(mContext.getString(R.string.template_success));
                                callBack.callBackInfo(data);
                                m_reset = true;
                            }
                        } else {
                            data.setInfoMsg(m_text_conclusionString);
                            callBack.callBackInfo(data);
                            continue;
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
            data.setInfoMsg(mContext.getString(R.string.init_fingerReader));
            callBack.callBackInfo(data);
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
        data.setOpenFlag(true);
        data.setInfoMsg(mContext.getString(R.string.opne_success));
        callBack.callBackInfo(data);
    }

    private void displayReaderNotFound() {
        data.setOpenFlag(false);
        data.setInfoMsg(mContext.getString(R.string.opne_fail));
        callBack.callBackInfo(data);
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
