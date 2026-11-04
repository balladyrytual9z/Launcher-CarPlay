
package pl.dock.widget;

import android.app.Activity;
import android.app.Dialog;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/** Panel ostatnio używanych aplikacji (jak iOS app switcher lite). */
public final class RecentAppsPopup {

    public static void show(final Activity a, final List<AppEntry> recents) {
        boolean night = Look.night(a);
        final Dialog[] ref = new Dialog[1];

        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.dp(a, 20), Ui.dp(a, 16), Ui.dp(a, 20), Ui.dp(a, 16));

        TextView title = Popups.text(a, "Ostatnio używane", 20, Popups.txt(night), true);
        col.addView(title);

        ScrollView sc = new ScrollView(a);
        LinearLayout list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        sc.addView(list);
        int h = Math.min(Ui.dp(a, 560), Math.max(Ui.dp(a, 80), recents.size() * Ui.dp(a, 62)));
        col.addView(sc, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h));

        for (final AppEntry e : recents) {
            LinearLayout row = new LinearLayout(a);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setMinimumHeight(Ui.dp(a, 62));
            row.setPadding(Ui.dp(a, 8), Ui.dp(a, 6), Ui.dp(a, 8), Ui.dp(a, 6));

            ImageView icon = new ImageView(a);
            icon.setImageBitmap(Icons.forEntry(a, e));
            row.addView(icon, new LinearLayout.LayoutParams(Ui.dp(a, 44), Ui.dp(a, 44)));

            View gap = new View(a);
            row.addView(gap, new LinearLayout.LayoutParams(Ui.dp(a, 14), 1));

            TextView label = Popups.text(a, appLabel(a, e.pkg), 18, Popups.txt(night), false);
            row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (ref[0] != null) ref[0].dismiss();
                    try {
                        android.content.Intent i = a.getPackageManager()
                                .getLaunchIntentForPackage(e.pkg);
                        if (i != null) {
                            Prefs.recordPkg(a, e.pkg);
                            i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                            a.startActivity(i);
                            a.overridePendingTransition(R.anim.cp_push_in, R.anim.cp_push_out);
                        }
                    } catch (Throwable ignored) {}
                }
            });
            list.addView(row);
        }

        if (recents.isEmpty()) {
            TextView empty = Popups.text(a, "Brak ostatnio używanych aplikacji", 15,
                    Popups.sub(night), false);
            empty.setPadding(0, Ui.dp(a, 24), 0, Ui.dp(a, 24));
            list.addView(empty);
        }

        ref[0] = Popups.panel(a, col, 520, Gravity.CENTER);
        ref[0].show();
    }

    private static String appLabel(Activity a, String pkg) {
        try {
            return a.getPackageManager().getApplicationLabel(
                    a.getPackageManager().getApplicationInfo(pkg, 0)).toString();
        } catch (Throwable t) {
            return pkg;
        }
    }

    private RecentAppsPopup() {}
}



