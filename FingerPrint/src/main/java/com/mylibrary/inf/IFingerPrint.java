package com.mylibrary.inf;

import com.digitalpersona.uareu.Fmd;

/**
 * Created by suntianwei on 2017/3/28.
 */

public interface IFingerPrint {

//
//    /**
//     * 模板初始化，切换usb
//     *
//     * @param serialport    串口
//     * @param braut         波特率
//     * @param power_typeint 上电类型
//     *                      * @param context
//     * @param gpio          上电管脚
//     * @throws IOException
//     */
//    public void initDev(Activity activity, DeviceControl.PowerType power_typeint,
//                        Context context, Handler handler,int... gpio) throws IOException;
//
//    /**
//     * 自动判断上电
//     *
//     * @param context
//     * @throws IOException
//     */
//    public void initDev(Context context, Activity activity) throws IOException;

    /**
     * 打开指纹模板
     */
    public boolean openReader();

    public boolean closeReader();

    public void getImage();

    public void createTemplate();

    public void enrollment();

    public void comparisonFinger(Fmd fmd1, Fmd fmd2);

    public void comparisonFinger(byte[] bytes1, byte[] bytes2);

    public void getImageQuality();

    public boolean clearFinger();
    public void unObject();


}
