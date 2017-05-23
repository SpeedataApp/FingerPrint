package com.mylibrary.realize;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import com.IDWORLD.LAPI;
import com.digitalpersona.uareu.Fmd;
import com.mylibrary.R;
import com.mylibrary.inf.IFingerPrint;
import com.mylibrary.inf.MsgCallBack;
import com.mylibrary.ulits.Data;

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
    private byte[] mItemplate;
    private int qualitys;
    private String msg = "";
    private MsgCallBack callBack;
    private Data data;
    private Bitmap bmp;

    public TCS1BRealize(Context context, Activity activity, MsgCallBack callBack) {
        mContext = context;
        mActivity = activity;
        this.callBack = callBack;
        mLapi = new LAPI(mActivity);
        data = new Data();
    }

    @Override
    public void openReader() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                m_hDevice = mLapi.OpenDeviceEx();
                if (m_hDevice != 0) {
                    data.setOpenFlag(true);
                    data.setInfoMsg(mContext.getString(R.string.opne_success));
                    callBack.callBackInfo(data);
                } else {
                    data.setOpenFlag(false);
                    data.setInfoMsg(mContext.getString(R.string.opne_fail));
                    callBack.callBackInfo(data);
                }
                Log.i(TAG, m_hDevice + "open");

            }
        }).start();
    }

    @Override
    public void closeReader() {
        m_hDevice = mLapi.CloseDeviceEx(m_hDevice);
        Log.i(TAG, m_hDevice + "close");
        if (m_hDevice == 1) {//关闭成功
            data.setOpenFlag(false);
            data.setInfoMsg(mContext.getString(R.string.close_success));
            callBack.callBackInfo(data);
        } else if (m_hDevice == 0) {
            data.setOpenFlag(true);
            data.setInfoMsg(mContext.getString(R.string.opne_fail));
            callBack.callBackInfo(data);
        }
    }

    @Override
    public void getImage() {
        resetData();
        int ret;
        ret = mLapi.GetImage(m_hDevice, m_image);
        if (ret == LAPI.TRUE) {
            Log.i(TAG, "getImage: success");
            data.setFingerBitmap(ShowFingerBitmap(m_image, LAPI.WIDTH, LAPI.HEIGHT));
            data.setInfoMsg(mContext.getString(R.string.get_image_success));
            callBack.callBackInfo(data);
        } else {
            Log.i(TAG, "Can't get image ! ");
            data.setInfoMsg(mContext.getString(R.string.get_image_fail));
            callBack.callBackInfo(data);
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
            data.setFingerBitmap(ShowFingerBitmap(m_image, LAPI.WIDTH, LAPI.HEIGHT));
            data.setInfoMsg(mContext.getString(R.string.get_image_success));
            qualitys = mLapi.GetImageQuality(m_hDevice, m_image);
            data.setFinferQualitys(qualitys);
            if (qualitys > 30) {
                ret = mLapi.IsPressFinger(m_hDevice, m_image);//判断手指是否在指纹模板上、返回值0-100
                if (ret != 0) {
                    mItemplate = new byte[LAPI.FPINFO_STD_MAX_SIZE];
                    ret = mLapi.CreateTemplate(m_hDevice, m_image, mItemplate);
                    if (ret == 0) {
                        msg = "Can't create template !";
                        Log.i(TAG, msg);
                        data.setInfoMsg(mContext.getString(R.string.template_fail));
                        callBack.callBackInfo(data);
                    } else {
                        msg = "";
                        for (int i = 0; i < LAPI.FPINFO_STD_MAX_SIZE; i++) {
                            msg += String.format("%02x", mItemplate[i]);
                        }
                        Log.e("finger", "senMessage: " + msg);

                        data.setInfoMsg(mContext.getString(R.string.template_success));
                        data.setTemplateBytes(mItemplate);
                        callBack.callBackInfo(data);
                    }
                } else {
                    msg = "No finger on reader !";
                    Log.i(TAG, msg);
                    data.setInfoMsg(mContext.getString(R.string.on_reader_nofinger));
                    callBack.callBackInfo(data);
                }

            } else {
                data.setInfoMsg(mContext.getString(R.string.quality_bad));
                data.setTemplateBytes(null);
                callBack.callBackInfo(data);
            }
        }

    }

    private void resetData() {
        data.setTemplateBytes(null);
        data.setInfoMsg("");
//        data.setFingerBitmap(null);
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
        int score;
//        data.setTemplateBytes(null);
        resetData();
        if (bytes1 != null && bytes2 != null) {
            score = mLapi.CompareTemplates(m_hDevice, bytes1, bytes2);//返回指纹模板对比分数
            Log.i(TAG, "comparisonFinger: " + score);
            data.setComparisonNum(score);
            callBack.callBackInfo(data);
        } else {
            msg = "ComparrisonFinger failed 请获取特征!";
            Log.i(TAG, "comparisonFinger: " + msg);
            data.setInfoMsg(mContext.getString(R.string.ComparrisonFinger_failed));
            callBack.callBackInfo(data);
//            handler.sendMessage(handler.obtainMessage(0, msg));
        }

    }

    @Override
    public void getImageQuality() {
        getImageQualitys();
    }

    private void getImageQualitys() {
        qualitys = mLapi.GetImageQuality(m_hDevice, m_image);
        data.setFinferQualitys(qualitys);
        callBack.callBackInfo(data);
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
    }


}
