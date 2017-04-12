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
    ZAandroid a6 = null;
    private static int DEV_ADDR = 0xffffffff;
    private static int IMG_SIZE = 0;//同参数：（0:256x288 1:256x360）
    private int stada;

    public TCS1Realize(Context context, Handler handler) {
        mContext = context;
        this.handler = handler;
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
        return false;
    }

    @Override
    public void getImage() {
//        onBackPressed();
        removeCallbacks();
        handler.postDelayed(fpTasks, 0);
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                int nRet = 0;
//                reset=true;
//                while (reset){
//                    nRet = a6.ZAZGetImage(DEV_ADDR);
//                    if (nRet==0){
//                        int[] len = {0, 0};
//                        byte[] Image = new byte[256 * 360];
//                        a6.ZAZUpImage(DEV_ADDR, Image, len);//上传图像
//                        String str = "/mnt/sdcard/test.bmp";
//                        a6.ZAZImgData2BMP(Image, str);//图像数据保存
//                        Bitmap bmpDefaultPic;
//                        bmpDefaultPic = BitmapFactory.decodeFile(str, null);
//                        handler.sendMessage(handler.obtainMessage(10, bmpDefaultPic));
//                    }else if (nRet == a6.PS_NO_FINGER) {
//                        continue;
//                    } else if (nRet == a6.PS_GET_IMG_ERR) {
//                        continue;
//                    } else if (nRet == -2) {
//                        continue;
//                    } else {
//                        continue;
//                    }
//                }
//            }
//        }).start();
    }
    private boolean reset=true;
    public void onBackPressed() {
        try {
            reset = false;
        } catch (Exception e) {
            Log.w("UareUSampleJava", "error during reader shutdown");
        }
    }
    private Runnable getImageTask = new Runnable() {
        // 运行该服务执行此函数
        public void run() {
            int nRet = 0;
                nRet = a6.ZAZGetImage(DEV_ADDR);
                if (nRet==0){
                    int[] len = {0, 0};
                    byte[] Image = new byte[256 * 360];
                    a6.ZAZUpImage(DEV_ADDR, Image, len);//上传图像
                    String str = "/mnt/sdcard/test.bmp";
                    a6.ZAZImgData2BMP(Image, str);//图像数据保存
                    Bitmap bmpDefaultPic;
                    bmpDefaultPic = BitmapFactory.decodeFile(str, null);
                    handler.sendMessage(handler.obtainMessage(10, bmpDefaultPic));
                }else if (nRet == a6.PS_NO_FINGER) {
                    return;
                } else if (nRet == a6.PS_GET_IMG_ERR) {
                    return;
                } else if (nRet == -2) {

                    return;
                } else {
                    return;
                }
            }
    };

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
                a6.ZAZImgData2BMP(Image, str);
                Bitmap bmpDefaultPic;
                bmpDefaultPic = BitmapFactory.decodeFile(str, null);
                handler.sendMessage(handler.obtainMessage(10, bmpDefaultPic));
            } else if (nRet == a6.PS_NO_FINGER) {
                handler.postDelayed(fpTasks, 100);
            } else if (nRet == a6.PS_GET_IMG_ERR) {
                handler.postDelayed(fpTasks, 100);
                return;
            } else if (nRet == -2) {
            } else {
                return;
            }

        }
    };
    @Override
    public void createTemplate() {
//        removeCallbacks();
        onBackPressed();
//        handler.postDelayed(Characteristic, 0);
        new  Thread(new Runnable() {
            @Override
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
                        handler.sendMessage(handler.obtainMessage(11, pTemplet));
                    } else {
                        handler.postDelayed(Characteristic, 1000);

                    }
                } else if (nRet == a6.PS_NO_FINGER) {
                    handler.postDelayed(Characteristic, 10);
                } else if (nRet == a6.PS_GET_IMG_ERR) {
                    handler.postDelayed(Characteristic, 10);
                    return;
                } else if (nRet == -2) {//数据包接受错误
                } else {//接受异常
                    return;
                }

            }
        }).start();
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
                    handler.sendMessage(handler.obtainMessage(11, pTemplet));
                } else {
                    handler.postDelayed(Characteristic, 1000);

                }
            } else if (nRet == a6.PS_NO_FINGER) {
                handler.postDelayed(Characteristic, 10);
            } else if (nRet == a6.PS_GET_IMG_ERR) {
                handler.postDelayed(Characteristic, 10);
                return;
            } else if (nRet == -2) {//数据包接受错误
            } else {//接受异常
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

    @Override
    public void getImageQuality() {

    }

    @Override
    public void clearFinger() {

    }
}
