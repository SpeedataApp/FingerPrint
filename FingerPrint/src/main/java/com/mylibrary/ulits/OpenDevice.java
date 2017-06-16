package com.mylibrary.ulits;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.za.finger.ZAandroid;

import java.io.DataOutputStream;
import java.io.File;
import java.util.HashMap;


/**
 * Created by suntianwei on 2017/3/1.
 */

public class OpenDevice {
    String TAG="opendevice";
    private int isusbfinshed = 0;
    Context mContext;
    private UsbManager mDevManager = null;
    private UsbInterface intf = null;

    /**
     *
     * @param context
     * @param ZAInteface  接口
     * @param DEV_ADDR   地址
     * @param IMG_SIZE   图片大小 同参数：（0:256x288 1:256x360）
     * @param usborcomtype  设备是否被root 0-正常 1-root
     * @return  0 打开成功 1 打开失败 2 设备未准备好
     */
    public int OpenDev(Context context, ZAandroid ZAInteface, int DEV_ADDR, int IMG_SIZE,int usborcomtype) {
        mContext = context;
        byte[] pPassword = new byte[4];
        int status = 0;
        if (1 == usborcomtype) {
            LongDunD8800_CheckEuq();
            status = ZAInteface.ZAZOpenDeviceEx(-1, 5, 3, 6, 0, 0);
            if (status == 1 && ZAInteface.ZAZVfyPwd(DEV_ADDR, pPassword) == 0) {
                status = 1;
            } else {
                status = 0;
            }
            ZAInteface.ZAZSetImageSize(IMG_SIZE);
        } else {
            device = null;
            isusbfinshed = getrwusbdevices();
            if (WaitForInterfaces() == false) {
                return 2;//USB没有准备好
            }
            int fd = OpenDeviceInterfaces();
            if (fd == -1) {
                return 2;//USB没有准备好
            }
            Log.e(TAG, "zhw === open fd: " + fd);
            status = ZAInteface.ZAZOpenDeviceEx(fd, 5, 3, 6, 0, 0);
            if (status == 1 && ZAInteface.ZAZVfyPwd(DEV_ADDR, pPassword) == 0) {
                status = 1;
            } else {
                status = 0;
            }
            ZAInteface.ZAZSetImageSize(IMG_SIZE);
        }
        Log.e(TAG, " open status: " + status);
        if (status == 1) {
           return 0;//打开成功
        } else {
           return 1;//打开失败
        }
    }

    public int LongDunD8800_CheckEuq() {
        Process process = null;
        DataOutputStream os = null;
        String path = "/dev/bus/usb/00*/*";
        String path1 = "/dev/bus/usb/00*/*";
        File fpath = new File(path);
        Log.d("*** LongDun D8800 ***", " check path:" + path);
        // if (fpath.exists())
        // {
        String command = "chmod 777 " + path;
        String command1 = "chmod 777 " + path1;
        Log.d("*** LongDun D8800 ***", " exec command:" + command);
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
            return 1;
        } catch (Exception e) {
            Log.d("*** DEBUG ***", "Unexpected error - Here is what I know: " + e.getMessage());
        }
        return 0;
    }

    UsbDevice device = null;
    private UsbDeviceConnection connection = null;

    public int OpenDeviceInterfaces() {
        UsbDevice mDevice = device;
        Log.d(TAG, "setDevice " + mDevice);
        int fd = -1;
        if (mDevice == null) return -1;
        connection = mDevManager.openDevice(mDevice);
        if (!connection.claimInterface(mDevice.getInterface(0), true)) return -1;

        if (mDevice.getInterfaceCount() < 1) return -1;
        intf = mDevice.getInterface(0);

        if (intf.getEndpointCount() == 0) return -1;

        if ((connection != null)) {
            if (true) Log.e(TAG, "open connection success!");
            fd = connection.getFileDescriptor();
            return fd;
        } else {
            if (true) Log.e(TAG, "finger device open connection FAIL");
            return -1;
        }
    }

    private PendingIntent permissionIntent = null;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    public int getrwusbdevices() {

        mDevManager = ((UsbManager) mContext.getSystemService(Context.USB_SERVICE));
        permissionIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        mContext.registerReceiver(mUsbReceiver, filter);
        HashMap<String, UsbDevice> deviceList = mDevManager.getDeviceList();
        if (true) Log.e(TAG, "news:" + "mDevManager");
        for (UsbDevice tdevice : deviceList.values()) {
            Log.i(TAG, tdevice.getDeviceName() + " " + Integer.toHexString(tdevice.getVendorId()) + " "
                    + Integer.toHexString(tdevice.getProductId()));
            if (tdevice.getVendorId() == 0x2109 && (tdevice.getProductId() == 0x7638)) {
                Log.e(TAG, " 指纹设备准备好了 ");
                mDevManager.requestPermission(tdevice, permissionIntent);
                return 1;
            }
        }
        Log.e(TAG, "news:" + "mDevManager  end");
        return 2;
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            context.unregisterReceiver(mUsbReceiver);
            isusbfinshed = 0;
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (context) {
                    device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    Log.e("BroadcastReceiver", "3333");
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            if (true) Log.e(TAG, "Authorize permission " + device);
                            isusbfinshed = 1;
                        }
                    } else {
                        if (true) Log.e(TAG, "permission denied for device " + device);
                        device = null;
                        isusbfinshed = 2;

                    }
                }
            }
        }
    };

    public boolean WaitForInterfaces() {

        while (device == null || isusbfinshed == 0) {
            if (isusbfinshed == 2) break;
            if (isusbfinshed == 3) break;
        }
        if (isusbfinshed == 2)
            return false;
        if (isusbfinshed == 3)
            return false;
        return true;
    }
}
