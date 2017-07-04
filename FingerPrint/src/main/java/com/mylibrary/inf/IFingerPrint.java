package com.mylibrary.inf;

import java.util.ArrayList;

/**
 * Created by suntianwei on 2017/3/28.
 */

public interface IFingerPrint  {

    /**
     * 打开指纹模板
     */
    public void openReader();

    /**
     * 关闭指纹模板
     */
    public void  closeReader();

    /**
     * 获取指纹图像
     */
    public void getImage();

    /**
     * 创建指纹特征模板
     */
    public void createTemplate();

    /**
     * 注册一个指纹
     */
    public void enrollment();

    /***
     * 金色指纹1比n
     * @param bytes  要比对的指纹特征模板
     * @param array  指纹模板组
     */
    public void comparisonFinger(byte[] bytes, ArrayList<byte[]> array);

    /***
     * 指纹1:1比对
     * @param bytes1  指纹特征模板1
     * @param bytes2  指纹特征模板2
     */
    public void comparisonFinger(byte[] bytes1, byte[] bytes2);

    public void comparisonFinger();

    /**
     * 获取公安指纹质量
     * @return  0～100
     */
    public int getImageQuality();

    /**
     * 民用指纹 清除指纹特征模板
     * @return
     */
    public boolean clearFinger();

    /**
     * 民用指纹 搜索指纹 1：n
     */
    public void searchFinger();

    /**
     * 退出
     */
    public void unObject();


}
