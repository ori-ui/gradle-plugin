package ori;

import android.content.Context;
import android.widget.FrameLayout;
import android.view.MotionEvent;
import android.view.GestureDetector;

public class OriPressable extends FrameLayout {
    long id;

    boolean isPressed = false;

    public OriPressable(Context context, long id) {
        super(context);
        setClipChildren(false);
        setClipToPadding(false);

        this.id = id;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        boolean handled = false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isPressed = true;
                handled |= onPress(id, 0, x, y);
                break;

            case MotionEvent.ACTION_MOVE:
                if (x < 0.0 || y < 0.0 || x > getWidth() || y > getHeight()) {
                    if (isPressed) {
                        isPressed = false;
                        handled |= onPress(id, 2, x, y);
                    }
                }

                handled |= onMove(id, x, y);
                break;

            case MotionEvent.ACTION_UP:
                if (isPressed) {
                    isPressed = false;
                    handled |= onPress(id, 1, x, y);
                }

                break;

            default:
                break;
        }

        return handled;
    }

    static native boolean onPress(long id, int state, float x, float y);
    static native boolean onMove(long id, float x, float y);
}
