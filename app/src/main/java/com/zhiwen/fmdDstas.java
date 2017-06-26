package com.zhiwen;

import java.io.Serializable;

/**
 * Created by lenovo-pc on 2017/6/19.
 */

public class fmdDstas implements Serializable {

    private static final long serialVersionUID = 1L;

//    Fmd fmd;

    byte[] FmdByte;

    public byte[] getFmdByte() {
        return FmdByte;
    }

        public void setFmdByte(byte[] fmdByte) {
        FmdByte = fmdByte;
    }

//    public Fmd getFmd() {
//        return fmd;
//    }
//
//    public void setFmd(Fmd fmd) {
//        this.fmd = fmd;
//    }
}
