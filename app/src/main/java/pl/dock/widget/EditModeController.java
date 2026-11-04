package pl.dock.widget;

import java.util.ArrayList;

/**
 * Wspólny tryb edycji (ikony "wobble" + znaczki usuwania).
 * Jeden stan dla siatki aplikacji i strony widżetów, żeby nie kolidowały.
 */
public class EditModeController {

    public interface Listener {
        void onEditModeChanged(boolean editMode);
    }

    private static final EditModeController I = new EditModeController();

    public static EditModeController get() {
        return I;
    }

    private boolean editMode;
    private final ArrayList<Listener> ls = new ArrayList<Listener>();

    public boolean isEditMode() {
        return editMode;
    }

    public void setEditMode(boolean v) {
        if (editMode == v) return;
        editMode = v;
        for (Listener l : new ArrayList<Listener>(ls)) {
            try {
                l.onEditModeChanged(v);
            } catch (Throwable ignored) {
            }
        }
    }

    public void addListener(Listener l) {
        if (l != null && !ls.contains(l)) ls.add(l);
    }

    public void removeListener(Listener l) {
        ls.remove(l);
    }
}
