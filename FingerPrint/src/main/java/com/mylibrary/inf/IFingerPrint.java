package com.mylibrary.inf;

import com.digitalpersona.uareu.Fmd;

/**
 * Created by suntianwei on 2017/3/28.
 */

public interface IFingerPrint  {

    /**
     * 打开指纹模板
     */
    public void openReader();

    public void  closeReader();

    public void getImage();

    public void createTemplate();


    public void enrollment();

    public void comparisonFinger(Fmd fmd1, Fmd fmd2);

    public void comparisonFinger(byte[] bytes1, byte[] bytes2);

    public void comparisonFinger();

    public int getImageQuality();

    public boolean clearFinger();

    public void searchFinger();

    public void unObject();


}
