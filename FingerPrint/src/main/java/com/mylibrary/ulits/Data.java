package com.mylibrary.ulits;

import android.graphics.Bitmap;

import com.digitalpersona.uareu.Fmd;

import java.io.Serializable;

/**
 * Created by lenovo-pc on 2017/5/9.
 */

public class Data implements Serializable {
    Bitmap FingerBitmap = null;
    byte[] TemplateBytes =null;
    String InfoMsg = "";
    boolean OpenFlag = true;
    Fmd Tcs1gFmd = null;
    int FinferQualitys = 0;

    int comparisonNum = -1;

    public int getFinferQualitys() {
        return FinferQualitys;
    }


    public void setFinferQualitys(int finferQualitys) {
        FinferQualitys = finferQualitys;
    }


    public Bitmap getFingerBitmap() {
        return FingerBitmap;
    }

    public void setFingerBitmap(Bitmap fingerBitmap) {
        FingerBitmap = fingerBitmap;
    }

    public byte[] getTemplateBytes() {
        return TemplateBytes;
    }

    public void setTemplateBytes(byte[] templateBytes) {
        TemplateBytes = templateBytes;
    }


    public String getInfoMsg() {
        return InfoMsg;
    }

    public void setInfoMsg(String infoMsg) {
        InfoMsg = infoMsg;
    }

    public boolean isOpenFlag() {
        return OpenFlag;
    }

    public void setOpenFlag(boolean openFlag) {
        OpenFlag = openFlag;
    }


    public Fmd getTcs1gFmd() {
        return Tcs1gFmd;
    }

    public void setTcs1gFmd(Fmd tcs1gFmd) {
        Tcs1gFmd = tcs1gFmd;
    }

    public int getComparisonNum() {
        return comparisonNum;
    }

    public void setComparisonNum(int comparisonNum) {
        this.comparisonNum = comparisonNum;
    }
}
