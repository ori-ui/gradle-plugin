package ori;

import android.content.Context;
import android.widget.FrameLayout;
import android.view.MotionEvent;
import android.view.GestureDetector;

public class OriPressable extends FrameLayout {
    long id;

    OriActivity activity;

    boolean isPressed = false;
    boolean isTransparent = false;

    public OriPressable(OriActivity context, long id) {
        super(context);

        setClipChildren(false);
        setClipToPadding(false);

        this.id = id;
        this.activity = context;
    }

    public void setTransparent(boolean isTransparent) {
        this.isTransparent = isTransparent;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        float logicalX = x / (float) activity.metrics.density;
        float logicalY = y / (float) activity.metrics.density;

        boolean handled = false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isPressed = true;
                handled |= onPress(id, 0, logicalX, logicalY);
                break;

            case MotionEvent.ACTION_MOVE:
                if (x < 0.0 || y < 0.0 || x > getWidth() || y > getHeight()) {
                    if (isPressed) {
                        isPressed = false;
                        onPress(id, 2, logicalX, logicalY);
                    }
                }

                onMove(id, logicalX, logicalY);
                break;

            case MotionEvent.ACTION_UP:
                if (isPressed) {
                    isPressed = false;
                    onPress(id, 1, logicalX, logicalY);
                }

                break;

            default:
                break;
        }

        return handled && !isTransparent;
    }

    static native boolean onPress(long id, int state, float x, float y);
    static native boolean onMove(long id, float x, float y);
}
