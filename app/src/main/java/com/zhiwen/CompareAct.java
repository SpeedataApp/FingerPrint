package com.zhiwen;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mylibrary.FingerManger;
import com.mylibrary.inf.IFingerPrint;

import java.text.DecimalFormat;

public class CompareAct extends AppCompatActivity implements View.OnClickListener {
    Button btnCompare, btnTempleat;
    ImageView imageView1, imageView2;
    TextView tvMsg;
    IFingerPrint iFingerPrint;
    boolean template = true;
    byte[] template1;
    byte[] template2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compare);
        tvMsg = (TextView) findViewById(R.id.textview);
        imageView1 = (ImageView) findViewById(R.id.img_1);
        imageView2 = (ImageView) findViewById(R.id.img_2);
        btnCompare = (Button) findViewById(R.id.btn_bidui);
        btnTempleat = (Button) findViewById(R.id.btn_tezheng);
        btnCompare.setOnClickListener(this);
        btnTempleat.setOnClickListener(this);
        iFingerPrint = FingerManger.InitPrintIntance(CompareAct.this, CompareAct.this, handler);
        iFingerPrint.openReader();
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    tvMsg.setText((String) msg.obj);
                    break;
                case 3:
                    Bitmap bitmap = (Bitmap) msg.obj;
                    if (template) {
                        imageView1.setImageBitmap(bitmap);
                    } else {
                        imageView2.setImageBitmap(bitmap);
                    }

                    break;
                case 4:
                    if (template) {
                        template = false;
//                        template1 = new byte[1024];
                        template1 = (byte[]) msg.obj;
//                        for (int i = 0; i < LAPI.FPINFO_STD_MAX_SIZE; i++) {
//                           msg += String.format("%02x", template1[i]);
//                        }
                        Toast.makeText(CompareAct.this, "template1", Toast.LENGTH_SHORT).show();
                    } else {
                        template = true;
//                        template2 = new byte[1024];
                        template2 = (byte[]) msg.obj;

                        Toast.makeText(CompareAct.this, "template2", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case 5:

                    int mScore = (Integer) msg.obj;
                    String comparison = "";
//                    if (fingerStata == 3) {
//                        DecimalFormat formatting = new DecimalFormat("##.######");
//                        comparison = "Dissimilarity Score: " + String.valueOf(mScore) + ", False match rate: "
//                                + Double.valueOf(formatting.format((double) mScore / 0x7FFFFFFF)) + " (" + (mScore < (0x7FFFFFFF / 100000) ? "match" : "no match") + ")";
//                    } else if (fingerStata == 1) {
//                        comparison = String.format(getString(R.string.comparison_finger) + "%d", mScore);
//                    } else if (fingerStata == 2) {
//                        comparison = getString(R.string.comparison_finger) + mScore;
//                    }
                    DecimalFormat formatting = new DecimalFormat("##.######");
                    comparison = "Dissimilarity Score: " + String.valueOf(mScore) + ", False match rate: "
                            + Double.valueOf(formatting.format((double) mScore / 0x7FFFFFFF)) + " (" + (mScore < (0x7FFFFFFF / 100000) ? "match" : "no match") + ")";
                    tvMsg.setText(comparison);
                    break;
            }
        }
    };

    @Override
    public void onClick(View v) {
        if (v == btnCompare) {
            iFingerPrint.comparisonFinger(template1, template2);
        } else if (v == btnTempleat) {
            iFingerPrint.createTemplate();
        }
    }
}
