package com.IDWORLD;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class LAPI {
	static final String TAG = "LAPI";
	//****************************************************************************************************
	static
	{
		try{
			System.loadLibrary("biofp_e_lapi");
		}
		catch(UnsatisfiedLinkError e) {
			Log.e("LAPI","biofp_e_lapi",e);
		}
	}
	//****************************************************************************************************
	static final File PWFILE = new File("/sys/class/power_supply/usb/device/CONTROL_GPIO114");
	//****************************************************************************************************
	public static final int VID = 0x0483;
	public static final int PID = 0x5710;
	private static HostUsb m_usbHost = null;
	private static int m_hUSB = 0;
	public static final int MSG_OPEN_DEVICE = 0x10;
	public static final int MSG_CLOSE_DEVICE = 0x11;
	public static final int MSG_BULK_TRANS_IN = 0x12;
	public static final int MSG_BULK_TRANS_OUT = 0x13;
	//****************************************************************************************************
	public static final int WIDTH  = 256;
	public static final int HEIGHT  = 360;
	public static final int IMAGE_SIZE = WIDTH*HEIGHT;
	//****************************************************************************************************
	public static final int FPINFO_STD_MAX_SIZE = 1024;
	public static final int DEF_QUALITY_SCORE = 30;
	public static final int DEF_MATCH_SCORE = 45;
	//****************************************************************************************************
	public static final int TRUE = 1;
	public static final int FALSE = 0;
	//****************************************************************************************************
	private static Activity m_content = null;



	//****************************************************************************************************
	private static int CallBack (int message, int notify, int param, Object data)
	{
		switch (message) {
			case MSG_OPEN_DEVICE:
				m_usbHost = new HostUsb(m_content,VID,PID);
				if (m_usbHost != null) {
					if(m_usbHost.WaitForInterfaces() == false) return 0;
					//m_usbHost.WaitForInterfaces();
					m_hUSB = m_usbHost.OpenDeviceInterfaces();
					if (m_hUSB<0) {
						return 0;
					}
				}
				return m_hUSB;
			case MSG_CLOSE_DEVICE:
				if (m_usbHost != null) {
					m_usbHost.CloseDeviceInterface();
					m_hUSB = -1;
				}
				return 1;
			case MSG_BULK_TRANS_IN:
				m_usbHost.USBBulkReceive((byte[])data,notify,param);
				return 1;
			case MSG_BULK_TRANS_OUT:
				m_usbHost.USBBulkSend((byte[])data,notify,param);
				return 1;
		}
		return 0;
	}
	//****************************************************************************************************
	public LAPI(Activity a) {
		m_content = a;
	}
	//****************************************************************************************************
	protected void POWER_ON()
	{
		try {
			FileReader inCmd = new FileReader(PWFILE);
			inCmd.read();
			inCmd.close();
		}
		catch (Exception e) {}
		//try {
		//	Thread.sleep(500);
		//} catch (InterruptedException e) {}
	}
	//****************************************************************************************************
	protected void POWER_OFF()
	{
		FileWriter closefr;
		try {
			closefr = new FileWriter(PWFILE);
			closefr.write("1");
			closefr.close();
		}
		catch (Exception e) {}
		//try {
		//	Thread.sleep(500);
		//} catch (InterruptedException e) {}
	}
	//------------------------------------------------------------------------------------------------//
	// Purpose   : This function returns string version of the Finger Recognition SDK.
	// Function  : GetVersion
	// Arguments : void
	// Return    : String
	//			     return string version of the Finger Library SDK.
	//------------------------------------------------------------------------------------------------//
	public native String GetVersion();
	//------------------------------------------------------------------------------------------------//
	// Purpose   : This function initializes the Finger Recognition SDK Library.
	// Function  : OpenDevice
	// Arguments : void
	// Return    : int If successful, return handle of device, else 0.
	//------------------------------------------------------------------------------------------------//
	private native int OpenDevice();
	public int OpenDeviceEx()
	{
		//POWER_ON();
		return OpenDevice();
	}
	//------------------------------------------------------------------------------------------------//
	// Purpose   : This function finalizes the Finger Recognition SDK Library.
	// Function  : CloseDevice
	// Arguments :
	//      (In） : int device : used with the return of function "OpenDevice()"
	// Return    : int
	//			      If successful, return 1, else 0
	//------------------------------------------------------------------------------------------------//
	private  native int CloseDevice(int device);
	public int CloseDeviceEx(int device)
	{
		int ret;
		ret = CloseDevice(device);
		//POWER_OFF();
		return ret;
	}
	//------------------------------------------------------------------------------------------------//
	// Purpose   : This function return image captured from this device.
	// Function  : GetImage
	// Arguments :
	//      (In） : int device : used with the return of function "OpenDevice()"
	//  (In/Out） : byte[] image : image captured from this device
	// Return    : int
	//			      If successful, return 1, else 0
	//------------------------------------------------------------------------------------------------//
	public native int GetImage(int device, byte[] image);
	//------------------------------------------------------------------------------------------------//
	// Purpose   : This function lets calibration of this sensor device.
	// Function  : Calibration
	// Arguments :
	//      (In） : int device : used with the return of function "OpenDevice()"
	// Return    :
	//			   int :   If successful, return 1, else 0
	//------------------------------------------------------------------------------------------------//
	public native int Calibration(int device);
	//------------------------------------------------------------------------------------------------//
	// Purpose   : This function checks whether finger is on sensor of this device or not.
	// Function  : IsPressFinger
	// Arguments :
	//      (In） : int device : used with the return of function "OpenDevice()"
	//		(In) : byte[] image : image return by function "GetImage()"
	// Return    : int
	//				   return percent value measured finger-print on sensor(0~100).
	//------------------------------------------------------------------------------------------------//
	public native int IsPressFinger(int device,byte[] image);
	//------------------------------------------------------------------------------------------------//
	// Purpose   : This function creates the ANSI standard template from the uncompressed raw image.
	// Function  : CreateStdTemplate
	// Arguments :
	//      (In）: int device : used with the return of function "OpenDevice()"
	//		(In) : byte[] image : image return by function "GetImage()"
	//	(In/Out) : byte[] itemplate : template created from image.
	// Return    : int :
	//				   If this function successes, return none-zero, else 0.
	//------------------------------------------------------------------------------------------------//
	public native int CreateTemplate(int device,byte[] image, byte[] itemplate);
	//------------------------------------------------------------------------------------------------//
	// Purpose   : This function gets the quality value of fingerprint raw image.
	// Function  : GetImageQuality
	// Arguments :
	//      (In）: int device : used with the return of function "OpenDevice()"
	//		(In) : byte[] image : image return by function "GetImage()"
	// Return    : int :
	//				   return quality value(0~100) of fingerprint raw image.
	//------------------------------------------------------------------------------------------------//
	public native int GetImageQuality(int device,byte[] image);
	//------------------------------------------------------------------------------------------------//
	// Purpose   : This function matches two templates and return similar match score.
	//             This function is for 1:1 Matching and only used in finger-print verification.
	// Function  : CompareTemplates
	// Arguments :
	//          (In）: int device : used with the return of function "OpenDevice()"
	//			(In) : byte[] itemplateToMatch : template to match : 
	//                 This template must be used as that is created by function "CreateANSITemplate()".  
	//                 or function "CreateISOTemplate()".
	//			(In) : byte[] itemplateToMatched : template to be matched
	//                 This template must be used as that is created by function "CreateANSITemplate()".  
	//                 or function "CreateISOTemplate()".
	// Return    : int 
	//					return similar match score(0~100) of two finger-print templates.
	//------------------------------------------------------------------------------------------------//
	public native int CompareTemplates(int device,byte[] itemplateToMatch, byte[] itemplateToMatched);
	//------------------------------------------------------------------------------------------------//
}
