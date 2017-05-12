package com.mylibrary.realize;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;

import com.digitalpersona.uareu.Fmd;
import com.mylibrary.inf.IFingerPrint;
import com.mylibrary.inf.MsgCallBack;
import com.mylibrary.ulits.Data;
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
    private int fpcharbuf = 1;
    private String TAG = "Tcs1";
    private MsgCallBack callBack = null;
    private Data data = null;

    public TCS1Realize(Context context, Handler handler, MsgCallBack callBack) {
        mContext = context;
        this.handler = handler;
        this.callBack = callBack;
        data = new Data();
    }

    @Override
    public void openReader() {
        a6 = new ZAandroid();
        final OpenDevice openDevice = new OpenDevice();
        Runnable r = new Runnable() {
            public void run() {
                stada = openDevice.OpenDev(mContext, a6, DEV_ADDR, IMG_SIZE, 0);
                if (stada == 0) {
                    data.setOpenFlag(true);
                    data.setInfoMsg("打开成功");
                    callBack.callBackInfo(data);
                } else {
                    data.setOpenFlag(false);
                    data.setInfoMsg("打开失败");
                    callBack.callBackInfo(data);
                }
            }
        };
        Thread s = new Thread(r);
        s.start();

    }

    @Override
    public void closeReader() {
        int status = a6.ZAZCloseDeviceEx();
        Log.e(TAG, " close status: " + status);
        if (stada == 0) {
            data.setOpenFlag(false);
            data.setInfoMsg("关闭成功");
            callBack.callBackInfo(data);
        } else {
            data.setOpenFlag(true);
            data.setInfoMsg("关闭失败");
            callBack.callBackInfo(data);
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
                data.setFingerBitmap(bmpDefaultPic);
                data.setInfoMsg("获取图像成功");
                callBack.callBackInfo(data);
//                handler.sendMessage(handler.obtainMessage(0, "获取图像成功"));
//                handler.sendMessage(handler.obtainMessage(10, bmpDefaultPic));
                handler.postDelayed(getFpTasks, 100);
            } else if (nRet == a6.PS_NO_FINGER) {
                data.setInfoMsg("传感器上没有手指");
                callBack.callBackInfo(data);
//                handler.sendMessage(handler.obtainMessage(0, "传感器上没有手指"));
                handler.postDelayed(getFpTasks, 100);
            } else if (nRet == a6.PS_GET_IMG_ERR) {
                data.setInfoMsg("指纹录取失败");
                callBack.callBackInfo(data);
//                handler.sendMessage(handler.obtainMessage(0, "指纹录取失败"));
                handler.postDelayed(getFpTasks, 100);
                return;
            } else if (nRet == -2) {
                data.setInfoMsg("接收数据包失败");
                callBack.callBackInfo(data);
//                handler.sendMessage(handler.obtainMessage(0, "接收数据包失败"));
                removeCallbacks();
            } else {
                data.setInfoMsg("通讯异常");
                callBack.callBackInfo(data);
//                handler.sendMessage(handler.obtainMessage(0, "通讯异常"));
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
                        data.setInfoMsg("获取特征成功");
                        data.setTemplateBytes(pTemplet);
                        callBack.callBackInfo(data);
//                        removeCallbacks();
//                        handler.sendMessage(handler.obtainMessage(11, pTemplet));
                    }
                    nRet = a6.ZAZDownChar(DEV_ADDR, a6.CHAR_BUFFER_A, pTemplet, iTempletLength[0]);
                    if (nRet == a6.PS_OK) {
                    }
                } else {
                    data.setInfoMsg("特征太差重新录入");
                    data.setTemplateBytes(null);
                    callBack.callBackInfo(data);
//                    handler.sendMessage(handler.obtainMessage(0, "特征太差重新录入"));
                    handler.postDelayed(templateTask, 600);
                }
            } else if (nRet == a6.PS_NO_FINGER) {
                data.setTemplateBytes(null);
                data.setInfoMsg("传感器上没有手指");
                callBack.callBackInfo(data);
//                handler.sendMessage(handler.obtainMessage(0, "传感器上没有手指"));
                handler.postDelayed(templateTask, 100);
            } else if (nRet == a6.PS_GET_IMG_ERR) {
                data.setTemplateBytes(null);
                data.setInfoMsg("指纹录失败");
                callBack.callBackInfo(data);
//                handler.sendMessage(handler.obtainMessage(0, "指纹录失败"));
                handler.postDelayed(templateTask, 100);
                return;
            } else if (nRet == -2) {
                data.setTemplateBytes(null);
                data.setInfoMsg("接收数据包失败");
                callBack.callBackInfo(data);
//                handler.sendMessage(handler.obtainMessage(0, "接收数据包失败"));
                removeCallbacks();
                return;
            } else {
                data.setTemplateBytes(null);
                data.setInfoMsg("通讯异常");
                callBack.callBackInfo(data);
//                handler.sendMessage(handler.obtainMessage(0, "通讯异常"));
                removeCallbacks();
                return;
            }

        }
    };

    @Override
    public void enrollment() {
        removeCallbacks();
        fpcharbuf = 1;
        data=new Data();
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
                            data.setInfoMsg("合成模板失败");
                            callBack.callBackInfo(data);
//                            handler.sendMessage(handler.obtainMessage(0, "合成模板失败"));

                        } else {
                            nRet = a6.ZAZStoreChar(DEV_ADDR, 1, iPageID);
                            if (nRet == a6.PS_OK) {

                                int[] iTempletLength = new int[1];
                                nRet = a6.ZAZUpChar(DEV_ADDR, 1, pTempletbase, iTempletLength);
                                data.setInfoMsg("合成模板成功 ID:"+ iPageID);
                                callBack.callBackInfo(data);
//                                handler.sendMessage(handler.obtainMessage(0, "合成模板 ID：" + iPageID));
                                iPageID++;
                            } else {
                                data.setInfoMsg("注册失败");
                                callBack.callBackInfo(data);
//                                handler.sendMessage(handler.obtainMessage(0, "注册失败"));
                            }
                        }
                    } else {
                        data.setInfoMsg("获取指纹成功,再次获取");
                        callBack.callBackInfo(data);
//                        handler.sendMessage(handler.obtainMessage(0, "获取指纹成功,再次获取"));
                        handler.postDelayed(enrollmentTasks, 800);

                    }
                } else {
                    data.setInfoMsg("特征太差");
                    callBack.callBackInfo(data);
//                    handler.sendMessage(handler.obtainMessage(0, "特征太差"));
                    handler.postDelayed(enrollmentTasks, 1000);
                }
            } else if (nRet == a6.PS_NO_FINGER) {
                data.setInfoMsg("传感器上没有手指");
                callBack.callBackInfo(data);
//                handler.sendMessage(handler.obtainMessage(0, "传感器上没有手指"));
                handler.postDelayed(enrollmentTasks, 100);
            } else if (nRet == a6.PS_GET_IMG_ERR) {
                data.setInfoMsg("指纹录失败");
                callBack.callBackInfo(data);
//                handler.sendMessage(handler.obtainMessage(0, "指纹录失败"));
                handler.postDelayed(enrollmentTasks, 100);
                return;
            } else if (nRet == -2) {
                data.setInfoMsg("接收数据包失败");
                callBack.callBackInfo(data);
//                handler.sendMessage(handler.obtainMessage(0, "接收数据包失败"));
                return;
            } else {
                data.setInfoMsg("通讯异常");
                callBack.callBackInfo(data);
//                handler.sendMessage(handler.obtainMessage(0, "通讯异常"));
                removeCallbacks();
                return;
            }

        }
    };

    @Override
    public void comparisonFinger(Fmd fmd1, Fmd fmd2) {

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
                            data.setInfoMsg("ID:"+id_iscore[0]+"对比成功得分：" + iScore[0]);
                            callBack.callBackInfo(data);
                        } else {
                            data.setInfoMsg("对比失败得分：" + iScore[0]);
                            callBack.callBackInfo(data);
                        }
                    } else {
                        data.setInfoMsg("搜索失败,请先注册指纹！");
                        callBack.callBackInfo(data);
                    }


//                    if (fpcharbuf == 1) {
//                        int[] iScore = {0, 0};
//                        nRet = a6.ZAZMatch(DEV_ADDR, iScore);
//                        if (nRet == a6.PS_OK) {
//
//                            handler.sendMessage(handler.obtainMessage(0, "对比成功得分：" + iScore[0]));
//                        } else {
//                            handler.sendMessage(handler.obtainMessage(0, "对比失败得分：" + iScore[0]));
//                        }
//                        return;
//                    }
//
//                    fpcharbuf = 2;
//                    handler.sendMessage(handler.obtainMessage(0, "请录入要对的指纹："));
//
//                    handler.postDelayed(ComparisonTasks, 700);
                } else {
                    data.setInfoMsg("特征太差重录");
                    callBack.callBackInfo(data);
                    handler.postDelayed(ComparisonTasks, 1000);

                }

            } else if (nRet == a6.PS_NO_FINGER) {
                data.setInfoMsg("传感器上没有手指");
                callBack.callBackInfo(data);
//                handler.sendMessage(handler.obtainMessage(0, "传感器上没有手指"));
                handler.postDelayed(ComparisonTasks, 10);
            } else if (nRet == a6.PS_GET_IMG_ERR) {
                data.setInfoMsg("指纹录失败");
                callBack.callBackInfo(data);
//                handler.sendMessage(handler.obtainMessage(0, "指纹录失败"));
                handler.postDelayed(ComparisonTasks, 10);
                return;
            } else if (nRet == -2) {
                data.setInfoMsg("接收数据包失败");
                callBack.callBackInfo(data);
//                handler.sendMessage(handler.obtainMessage(0, "接收数据包失败"));
                handler.postDelayed(ComparisonTasks, 10);
                return;
            } else {
                data.setInfoMsg("通讯异常");
                callBack.callBackInfo(data);
//                handler.sendMessage(handler.obtainMessage(0, "通讯异常"));
                removeCallbacks();
                return;
            }
        }
    };


    @Override
    public void getImageQuality() {

    }

    @Override
    public boolean clearFinger() {
        int Rnet = a6.ZAZEmpty(DEV_ADDR);
        return true;

    }

    @Override
    public void searchFinger() {
        removeCallbacks();
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
                        data.setInfoMsg("搜索成功 ID:" + id_iscore[0]);
                        callBack.callBackInfo(data);
//                        handler.sendMessage(handler.obtainMessage(0, "搜索成功 ID：" + id_iscore[0]));
                    } else {
                        data.setInfoMsg("搜索失败");
                        callBack.callBackInfo(data);
//                        handler.sendMessage(handler.obtainMessage(0, "搜索失败"));
                    }

                } else {
                    data.setInfoMsg("特征太差重新录入");
                    callBack.callBackInfo(data);
//                    handler.sendMessage(handler.obtainMessage(0, "特征太差重新录入"));
                    handler.postDelayed(searchTasks, 800);

                }

            } else if (nRet == a6.PS_NO_FINGER) {
                data.setInfoMsg("传感器上没有手指");
                callBack.callBackInfo(data);
                handler.postDelayed(searchTasks, 10);
            } else if (nRet == a6.PS_GET_IMG_ERR) {
                data.setInfoMsg("指纹录失败");
                callBack.callBackInfo(data);
                handler.postDelayed(searchTasks, 10);
                return;
            } else if (nRet == -2) {
                data.setInfoMsg("接收数据包失败");
                callBack.callBackInfo(data);
                removeCallbacks();
                return;
            } else {
                data.setInfoMsg("通讯异常");
                callBack.callBackInfo(data);
                removeCallbacks();
                return;
            }

        }
    };

    @Override
    public void unObject() {
        if (data != null) {
            data = null;
        }
        removeCallbacks();
    }
}
