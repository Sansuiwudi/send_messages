package com.example.send_messages;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SendMessages extends AppCompatActivity {
    String phone;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_messages);
        Intent intent=getIntent();
        String name_phone=intent.getStringExtra("name_phone");
        Log.d("aaa",name_phone);
        String[] strings=name_phone.split("\n");
        Log.d("aaa",strings[0]);
        Log.d("aaa",strings[1]);

        phone=strings[1].replaceAll(" ","");
        TextView message=(TextView) findViewById(R.id.messages);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.READ_SMS }, 1);
        } else {
            message.setText(getSmsInPhone());
        }
    }

        @SuppressLint("LongLogTag")
        public String getSmsInPhone() {
            final String SMS_URI_ALL = "content://sms/";
            final String SMS_URI_INBOX = "content://sms/inbox";
            final String SMS_URI_SEND = "content://sms/sent";
            final String SMS_URI_DRAFT = "content://sms/draft";
            final String SMS_URI_OUTBOX = "content://sms/outbox";
            final String SMS_URI_FAILED = "content://sms/failed";
            final String SMS_URI_QUEUED = "content://sms/queued";

            StringBuilder smsBuilder = new StringBuilder();

            try {
                Uri uri = Uri.parse(SMS_URI_ALL);
                String[] projection = new String[] { "_id", "address", "person", "body", "date", "type" };
                Cursor cur = getContentResolver().query(uri, projection, "address=?", new String[]{phone}, "date desc");		// 获取手机内部短信
                //Cursor cur = getContentResolver().query(uri, projection, null, null, "date desc");		// 获取手机内部短信

                if (cur.moveToFirst()) {
                    int index_Address = cur.getColumnIndex("address");
                    int index_Person = cur.getColumnIndex("person");
                    int index_Body = cur.getColumnIndex("body");
                    int index_Date = cur.getColumnIndex("date");
                    int index_Type = cur.getColumnIndex("type");

                    do {
                        String strAddress = cur.getString(index_Address);
                        int intPerson = cur.getInt(index_Person);
                        String strbody = cur.getString(index_Body);
                        long longDate = cur.getLong(index_Date);
                        int intType = cur.getInt(index_Type);

                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                        Date d = new Date(longDate);
                        String strDate = dateFormat.format(d);

                        String strType = "";
                        if (intType == 1) {
                            strType = "接收";
                        } else if (intType == 2) {
                            strType = "发送";
                        } else {
                            strType = "null";
                        }

                        smsBuilder.append("[ ");
                        smsBuilder.append(strAddress + ", ");
                        smsBuilder.append(intPerson + ", ");
                        smsBuilder.append(strbody + ", ");
                        smsBuilder.append(strDate + ", ");
                        smsBuilder.append(strType);
                        smsBuilder.append(" ]\n\n");
                    } while (cur.moveToNext());

                    if (!cur.isClosed()) {
                        cur.close();
                        cur = null;
                    }
                } else {
                    smsBuilder.append("no result!");
                } // end if

                smsBuilder.append("getSmsInPhone has executed!");

            } catch (SQLiteException ex) {
                Log.d("SQLiteException in getSmsInPhone", ex.getMessage());
            }

            return smsBuilder.toString();
        }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getSmsInPhone();
                } else {
                    Toast.makeText(this, "You denied the permission", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }
    }