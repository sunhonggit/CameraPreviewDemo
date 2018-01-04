package com.skyworth.camerapreview.demo.camerapreviewdemo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class PermissionAcitivity extends Activity {

    public static int PERMISSION_REQ = 0x123456;

    private String[] mPermission = new String[] {
            Manifest.permission.CAMERA
    };

    private List<String> mRequestPermission = new ArrayList<String>();

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
    ;@Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            for (String one : mPermission) {
                if (PackageManager.PERMISSION_GRANTED != this.checkPermission(one, Process.myPid(), Process.myUid())) {
                    mRequestPermission.add(one);
                }
            }
            if (!mRequestPermission.isEmpty()) {
                this.requestPermissions(mRequestPermission.toArray(new String[mRequestPermission.size()]), PERMISSION_REQ);
                return ;
            }
        }
        startActiviy();
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions,  int[] grantResults) {
        // 版本兼容
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            return;
        }
        if (requestCode == PERMISSION_REQ) {
            for (int i = 0; i < grantResults.length; i++) {
                for (String one : mPermission) {
                    Log.d("sunhong", "permission " + one + " result:" + grantResults[i]);
                    if (permissions[i].equals(one) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        mRequestPermission.remove(one);
                    }
                }
            }
            startActiviy();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PERMISSION_REQ) {
            if (resultCode == 0) {
                this.finish();
            }
        }
    }

    public void startActiviy() {
        if (mRequestPermission.isEmpty()) {
            Intent home = new Intent(PermissionAcitivity.this, MainActivity.class);
            startActivity(home);
            PermissionAcitivity.this.finish();
        } else {
            Toast.makeText(this, "PERMISSION DENIED!", Toast.LENGTH_LONG).show();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    PermissionAcitivity.this.finish();
                }
            }, 3000);
        }
    }
}
