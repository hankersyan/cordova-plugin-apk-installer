package io.hankersyan.cordova.installer;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;

import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class echoes a string called from JavaScript.
 */
public class Installer extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("install")) {
            String message = args.getString(0);
            this.install(message, callbackContext);
            return true;
        }
        return false;
    }

    private void install(String message, CallbackContext callbackContext) {
        if (TextUtils.isEmpty(message)) {
            callbackContext.error("need a file.");
            return;
        }
        if (!message.startsWith(Environment.getExternalStorageDirectory().toString())) {
            System.out.println("the input file is not in sdcard folder. \nmaybe access need permission.");
        }
        //Uri uri = message.startsWith("file:///") ? Uri.parse(message) : Uri.fromFile(new File(message));
        Uri uri = message.startsWith("file:///") ? Uri.parse(message) : Uri.fromFile(new File(message));
        if (message.startsWith("file:///")) {
            message = message.replace("file:///", "/");
        }
        File apkFile = new File(message);
        if (!apkFile.exists()) {
            callbackContext.error("File NOT exists." + message);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri apkUri = FileProvider.getUriForFile(cordova.getActivity(), cordova.getActivity().getPackageName() + ".fileprovider", apkFile);
            Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            cordova.getActivity().grantUriPermission(cordova.getActivity().getPackageName(), apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            cordova.getActivity().startActivity(intent);
        } else {
            File dir = new File(Environment.getExternalStorageDirectory() + "/temp");
            if (!dir.isDirectory()) {
                dir.mkdirs();
            }
            File extFile = new File(Environment.getExternalStorageDirectory() + "/temp/" + apkFile.getName());
            try {
                if (!extFile.exists()) {
                    extFile.createNewFile();
                }
                copy(apkFile, extFile);
            } catch (IOException e) {
                callbackContext.error("Failed to copy file to sdcard, " + extFile);
            }
            Uri apkUri = Uri.fromFile(extFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            cordova.getActivity().startActivity(intent);
        }
    }

    public static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        try {
            OutputStream out = new FileOutputStream(dst);
            try {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }
}
