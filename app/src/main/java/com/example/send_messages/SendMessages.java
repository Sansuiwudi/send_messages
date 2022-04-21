package com.example.send_messages;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SendMessages extends AppCompatActivity {
    String phone;

    private Button sendmessage;
    private EditText editText;

    public static final int TAKE_PHOTO = 1;

    public static final int CHOOSE_PHOTO = 2;

    private ImageView picture;

    private Uri imageUri;

    List<String> messageslist= new ArrayList<>();

    ArrayAdapter<String> adapter;

    ListView messagesView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_messages);
        Intent intent=getIntent();
        String name_phone=intent.getStringExtra("name_phone");
        String[] strings=name_phone.split("\n");

        phone=strings[1].replaceAll(" ","");
        messagesView=(ListView)findViewById(R.id.messages);
        adapter =new ArrayAdapter<String>(this,android.R.layout. simple_list_item_1,messageslist);
        messagesView.setAdapter(adapter);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.READ_SMS }, 1);
        } else {
            getSmsInPhone();
        }



        //发送短信功能
         editText=(EditText)findViewById(R.id.edit_text);
         sendmessage=(Button)findViewById(R.id.button_send);
        sendmessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(SendMessages.this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(SendMessages.this, new String[]{ Manifest.permission.SEND_SMS }, 3);
                } else {
                sendMessage();
                flush(view);
            }

            }
        });


        //拍照功能
        Button takePhoto = (Button) findViewById(R.id.take_photo);
        Button chooseFromAlbum = (Button) findViewById(R.id.choose_from_album);
        picture = (ImageView) findViewById(R.id.picture);
        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 创建File对象，用于存储拍照后的图片
                File outputImage = new File(getExternalCacheDir(), "output_image.jpg");
                try {
                    if (outputImage.exists()) {
                        outputImage.delete();
                    }
                    outputImage.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (Build.VERSION.SDK_INT < 24) {
                    imageUri = Uri.fromFile(outputImage);
                } else {
                    imageUri = FileProvider.getUriForFile(SendMessages.this, "com.example.cameraalbumtest.fileprovider", outputImage);
                }
                // 启动相机程序
                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(intent, TAKE_PHOTO);
            }
        });
        chooseFromAlbum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(SendMessages.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(SendMessages.this, new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE }, 2);
                } else {
                    openAlbum();
                }
            }
        });
    }
protected void sendMessage(){
    String etmessage=editText.getText().toString();

    try {
        SmsManager smsManager=SmsManager.getDefault();
        smsManager.sendTextMessage(phone,null,etmessage,null,null);
        Toast.makeText(getApplicationContext(), "发送成功",
                Toast.LENGTH_LONG).show();
    }catch (Exception e){
        Toast.makeText(getApplicationContext(),
                "发送失败，请重试！",
                Toast.LENGTH_LONG).show();
        e.printStackTrace();


    }
}

    private void openAlbum() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent, CHOOSE_PHOTO); // 打开相册
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    try {
                        // 将拍摄的照片显示出来
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                        picture.setImageBitmap(bitmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            case CHOOSE_PHOTO:
                if (resultCode == RESULT_OK) {
                    // 判断手机系统版本号
                    if (Build.VERSION.SDK_INT >= 19) {
                        // 4.4及以上系统使用这个方法处理图片
                        handleImageOnKitKat(data);
                    } else {
                        // 4.4以下系统使用这个方法处理图片
                        handleImageBeforeKitKat(data);
                    }
                }
                break;
            default:
                break;
        }
    }

    @TargetApi(19)
    private void handleImageOnKitKat(Intent data) {
        String imagePath = null;
        Uri uri = data.getData();
        Log.d("TAG", "handleImageOnKitKat: uri is " + uri);
        if (DocumentsContract.isDocumentUri(this, uri)) {
            // 如果是document类型的Uri，则通过document id处理
            String docId = DocumentsContract.getDocumentId(uri);
            if("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1]; // 解析出数字格式的id
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // 如果是content类型的Uri，则使用普通方式处理
            imagePath = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            // 如果是file类型的Uri，直接获取图片路径即可
            imagePath = uri.getPath();
        }
        displayImage(imagePath); // 根据图片路径显示图片
    }

    private void handleImageBeforeKitKat(Intent data) {
        Uri uri = data.getData();
        String imagePath = getImagePath(uri, null);
        displayImage(imagePath);
    }

    @SuppressLint("Range")
    private String getImagePath(Uri uri, String selection) {
        String path = null;
        // 通过Uri和selection来获取真实的图片路径
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    private void displayImage(String imagePath) {
        if (imagePath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            picture.setImageBitmap(bitmap);
        } else {
            Toast.makeText(this, "failed to get image", Toast.LENGTH_SHORT).show();
        }
    }

        @SuppressLint("LongLogTag")
        public void getSmsInPhone() {
            final String SMS_URI_ALL = "content://sms/";

            Cursor cur = null;

            try {
                Uri uri = Uri.parse(SMS_URI_ALL);
                String[] projection = new String[] { "_id", "address", "person", "body", "date", "type" };
                 cur = getContentResolver().query(uri, projection, "address=?", new String[]{phone}, "date desc");		// 获取手机内部短信
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
                            strType = "已接收";
                        } else if (intType == 2) {
                            strType = "已发送";
                        } else {
                            strType = "null";
                        }

                        messageslist.add("[(电话号码："+strAddress+" 时间："+strDate+")\n"
                        +"短信内容："+strbody+"\n"+strType+"]\n");
                    } while (cur.moveToNext());

                    if (!cur.isClosed()) {
                        cur.close();
                        cur = null;
                    }
                } else {
                    messageslist.add("暂无内容");
                } // end if

                messageslist.add("以上为全部内容");

            } catch (SQLiteException ex) {
                Log.d("SQLiteException in getSmsInPhone", ex.getMessage());
            }

        }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getSmsInPhone();
                }
                else {
                    Toast.makeText(this, "You denied the permission", Toast.LENGTH_SHORT).show();
                }
                break;
            case 2:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openAlbum();
                } else {
                    Toast.makeText(this, "You denied the permission", Toast.LENGTH_SHORT).show();
                }
                break;
            case 3:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    sendMessage();
                } else {
                    Toast.makeText(this, "You denied the permission", Toast.LENGTH_SHORT).show();
                }
                break;

            default:
        }
    }
    public void flush(View view) {
        finish();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
    }