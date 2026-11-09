package pl.dock.widget;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.widget.Toast;

import java.util.List;

// Udostępnianie diagnostyki zawsze przez LocalSend (dopasowanie po nazwie / pakiecie)
public class Share {
    public static void viaLocalSend(Activity a, String subject, String text) {
        PackageManager pm = a.getPackageManager();
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, subject);
        send.putExtra(Intent.EXTRA_TEXT, text);
        try {
            List<ResolveInfo> l = pm.queryIntentActivities(send, 0);
            for (ResolveInfo ri : l) {
                String lab = ri.loadLabel(pm).toString().toLowerCase();
                String pkg = ri.activityInfo.packageName.toLowerCase();
                if (lab.contains("localsend") || pkg.contains("localsend")) {
                    send.setClassName(ri.activityInfo.packageName, ri.activityInfo.name);
                    a.startActivity(send);
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
        Toast.makeText(a, "LocalSend nie przyjmuje tego udostępnienia", Toast.LENGTH_SHORT).show();
    }
}





