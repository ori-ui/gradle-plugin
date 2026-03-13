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
        this.id = id;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isPressed = true;
                onPress(id, 0);
                return true;

            case MotionEvent.ACTION_MOVE:
                if (event.getX() < 0.0
                        || event.getY() < 0.0
                        || event.getX() > getWidth()
                        || event.getY() > getHeight()) {
                    if (isPressed) {
                        isPressed = false;
                        onPress(id, 2);
                    }
                }

                return true;

            case MotionEvent.ACTION_UP:
                if (isPressed) {
                    isPressed = false;
                    onPress(id, 1);
                }

                return true;

            default:
                return false;
        }
    }

    static native boolean onPress(long id, int state);
}
