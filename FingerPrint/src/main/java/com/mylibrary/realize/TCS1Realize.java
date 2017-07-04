package com.mylibrary.realize;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;

import com.mylibrary.R;
import com.mylibrary.inf.IFingerPrint;
import com.mylibrary.ulits.OpenDevice;
import com.za.finger.ZAandroid;

import java.util.ArrayList;

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
    private int fpcharbuf = 1;
    private String TAG = "Tcs1";
    private Thread thread;

    public TCS1Realize(Context context, Handler handler) {
        mContext = context;
        this.handler = handler;
    }

    @Override
    public void openReader() {
        a6 = new ZAandroid();
        final OpenDevice openDevice = new OpenDevice();
//        handler.postDelayed(r,0);
        Runnable r = new Runnable() {
            public void run() {
                stada = openDevice.OpenDev(mContext, a6, DEV_ADDR, IMG_SIZE, 0);
                if (stada == 0) {
                    handler.sendMessage(handler.obtainMessage(1, true));
                } else {
                    handler.sendMessage(handler.obtainMessage(1, false));
                }

            }
        };
        thread = new Thread(r);
        thread.start();
    }

    @Override
    public void closeReader() {
        int status = a6.ZAZCloseDeviceEx();
        Log.e(TAG, " close status: " + status);
        if (stada == 0) {
            thread.interrupt();
            thread = null;
            removeCallbacks();
            handler.sendMessage(handler.obtainMessage(2, true));
        } else {
            removeCallbacks();
            handler.sendMessage(handler.obtainMessage(2, false));
        }
    }

    @Override
    public void getImage() {
        removeCallbacks();
        handler.postDelayed(getFpTasks, 0);
    }

    private void removeCallbacks() {
        handler.removeCallbacks(templateTask);
        handler.removeCallbacks(getFpTasks);
        handler.removeCallbacks(enrollmentTasks);
        handler.removeCallbacks(ComparisonTasks);
        handler.removeCallbacks(searchTasks);
    }

    private Runnable getFpTasks = new Runnable() {
        public void run() {
            int nRet = 0;
            nRet = a6.ZAZGetImage(DEV_ADDR);
            if (nRet == 0) {
                int[] len = {0, 0};
                byte[] Image = new byte[256 * 360];
                a6.ZAZUpImage(DEV_ADDR, Image, len);
                String str = "/mnt/sdcard/test.bmp";
                a6.ZAZImgData2BMP(Image, str);
                Bitmap bmpDefaultPic = BitmapFactory.decodeFile(str);
                handler.sendMessage(handler.obtainMessage(3, bmpDefaultPic));
                handler.postDelayed(getFpTasks, 1000);
            } else if (nRet == a6.PS_NO_FINGER) {
                handler.sendMessage(handler.obtainMessage(0, mContext.getString(R.string.on_reader_nofinger)));
                handler.postDelayed(getFpTasks, 100);

            } else if (nRet == a6.PS_GET_IMG_ERR) {
                handler.sendMessage(handler.obtainMessage(0, mContext.getString(R.string.get_image_fail)));
                handler.postDelayed(getFpTasks, 100);
                return;
            } else if (nRet == -2) {
                handler.sendMessage(handler.obtainMessage(0, mContext.getString(R.string.receive_erro)));
                removeCallbacks();
            } else {
                handler.sendMessage(handler.obtainMessage(0, mContext.getString(R.string.communication_erro)));
                removeCallbacks();
                return;
            }
        }
    };

    @Override
    public void createTemplate() {
        removeCallbacks();
        handler.postDelayed(templateTask, 0);
    }

    byte[] pTemplet = null;
    private Runnable templateTask = new Runnable() {
        // 运行该服务执行此函数
        public void run() {
            int nRet = 0;
            nRet = a6.ZAZGetImage(DEV_ADDR);
            if (nRet == 0) {
                nRet = a6.ZAZGenChar(DEV_ADDR, a6.CHAR_BUFFER_A);
                if (nRet == a6.PS_OK) {
                    int[] iTempletLength = {0, 0};
                    pTemplet = new byte[512];
                    a6.ZAZSetCharLen(512);
                    nRet = a6.ZAZUpChar(DEV_ADDR, a6.CHAR_BUFFER_A, pTemplet, iTempletLength);
                    if (nRet == a6.PS_OK) {

                        handler.sendMessage(handler.obtainMessage(4, pTemplet));
                    }
                    nRet = a6.ZAZDownChar(DEV_ADDR, a6.CHAR_BUFFER_A, pTemplet, iTempletLength[0]);
                    if (nRet == a6.PS_OK) {
                    }
                } else {
                    handler.sendMessage(handler.obtainMessage(0, mContext.getString(R.string.template_bad)));
                    handler.postDelayed(templateTask, 600);
                }
            } else if (nRet == a6.PS_NO_FINGER) {
                handler.postDelayed(templateTask, 100);
            } else if (nRet == a6.PS_GET_IMG_ERR) {
                handler.sendMessage(handler.obtainMessage(0, mContext.getString(R.string.get_image_fail)));
                handler.postDelayed(templateTask, 100);
                return;
            } else if (nRet == -2) {
                handler.sendMessage(handler.obtainMessage(0, mContext.getString(R.string.receive_erro)));
                removeCallbacks();
                return;
            } else {
                handler.sendMessage(handler.obtainMessage(0, mContext.getString(R.string.communication_erro)));
                removeCallbacks();
                return;
            }

        }
    };

    @Override
    public void enrollment() {
        removeCallbacks();
        fpcharbuf = 1;
        handler.postDelayed(enrollmentTasks, 0);
    }

    private int iPageID = 0;
    byte[] pTempletbase = new byte[2304];
    private Runnable enrollmentTasks = new Runnable() {
        public void run()// 运行该服务执行此函数
        {
            int nRet = 0;
            nRet = a6.ZAZGetImage(DEV_ADDR);
            if (nRet == 0) {
                nRet = a6.ZAZGenChar(DEV_ADDR, fpcharbuf);
                if (nRet == a6.PS_OK) {
                    fpcharbuf++;
                    if (fpcharbuf > 2) {
                        nRet = a6.ZAZRegModule(DEV_ADDR);
                        if (nRet != a6.PS_OK) {
                            handler.sendMessage(handler.obtainMessage(0, mContext.getString(R.string.RegModulefail_str)));
                        } else {
                            nRet = a6.ZAZStoreChar(DEV_ADDR, 1, iPageID);
                            if (nRet == a6.PS_OK) {

                                int[] iTempletLength = new int[1];
                                nRet = a6.ZAZUpChar(DEV_ADDR, 1, pTempletbase, iTempletLength);
                                handler.sendMessage(handler.obtainMessage(6, iPageID));
                                iPageID++;
                            } else {
                            }
                        }
                    } else {
                        handler.sendMessage(handler.obtainMessage(0, mContext.getString(R.string.get_image_again)));
                        handler.postDelayed(enrollmentTasks, 800);

                    }
                } else {
                    handler.sendMessage(handler.obtainMessage(0, mContext.getString(R.string.template_bad)));
                    handler.postDelayed(enrollmentTasks, 1000);
                }
            } else if (nRet == a6.PS_NO_FINGER) {
                handler.sendMessage(handler.obtainMessage(0, mContext.getString(R.string.on_reader_nofinger)));
                handler.postDelayed(enrollmentTasks, 100);
            } else if (nRet == a6.PS_GET_IMG_ERR) {
                handler.sendMessage(handler.obtainMessage(0, mContext.getString(R.string.get_image_fail)));
                removeCallbacks();
                return;
            } else if (nRet == -2) {
                handler.sendMessage(handler.obtainMessage(0, mContext.getString(R.string.receive_erro)));
                handler.postDelayed(enrollmentTasks, 100);
                return;
            } else {
                handler.sendMessage(handler.obtainMessage(0, mContext.getString(R.string.communication_erro)));
                removeCallbacks();
                return;
            }

        }
    };

    @Override
    public void comparisonFinger(byte[] bytes, ArrayList<byte[]> array) {

    }

    @Override
    public void comparisonFinger(byte[] bytes1, byte[] bytes2) {

    }

    @Override
    public void comparisonFinger() {
        removeCallbacks();
        fpcharbuf = 1;
        handler.postDelayed(ComparisonTasks, 0);
    }

    private Runnable ComparisonTasks = new Runnable() {
        // 运行该服务执行此函数
        public void run() {
            int nRet = 0;
            int[] id_iscore = new int[1];
            nRet = a6.ZAZGetImage(DEV_ADDR);

            if (nRet == 0) {
                nRet = a6.ZAZGenChar(DEV_ADDR, fpcharbuf);// != PS_OK) {
                if (nRet == a6.PS_OK) {
                    nRet = a6.ZAZHighSpeedSearch(DEV_ADDR, 1, 0, 1000, id_iscore);
                    if (nRet == a6.PS_OK) {
                        int[] iScore = {0, 0};
                        nRet = a6.ZAZMatch(DEV_ADDR, iScore);
                        if (nRet == a6.PS_OK) {
                            Log.i(TAG, "ID:" + id_iscore[0]);
                            handler.sendMessage(handler.obtainMessage(5, +iScore[0]));
                        } else {
                            handler.sendMessage(handler.obtainMessage(5, iScore[0]));
                        }
                    } else {
                        handler.sendMessage(handler.obtainMessage(0, mContext.getString(R.string.search_fail)));
                    }
                } else {
                    handler.sendMessage(handler.obtainMessage(0, mContext.getString(R.string.template_bad)));
                    handler.postDelayed(ComparisonTasks, 1000);

                }

            } else if (nRet == a6.PS_NO_FINGER) {
                handler.sendMessage(handler.obtainMessage(0, mContext.getString(R.string.on_reader_nofinger)));
                handler.postDelayed(ComparisonTasks, 100);
            } else if (nRet == a6.PS_GET_IMG_ERR) {
                handler.sendMessage(handler.obtainMessage(0, mContext.getString(R.string.get_image_fail)));
                handler.postDelayed(ComparisonTasks, 100);
                return;
            } else if (nRet == -2) {
                handler.sendMessage(handler.obtainMessage(0, mContext.getString(R.string.receive_erro)));
                handler.postDelayed(ComparisonTasks, 100);
                return;
            } else {
                handler.sendMessage(handler.obtainMessage(0, mContext.getString(R.string.communication_erro)));
                removeCallbacks();
                return;
            }
        }
    };


    @Override
    public int getImageQuality() {
        return 0;
    }

    @Override
    public boolean clearFinger() {
        int Rnet = a6.ZAZEmpty(DEV_ADDR);
        iPageID = 0;
        return true;

    }

    @Override
    public void searchFinger() {
        removeCallbacks();
        fpcharbuf=1;
        handler.postDelayed(searchTasks, 0);
    }

    private Runnable searchTasks = new Runnable() {
        public void run()// 运行该服务执行此函数
        {

            int[] id_iscore = new int[1];
            int nRet = a6.ZAZGetImage(DEV_ADDR);
            if (nRet == 0) {
                nRet = a6.ZAZGenChar(DEV_ADDR, fpcharbuf);// != PS_OK) {
                if (nRet == a6.PS_OK) {
                    nRet = a6.ZAZHighSpeedSearch(DEV_ADDR, 1, 0, 1000, id_iscore);
                    if (nRet == a6.PS_OK) {
                        handler.sendMessage(handler.obtainMessage(7, id_iscore[0]));
                    } else {
                        handler.sendMessage(handler.obtainMessage(0, mContext.getString(R.string.search_failed)));
                    }

                } else {
                    handler.sendMessage(handler.obtainMessage(0, mContext.getString(R.string.template_bad)));
                    handler.postDelayed(searchTasks, 800);

                }

            } else if (nRet == a6.PS_NO_FINGER) {
                handler.postDelayed(searchTasks, 100);
            } else if (nRet == a6.PS_GET_IMG_ERR) {
                handler.sendMessage(handler.obtainMessage(0, mContext.getString(R.string.get_image_fail)));
                handler.postDelayed(searchTasks, 100);
                return;
            } else if (nRet == -2) {
                handler.sendMessage(handler.obtainMessage(0, mContext.getString(R.string.receive_erro)));
                removeCallbacks();
                return;
            } else {
                handler.sendMessage(handler.obtainMessage(0, mContext.getString(R.string.communication_erro)));
                removeCallbacks();
                return;
            }

        }
    };

    @Override
    public void unObject() {
        removeCallbacks();
    }

}
