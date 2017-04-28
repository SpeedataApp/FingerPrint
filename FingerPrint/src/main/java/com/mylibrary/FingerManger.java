package com.mylibrary;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.mylibrary.inf.IFingerPrint;
import com.mylibrary.realize.TCS1BRealize;
import com.mylibrary.realize.TCS1GRealize;
import com.mylibrary.realize.TCS1Realize;
import com.mylibrary.ulits.FingerTypes;

/**
 * Created by suntianwei on 2017/4/6.
 */

public class FingerManger {
    public static IFingerPrint iFingerPrint = null;
    static Context mContext;
    static Activity mActivity;
    static Handler mHandler;
    static String TAG = "manager";
    private static int state = 0;

    public static IFingerPrint getIFingerPrintIntance(Context context, Activity activity, Handler handler) {
        mContext = context;
        mActivity = activity;
        mHandler = handler;
        state = FingerTypes.getrwusbdevices(mContext);
        if (iFingerPrint == null) {
            switch (state) {
                case 0:
                    iFingerPrint = null;
                    break;
                case 1:
                    iFingerPrint = new TCS1BRealize(mContext, mActivity, mHandler);
                    Log.e(TAG, "tcs1b");
                    break;
                case 2:
                    iFingerPrint = new TCS1Realize(mContext, mHandler);
                    Log.e(TAG, "tcs1");
                    break;
                case 3:
                    iFingerPrint = new TCS1GRealize(mContext, mActivity, mHandler);
                    Log.e(TAG, "tcs1g");
                    break;
            }
        }
        return iFingerPrint;
    }

}
