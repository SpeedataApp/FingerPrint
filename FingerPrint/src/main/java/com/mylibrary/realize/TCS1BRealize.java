package com.mylibrary.realize;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;

import com.IDWORLD.LAPI;
import com.digitalpersona.uareu.Fmd;
import com.mylibrary.R;
import com.mylibrary.inf.IFingerPrint;

/**
 * Created by suntianwei on 2017/4/6.
 */

public class TCS1BRealize implements IFingerPrint {
    private String TAG = "Finger_TCS1B";
    private Context mContext;
    private Activity mActivity;
    private int m_hDevice;
    private LAPI mLapi;
    private byte[] m_image = new byte[LAPI.WIDTH * LAPI.HEIGHT];
    private byte[] mItemplate = null;
    private String msg = "";
    private Handler handler;


    public TCS1BRealize(Context context, Activity activity, Handler handler) {
        mContext = context;
        mActivity = activity;
        this.handler = handler;
        mLapi = new LAPI(mActivity);
    }

    @Override
    public void openReader() {
        Log.i(TAG, m_hDevice + "open");
        new Thread(new Runnable() {
            @Override
            public void run() {
                m_hDevice = mLapi.OpenDeviceEx();
                if (m_hDevice != 0) {
                    handler.sendMessage(handler.obtainMessage(1, true));
                } else {
                    handler.sendMessage(handler.obtainMessage(1, false));
                }
            }
        }).start();
    }

    @Override
    public void closeReader() {
        Log.i(TAG, m_hDevice + "close");
        if (mLapi.CloseDeviceEx(m_hDevice) == 1) {//关闭成功
            handler.sendMessage(handler.obtainMessage(2, true));
        } else if (mLapi.CloseDeviceEx(m_hDevice) == 0) {
            handler.sendMessage(handler.obtainMessage(2, false));
        }
    }

    @Override
    public void getImage() {
        Bitmap bitmap = null;
        int ret;
        ret = mLapi.GetImage(m_hDevice, m_image);
        if (ret == LAPI.TRUE) {
            Log.i(TAG, "getImage: success");
            bitmap = ShowFingerBitmap(m_image, LAPI.WIDTH, LAPI.HEIGHT);
            handler.sendMessage(handler.obtainMessage(3, bitmap));
        } else {
            handler.sendMessage(handler.obtainMessage(3, bitmap));
        }
    }


    private Bitmap ShowFingerBitmap(byte[] image, int width, int height) {
        if (width == 0) {
            return null;
        }
        if (height == 0) {
            return null;
        }
        int[] RGBbits = new int[width * height];
        for (int i = 0; i < width * height; i++) {
            int v;
            if (image != null) v = image[i] & 0xff;
            else v = 0;
            RGBbits[i] = Color.rgb(v, v, v);
        }
        return Bitmap.createBitmap(RGBbits, width, height, Bitmap.Config.RGB_565);
    }

    @Override
    public void createTemplate() {
        int ret;
        ret = mLapi.GetImage(m_hDevice, m_image);
        if (ret == LAPI.TRUE) {
            handler.sendMessage(handler.obtainMessage(3, ShowFingerBitmap(m_image, LAPI.WIDTH, LAPI.HEIGHT)));
            if (getImageQualitys() > 30) {
                ret = mLapi.IsPressFinger(m_hDevice, m_image);//判断手指是否在指纹模板上、返回值0-100
                if (ret != 0) {
                    mItemplate = new byte[LAPI.FPINFO_STD_MAX_SIZE];
                    ret = mLapi.CreateTemplate(m_hDevice, m_image, mItemplate);
                    if (ret == 0) {
                        msg = "Can't create template !";
                        Log.i(TAG, msg);
                        handler.sendMessage(handler.obtainMessage(0, mContext.getString(R.string.template_fail)));
                    } else {
                        msg = "";
//                        for (int i = 0; i < LAPI.FPINFO_STD_MAX_SIZE; i++) {
//                            msg += String.format("%02x", mItemplate[i]);
//                        }
                        Log.e("finger", "senMessage: " + msg);
                        handler.sendMessage(handler.obtainMessage(4, mItemplate));
                    }
                } else {
                    msg = "No finger on reader !";
                    Log.i(TAG, msg);
                    handler.sendMessage(handler.obtainMessage(0, mContext.getString(R.string.on_reader_nofinger)));
                }

            } else {
                handler.sendMessage(handler.obtainMessage(0, mContext.getString(R.string.on_reader_nofinger)));
            }
        }
    }


    @Override
    public void enrollment() {

    }

    @Override
    public void comparisonFinger(Fmd fmd1, Fmd fmd2) {

    }

    @Override
    public void comparisonFinger() {

    }

    @Override
    public void comparisonFinger(byte[] bytes1, byte[] bytes2) {
        int score = 0;
        if (bytes1 != null && bytes2 != null) {
            score = mLapi.CompareTemplates(m_hDevice, bytes1, bytes2);//返回指纹模板对比分数
            Log.i(TAG, "comparisonFinger: " + score);
            handler.sendMessage(handler.obtainMessage(6, score));
        } else {
            msg = "ComparrisonFinger failed 请获取特征!";
            Log.i(TAG, "comparisonFinger: " + msg);
            handler.sendMessage(handler.obtainMessage(0, mContext.getString(R.string.ComparrisonFinger_failed)));
        }

    }

    @Override
    public int getImageQuality() {
        return getImageQualitys();
    }

    private int getImageQualitys() {
        return mLapi.GetImageQuality(m_hDevice, m_image);
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
    }

}
