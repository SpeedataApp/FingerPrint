package com.IDWORLD;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.HashMap;

public class HostUsb 
{
	  private static final String TAG = "OpenHostUsb";
	  private static final boolean D = true;
	  
	   private static final String ACTION_USB_PERMISSION = "com.IDWORLD.USB_PERMISSION";
	
		private Context context = null;
		private UsbManager mDevManager = null;
		private PendingIntent permissionIntent = null;
		private UsbInterface intf = null;
		private UsbDeviceConnection connection = null;
		private UsbDevice device = null;
		public int isusbfinshed = 0;

		private int m_nEPOutSize = 2048;
		private int m_nEPInSize = 2048;
	    private byte[] m_abyTransferBuf = new byte[2048];

		UsbEndpoint endpoint_IN = null;
		UsbEndpoint endpoint_OUT = null;
		UsbEndpoint endpoint_INT = null;
		UsbEndpoint curEndpoint = null;
		
		public HostUsb(Context a, int VID, int PID ) {
			AuthorizeDevice(a,VID,PID);
		}
		
		private boolean AuthorizeDevice(Context paramContext, int VID, int PID) {
			context = paramContext;
			mDevManager = ((UsbManager) paramContext.getApplicationContext().getSystemService(Context.USB_SERVICE));
			permissionIntent = PendingIntent.getBroadcast(paramContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
			IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
			paramContext.registerReceiver(mUsbReceiver, filter);
			paramContext.registerReceiver(mUsbReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));

			HashMap<String, UsbDevice> deviceList = mDevManager.getDeviceList();

			if (D) Log.e(TAG, "news:" + "mDevManager");

			for (UsbDevice tdevice : deviceList.values()) { 
				if (D) Log.e(TAG, "news:" + tdevice);

                if (tdevice.getVendorId() == VID && (tdevice.getProductId() == PID)) 
				{

					mDevManager.requestPermission(tdevice, permissionIntent);
					return true;
				}
			}
			return false;
		}

		private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
			public void onReceive(Context context, Intent intent) {
				context.unregisterReceiver(mUsbReceiver);  
				isusbfinshed = 0; 
				String action = intent.getAction();
				if (ACTION_USB_PERMISSION.equals(action)) {
					synchronized (context) { 
						device = (UsbDevice) intent	.getParcelableExtra(UsbManager.EXTRA_DEVICE);
						if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
								if (device != null) {
									if (D) Log.e(TAG, "Authorize permission " + device); 
									isusbfinshed = 1;
								}
						} 
						else {
							if (D) Log.e(TAG, "permission denied for device " + device); 
							device=null;
							isusbfinshed = 2;
							
						}
					}
				}
			}
		};

		public boolean WaitForInterfaces() {
			
			while (device==null || isusbfinshed == 0) {
				if(isusbfinshed == 2)break;
			} 
			if(isusbfinshed == 2)
				return false;
			return true;
		}

		public int OpenDeviceInterfaces() {
			UsbDevice mDevice = device;
			Log.d(TAG, "setDevice " + mDevice);
			int fd = -1;

			if (mDevice == null) return -1;

			connection = mDevManager.openDevice(mDevice);
	        if (!connection.claimInterface(mDevice.getInterface(0), true)) return -1;
			
			if (mDevice.getInterfaceCount() < 1) return -1;
			intf = mDevice.getInterface(0);

			if (intf.getEndpointCount() == 0) 	return -1;
			
			for (int i = 0; i < intf.getEndpointCount(); i++) {
				if (intf.getEndpoint(i).getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
					if (intf.getEndpoint(i).getDirection() == UsbConstants.USB_DIR_IN) {
						endpoint_IN = intf.getEndpoint(i);
					}
					else if (intf.getEndpoint(i).getDirection() == UsbConstants.USB_DIR_OUT) {
						endpoint_OUT = intf.getEndpoint(i);
					}
				} 
				else if (intf.getEndpoint(i).getType() == UsbConstants.USB_ENDPOINT_XFER_INT) {
					endpoint_INT = intf.getEndpoint(i);
				} 
				else {
					if (D) Log.e(TAG, "Not Endpoint or other Endpoint ");
				}
			}
			curEndpoint = intf.getEndpoint(0);

			if ((connection != null)) {
				if (D) Log.e(TAG, "open connection success!");
				fd = connection.getFileDescriptor();
				return fd;
			} 
			else {
				if (D) Log.e(TAG, "finger device open connection FAIL");
				return -1;
			}
		}

		public void CloseDeviceInterface() {
			if (connection != null) {
				connection.releaseInterface(intf);
				connection.close();
			}
		}

	    public boolean USBBulkSend(byte[] pBuf, int nLen, int nTimeOut)
	    {
	        int i, n, r, w_nRet;

	        n = nLen / m_nEPOutSize;
	        r = nLen % m_nEPOutSize;

			for(i=0; i<n; i++)
	        {
	            System.arraycopy(pBuf, i*m_nEPOutSize, m_abyTransferBuf, 0, m_nEPOutSize);
	            w_nRet = connection.bulkTransfer(endpoint_OUT, m_abyTransferBuf, m_nEPOutSize, nTimeOut);
	            if (w_nRet != m_nEPOutSize)
	                return false;
	        }

	        if (r > 0)
	        {
	            System.arraycopy(pBuf, i*m_nEPOutSize, m_abyTransferBuf, 0, r);
	            w_nRet = connection.bulkTransfer(endpoint_OUT, m_abyTransferBuf, r, nTimeOut);
	            if (w_nRet != r)
	                return false;
	        }
	        return true;
	    }

	    public boolean USBBulkReceive(byte[] pBuf, int nLen, int nTimeOut)
	    {
	        int i, n, r, w_nRet;

	        n = nLen / m_nEPInSize;
	        r = nLen % m_nEPInSize;

	        for(i=0; i<n; i++)
	        {
	            w_nRet = connection.bulkTransfer(endpoint_IN, m_abyTransferBuf, m_nEPInSize, nTimeOut);
	            if (w_nRet != m_nEPInSize)
	                return false;
	            System.arraycopy(m_abyTransferBuf, 0, pBuf, i*m_nEPInSize, m_nEPInSize);
	        }

	        if (r > 0)
	        {
	            w_nRet = connection.bulkTransfer(endpoint_IN, m_abyTransferBuf, r, nTimeOut);
	            if (w_nRet != r)
	                return false;
	            System.arraycopy(m_abyTransferBuf, 0, pBuf, i*m_nEPInSize, r);
	        }
	        return true;
	    }
	}


