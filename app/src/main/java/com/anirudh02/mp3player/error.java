package com.anirudh02.mp3player;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class error extends AppCompatActivity {
    Button storageCheck;
    boolean permTrack;
    String mp3Path = Environment.getExternalStorageDirectory().getPath() + "/Download/";
    @Override
    //If storage access permissions have been granted (and the issue is the lack of MP3 files in the
    //Download folder, the button to request Storage access will become a colorful textbox that conveys this (as per the check Perm method).
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Error");
        setContentView(R.layout.activity_error);
        permTrack = false;
        storageCheck = (Button) findViewById(R.id.storageperm);
        checkPerm();

    }
    //When the user presses the "Provide Access to Storage" button, one of two things can happen -
    //(1) If the user had previously pressed the button, checked the "Don't ask again" box when asked to give
    //permission, and then pressed deny, they will receive a pop-up message letting them know they have to
    //give the app permission to access their device storage in the settings app. They can press
    //a button to be redirected to the settings app or to close the message.
    //(2) If the user had never before pressed the button, had pressed
    // the button but had not checked the "Don't ask again" box before pressing deny, had not
    //manually allowed the app to access device storage in settings, and/or if they had reset/reinstalled
    //the app, they will be asked to give the app permission to device access storage from within this app itself.
    //If the app already has access to device storage, the button won't be visible at all
    public void requestperm(View view) {
        String p1 = Manifest.permission.READ_EXTERNAL_STORAGE;
        String p2 = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        int permissionReadStorage = ContextCompat.checkSelfPermission(this, p1);
        int permissionWriteStorage = ContextCompat.checkSelfPermission(this, p2);
        ArrayList<String> permissions = new ArrayList<>();
        if(permissionReadStorage != PackageManager.PERMISSION_GRANTED) {
            permissions.add(p1);
        }
        if(permissionWriteStorage != PackageManager.PERMISSION_GRANTED) {
            permissions.add(p2);
        }
        if(permissions.size() > 0) {
            String[] finalPerms = new String[permissions.size()];
            for(int i = 0; i < permissions.size(); i++) {
                finalPerms[i] = permissions.get(i);
            }
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                AlertDialog.Builder perms = new AlertDialog.Builder(error.this);
                perms.setMessage("Please allow this app to access the storage of this device to use"
                                + " the MP3 Player. You can do this in your device settings in"
                                + " the App Permissions section for this app's settings.");
                perms.setCancelable(true);
                perms.setPositiveButton(
                        "Go to Settings",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                i.setData(uri);
                                startActivity(i);
                                dialog.cancel();
                            }
                        }
                    );
                perms.setNegativeButton(
                        "No",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        }
                    );
                AlertDialog showPerms = perms.create();
                showPerms.show();
            } else {
                ActivityCompat.requestPermissions(this, finalPerms, 8080);
            }
        }
    }
    //If the app requested permissions to device storage from within this app, this method will be run
    //immediately after the user accepted or denied permission. If permission was granted,
    //the app will be restarted (and if MP3 files are in the Download folder, the MP3 Player will show up
    //rather than this page).
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (results[0] == PackageManager.PERMISSION_GRANTED) {
            restart(null);
        }
    }
    //This method will be called to check whether or not permissions have been granted for device storage access. If they have been granted, the user
    //will be notified that the issue is not with lack of storage permissions, but rather a lack of mp3 files in the Download folder.
    public void checkPerm() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            storageCheck.setText("Storage Access has been granted!\nPlease upload mp3 files (with the .mp3 file extension) to the Download folder of this device.");
            storageCheck.setClickable(false);
            permTrack = true;
            if(permTrack) {
                File directory = new File(Environment.getExternalStorageDirectory() + "/Download/");
                if (!directory.exists()) {
                    directory.mkdirs();
                } else {
                    File[] listFiles = new File(mp3Path).listFiles();
                    boolean isMp3 = false;
                    if(listFiles != null) {
                        String fileName, extName;
                        for (File file : listFiles) {
                            fileName = file.getName();
                            extName = fileName.substring(fileName.length() - 3);
                            if (extName.equals((String) "mp3"))
                                isMp3 = true;
                        }
                    }
                    if(isMp3) {
                        restart(null);
                    }
                }
            }
        }
    }
    //This method will be called to restart the app.
    public void restart(View view) {
        Intent i = new Intent(getApplicationContext(), MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }
    //If the user was previously in the Settings app to manually change the permissions (or anywhere other than this app) while keeping this
    //app in the background, this method will execute once this app is back in the foreground.
    @Override
    public void onResume() {
        super.onResume();
        checkPerm();
    }
}