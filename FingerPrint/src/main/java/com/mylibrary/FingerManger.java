package com.mylibrary;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;

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

    public static IFingerPrint getIFingerPrintIntance(Context context, Activity activity, Handler handler) {
        mContext = context;
        mActivity = activity;
        int state = FingerTypes.getrwusbdevices(mContext);
        if (iFingerPrint == null) {
            switch (state) {
                case 0:
                    iFingerPrint = null;
                    break;
                case 1:
                    iFingerPrint = new TCS1BRealize(mContext, mActivity, handler);
                    break;
                case 2:
                    iFingerPrint=new TCS1Realize(mContext,handler);
                    break;
                case 3:
                    iFingerPrint = new TCS1GRealize(mContext, handler);
                    break;

            }
        }
        return iFingerPrint;
    }

}
