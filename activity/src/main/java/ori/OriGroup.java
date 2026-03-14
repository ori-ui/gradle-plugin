package ori;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Paint;

public class OriGroup extends ViewGroup {
    private int backgroundColor = 0;
    private int borderColor = 0;

    private float radiusTL = 0f;
    private float radiusTR = 0f;
    private float radiusBR = 0f;
    private float radiusBL = 0f;

    private float borderT = 0f;
    private float borderR = 0f;
    private float borderB = 0f;
    private float borderL = 0f;

    private Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Path backgroundPath = new Path();
    private Path borderPath = new Path();
    private Path clipPath = new Path();

    private boolean overflowVisible = false;

    public OriGroup(Context context) {
        super(context);
        setWillNotDraw(false);
    }

    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
        invalidate();
    }

    public void setCornerRadii(float tl, float tr, float br, float bl) {
        this.radiusTL = tl;
        this.radiusTR = tr;
        this.radiusBR = br;
        this.radiusBL = bl;
        invalidate();
    }

    public void setBorderWidth(float t, float r, float b, float l) {
        this.borderT = t;
        this.borderR = r;
        this.borderB = b;
        this.borderL = l;
        invalidate();
    }

    public void setBorderColor(int color) {
        this.borderColor = color;
        invalidate();
    }

    public void setOverflow(boolean visible) {
        this.overflowVisible = visible;
        setClipChildren(!visible);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int maxWidth = 0;
        int maxHeight = 0;

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();

            int childWidthSpec = View.MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
            int childHeightSpec = View.MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
            child.measure(childWidthSpec, childHeightSpec);

            maxWidth = Math.max(maxWidth, lp.x + lp.width);
            maxHeight = Math.max(maxHeight, lp.y + lp.height);
        }

        setMeasuredDimension(resolveSize(maxWidth, widthSpec),
                resolveSize(maxHeight, heightSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            child.layout(lp.x, lp.y, lp.x + lp.width, lp.y + lp.height);
        }
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(0, 0, 0, 0);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p.width, p.height, 0, 0);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();

        /* draw background */

        backgroundPaint.setColor(backgroundColor);
        backgroundPaint.setStyle(Paint.Style.FILL);

        backgroundPath.reset();
        backgroundPath.addRoundRect(
                0, 0,
                w, h,
                new float[] {
                        radiusTL, radiusTL,
                        radiusTR, radiusTR,
                        radiusBR, radiusBR,
                        radiusBL, radiusBL
                }, Path.Direction.CW);

        canvas.drawPath(backgroundPath, backgroundPaint);

        /* draw border */

        borderPaint.setColor(borderColor);
        borderPaint.setStyle(Paint.Style.FILL);

        borderPath.reset();
        borderPath.addRoundRect(
                0, 0,
                w, h,
                new float[] {
                        radiusTL, radiusTL,
                        radiusTR, radiusTR,
                        radiusBR, radiusBR,
                        radiusBL, radiusBL
                }, Path.Direction.CW);

        float tl = radiusTL - Math.max(borderT, borderL);
        float tr = radiusTR - Math.max(borderT, borderR);
        float br = radiusBR - Math.max(borderB, borderR);
        float bl = radiusBL - Math.max(borderB, borderL);

        borderPath.addRoundRect(
                borderL, borderT,
                w - borderR, h - borderB,
                new float[] {
                        tl, tl,
                        tr, tr,
                        br, br,
                        bl, bl
                }, Path.Direction.CCW);

        canvas.drawPath(borderPath, borderPaint);

        /* draw clip */


        if (!overflowVisible) {
            clipPath.reset();
            clipPath.addRoundRect(
                    borderL, borderT,
                    w - borderR, h - borderB,
                    new float[] {
                            tl, tl,
                            tr, tr,
                            br, br,
                            bl, bl
                    }, Path.Direction.CW);

            canvas.save();
            canvas.clipPath(clipPath);
        }

        super.onDraw(canvas);

        if (!overflowVisible) {
            canvas.restore();
        }
    }

    public static class LayoutParams extends ViewGroup.LayoutParams {
        public int x = 0;
        public int y = 0;

        public LayoutParams(int width, int height, int x, int y) {
            super(width, height);
            this.x = x;
            this.y = y;
        }
    }
}
