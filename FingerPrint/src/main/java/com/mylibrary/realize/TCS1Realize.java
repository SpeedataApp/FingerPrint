package com.mylibrary.realize;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import com.digitalpersona.uareu.Fmd;
import com.mylibrary.inf.IFingerPrint;
import com.mylibrary.ulits.OpenDevice;
import com.za.finger.ZAandroid;

/**
 * Created by suntianwei on 2017/4/10.
 */

public class TCS1Realize implements IFingerPrint {
    Context mContext;
    Handler handler;
    Handler handlers;
    ZAandroid a6 = null;
    private static int DEV_ADDR = 0xffffffff;
    private static int IMG_SIZE = 0;//同参数：（0:256x288 1:256x360）
    private int stada;
    private String TAG = "Tcs1";

    public TCS1Realize(Context context, Handler handler) {
        mContext = context;
        this.handler = handler;
//        handlers = new Handler();
    }

    @Override
    public boolean openReader() {
        a6 = new ZAandroid();
        final OpenDevice openDevice = new OpenDevice();
        Runnable r = new Runnable() {
            public void run() {
                stada = openDevice.OpenDev(mContext, a6, DEV_ADDR, IMG_SIZE, 0);

            }
        };
        Thread s = new Thread(r);
        s.start();
        if (stada == 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean closeReader() {
        int status = 2;
        if (stada == 0) {
            status = a6.ZAZCloseDeviceEx();
            Log.e(TAG, " close status: " + status);
        }

        if (status == 0) {
            return true;
        } else {
            return false;
        }

    }

    @Override
    public void getImage() {
        removeCallbacks();
        handlers.postDelayed(fpTasks, 0);
    }

    private void removeCallbacks() {
        handler.removeCallbacks(Characteristic);
        handler.removeCallbacks(fpTasks);
    }

    private Runnable fpTasks = new Runnable() {
        public void run()// 运行该服务执行此函数
        {
            int nRet = 0;
            nRet = a6.ZAZGetImage(DEV_ADDR);
            if (nRet == 0) {
                int[] len = {0, 0};
                byte[] Image = new byte[256 * 360];
                a6.ZAZUpImage(DEV_ADDR, Image, len);
                String str = "/mnt/sdcard/test.bmp";
                int iii = a6.ZAZImgData2BMP(Image, str);
                Bitmap bmpDefaultPic = BitmapFactory.decodeFile(str);
                handler.sendMessage(handler.obtainMessage(10, bmpDefaultPic));
            } else if (nRet == a6.PS_NO_FINGER) {
                handlers.postDelayed(fpTasks, 100);
            } else if (nRet == a6.PS_GET_IMG_ERR) {
                handlers.postDelayed(fpTasks, 100);
                return;
            } else if (nRet == -2) {
            } else {
                return;
            }

        }
    };

    @Override
    public void createTemplate() {
        removeCallbacks();
        handler.postDelayed(Characteristic, 0);
    }

    byte[] pTemplet = null;
    private Runnable Characteristic = new Runnable() {
        // 运行该服务执行此函数
        public void run() {
            int nRet = 0;
            nRet = a6.ZAZGetImage(DEV_ADDR);
            if (nRet == 0) {
                SystemClock.sleep(200);
                nRet = a6.ZAZGetImage(DEV_ADDR);
            }
            if (nRet == 0) {
                nRet = a6.ZAZGenChar(DEV_ADDR, a6.CHAR_BUFFER_A);// != PS_OK) {
                if (nRet == a6.PS_OK) {
                    int[] iTempletLength = {0, 0};
                    pTemplet = new byte[512];
                    a6.ZAZSetCharLen(512);
                    nRet = a6.ZAZUpChar(DEV_ADDR, a6.CHAR_BUFFER_A, pTemplet, iTempletLength);
                    if (nRet == a6.PS_OK) {
                        handler.sendMessage(handler.obtainMessage(11, pTemplet));
                    }
                    nRet = a6.ZAZDownChar(DEV_ADDR, a6.CHAR_BUFFER_A, pTemplet, iTempletLength[0]);
                    if (nRet == a6.PS_OK) {
                    }
                } else {
                    handler.postDelayed(Characteristic, 1000);

                }
            } else if (nRet == a6.PS_NO_FINGER) {
                handler.postDelayed(Characteristic, 10);
            } else if (nRet == a6.PS_GET_IMG_ERR) {
                handler.postDelayed(Characteristic, 10);
                return;
            } else if (nRet == -2) {

                return;
            } else {
                return;
            }

        }
    };

    @Override
    public void enrollment() {

    }

    @Override
    public void comparisonFinger(Fmd fmd1, Fmd fmd2) {

    }

    @Override
    public void comparisonFinger(byte[] bytes1, byte[] bytes2) {

    }
  int fpcharbuf=1;
//    private Runnable ComparisonTasks = new Runnable() {
//        // 运行该服务执行此函数
//        public void run() {
//            String temp = "";
//            long timecount = 0;
//            int nRet = 0;
//            nRet = a6.ZAZGetImage(DEV_ADDR);
//            if (nRet == 0) {
//                nRet = a6.ZAZGetImage(DEV_ADDR);
//            }
//            if (nRet == 0) {
//
//                //nRet = a6.ZAZLoadChar( DEV_ADDR,2,1);
//                //a6.ZAZSetCharLen(2304);
//                //nRet = a6.ZAZDownChar(DEV_ADDR, 2, pTempletbase, 2304);
//                nRet = a6.ZAZGenChar(DEV_ADDR, fpcharbuf);// != PS_OK) {
//                if (nRet == a6.PS_OK) {
//                    if (fpcharbuf != 1) {
//                        int[] iScore = {0, 0};
//                        nRet = a6.ZAZMatch(DEV_ADDR, iScore);
//                        if (nRet == a6.PS_OK) {
//                            temp = getResources().getString(R.string.matchsuccess_str) + iScore[0];
//                            mtvMessage.setText(temp);
//                        } else {
//                            temp = getResources().getString(R.string.matchfail_str) + iScore[0];
//                            mtvMessage.setText(temp);
//                        }
//                        return;
//                    }
//
//                } else {
//
//                }
//
//            } else if (nRet == a6.PS_NO_FINGER) {
//            } else if (nRet == a6.PS_GET_IMG_ERR) {
//                return;
//            } else if (nRet == -2) {
//            } else {
//                return;
//            }
//        }
//    };

    @Override
    public void getImageQuality() {

    }

    @Override
    public boolean clearFinger() {
        int Rnet = a6.ZAZEmpty(DEV_ADDR);
        return true;

    }

    @Override
    public void unObject() {

    }
}
