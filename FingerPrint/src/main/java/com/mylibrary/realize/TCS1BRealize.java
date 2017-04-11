package com.mylibrary.realize;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.IDWORLD.LAPI;
import com.digitalpersona.uareu.Fmd;
import com.mylibrary.inf.IFingerPrint;

/**
 * Created by suntianwei on 2017/4/6.
 */

public class TCS1BRealize implements IFingerPrint {
    private Context mContext;
    private Activity mActivity;
    private Handler handler;
    private int m_hDevice;
    private LAPI mLapi;
    private byte[] m_image = new byte[LAPI.WIDTH * LAPI.HEIGHT];
    private byte[] mItemplate;
    private int qualitys;

    public TCS1BRealize(Context context, Activity activity, Handler handler) {
        mContext = context;
        mActivity = activity;
        this.handler = handler;
    }

    @Override
    public boolean openReader() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mLapi = new LAPI(mActivity);
                m_hDevice = mLapi.OpenDeviceEx();

            }
        }).start();
        if (m_hDevice == LAPI.FALSE) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean closeReader() {
        if (m_hDevice == 0) {
            return false;
        } else {
            mLapi.CloseDeviceEx(m_hDevice);
            m_hDevice = 0;
            return true;
        }
    }

    @Override
    public void getImage() {
        int ret;
        ret = mLapi.GetImage(m_hDevice, m_image);
        if (ret == LAPI.TRUE) {
            handler.sendMessage(handler.obtainMessage(1, LAPI.WIDTH, LAPI.HEIGHT, m_image));
        } else {
//            msg = "Can't get image !";
        }
    }

    @Override
    public void createTemplate() {
        int ret;
        ret = mLapi.GetImage(m_hDevice, m_image);
        if (ret == LAPI.TRUE) {
            handler.sendMessage(handler.obtainMessage(1, LAPI.WIDTH, LAPI.HEIGHT, m_image));
        } else {
//            msg = "Can't get image !";
        }
        String msg = "";
        getImageQualitys();
        if (qualitys > 50) {
            ret = mLapi.IsPressFinger(m_hDevice, m_image);//判断手指是否在指纹模板上、返回值0-100
            if (ret != 0) {
                mItemplate = new byte[LAPI.FPINFO_STD_MAX_SIZE];
                ret = mLapi.CreateTemplate(m_hDevice, m_image, mItemplate);
                if (ret == 0) {
                    msg = "Can't create template !";
                } else {
                    for (int i = 0; i < LAPI.FPINFO_STD_MAX_SIZE; i++) {
                        msg += String.format("%02x", mItemplate[i]);
                    }
                    Log.e("finger", "senMessage: "  + msg);
                    handler.sendMessage(handler.obtainMessage(3, mItemplate));
                }
            }
        } else {
//            handler.sendMessage(handler.obtainMessage(9, mItemplate));
        }
    }

    @Override
    public void enrollment() {

    }

    @Override
    public void comparisonFinger(Fmd fmd1, Fmd fmd2) {

    }

    @Override
    public void comparisonFinger(byte[] bytes1, byte[] bytes2) {
        int score;
        if (bytes1 != null && bytes2 != null) {
            score = mLapi.CompareTemplates(m_hDevice, bytes1, bytes2);//返回指纹模板对比分数
            handler.sendMessage(handler.obtainMessage(4, score));
        }

    }

    @Override
    public void getImageQuality() {
        getImageQualitys();
    }

    private void getImageQualitys() {
        qualitys = mLapi.GetImageQuality(m_hDevice, m_image);
        handler.sendMessage(handler.obtainMessage(2, qualitys));
    }

    @Override
    public void clearFinger() {

    }
}
