

package pl.dock.widget.ui;

import android.animation.TimeInterpolator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

/**
 * Shared interpolators for Dock 2.0. Prefer predictability over springy excess.
 */
public final class DockMotion {

    public static final TimeInterpolator STANDARD = new AccelerateDecelerateInterpolator();
    public static final TimeInterpolator DECELERATE = new DecelerateInterpolator(1.5f);
    public static final TimeInterpolator EMPHASIZED = new DecelerateInterpolator(2f);
    public static final TimeInterpolator QUICK = new LinearInterpolator();
    public static final TimeInterpolator SOFT_OVERSHOOT = new OvershootInterpolator(0.6f);

    private DockMotion() {}
}




