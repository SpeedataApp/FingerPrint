package com.mylibrary.ulits;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import com.digitalpersona.uareu.Fmd;

/**
 * Created by lenovo-pc on 2017/5/9.
 */

public class Data implements Parcelable {
    Bitmap FingerBitmap = null;
    byte[] TemplateBytes = null;
    String InfoMsg = "";
    boolean OpenFlag = true;
    Fmd Tcs1gFmd = null;
    int FinferQualitys = 0;

    int comparisonNum = -1;

    public Data() {

    }

    public Data(Parcel in) {
        FingerBitmap = in.readParcelable(Bitmap.class.getClassLoader());
        TemplateBytes = in.createByteArray();
        InfoMsg = in.readString();
        OpenFlag = in.readByte() != 0;
        FinferQualitys = in.readInt();
        comparisonNum = in.readInt();
    }

    public static final Creator<Data> CREATOR = new Creator<Data>() {
        @Override
        public Data createFromParcel(Parcel in) {
            return new Data(in);
        }

        @Override
        public Data[] newArray(int size) {
            return new Data[size];
        }
    };



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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(FingerBitmap, flags);
        dest.writeByteArray(TemplateBytes);
        dest.writeString(InfoMsg);
        dest.writeByte((byte) (OpenFlag ? 1 : 0));
        dest.writeInt(FinferQualitys);
        dest.writeInt(comparisonNum);
    }
}
