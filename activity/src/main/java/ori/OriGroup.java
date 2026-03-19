package ori;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Paint;
import android.graphics.Bitmap;
import android.graphics.Outline;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

import androidx.core.view.ViewCompat;

public class OriGroup extends ViewGroup {
    private float radiusTL = 0.0f;
    private float radiusTR = 0.0f;
    private float radiusBR = 0.0f;
    private float radiusBL = 0.0f;

    private float borderT = 0.0f;
    private float borderR = 0.0f;
    private float borderB = 0.0f;
    private float borderL = 0.0f;

    private float shadowOffsetX = 0.0f;
    private float shadowOffsetY = 0.0f;
    private float shadowRadius = 0.0f;
    private float shadowSpread = 0.0f;

    private Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private boolean drawBackground = false;
    private boolean drawBorder = false;
    private boolean drawShadow = false;

    private Path backgroundPath = new Path();
    private Path borderPath = new Path();
    private Path shadowPath = new Path();
    private Path clipPath = new Path();

    private Bitmap shadowBitmap;

    private boolean overflowVisible = false;

    public OriGroup(Context context) {
        super(context);
        setWillNotDraw(false);
        setClipChildren(false);
        setClipToPadding(false);

        backgroundPaint.setColor(0);
        borderPaint.setColor(0);
    }

    public void setBackgroundColor(int color) {
        backgroundPaint.setColor(color);
        drawBackground = color != 0;
        invalidate();
    }

    public void setCornerRadii(float tl, float tr, float br, float bl) {
        radiusTL = tl;
        radiusTR = tr;
        radiusBR = br;
        radiusBL = bl;

        computePaths(getWidth(), getHeight());

        invalidate();
    }

    public void setBorderWidth(float t, float r, float b, float l) {
        borderT = t;
        borderR = r;
        borderB = b;
        borderL = l;

        computePaths(getWidth(), getHeight());

        invalidate();
    }

    public void setBorderColor(int color) {
        borderPaint.setColor(color);
        drawBorder = color != 0;
        invalidate();
    }

    public void setOverflow(boolean visible) {
        overflowVisible = visible;
        invalidate();
    }

    public void setShadow(
            int color,
            float offsetX,
            float offsetY,
            float radius,
            float spread) {
        drawShadow = color != 0;
        if (!drawShadow) return;

        shadowPaint.setColor(color);
        shadowOffsetX = offsetX;
        shadowOffsetY = offsetY;
        shadowRadius = radius;
        shadowSpread = spread;

        generateShadow(getWidth(), getHeight(), true);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int maxWidth = 0;
        int maxHeight = 0;

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();

            int childWidthSpec = View.MeasureSpec.makeMeasureSpec(lp.width, View.MeasureSpec.EXACTLY);
            int childHeightSpec = View.MeasureSpec.makeMeasureSpec(lp.height, View.MeasureSpec.EXACTLY);
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

    public static class LayoutParams extends ViewGroup.LayoutParams {
        public int x = 0;
        public int y = 0;

        public LayoutParams(int width, int height, int x, int y) {
            super(width, height);
            this.x = x;
            this.y = y;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        computePaths(w, h);

        if (drawShadow) {
            generateShadow(w, h, false);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (drawShadow) {
            drawShadow(canvas);
        }

        if (drawBackground) {
            drawBackground(canvas);
        }

        if (drawBorder) {
            canvas.drawPath(borderPath, borderPaint);
        }
    }

    private void drawBackground(Canvas canvas) {
        // if there are no corners, fill a rect
        if (radiusTL == 0 && radiusTR == 0 && radiusBR == 0 && radiusBL == 0) {
            RectF rect = new RectF(0, 0, getWidth(), getHeight());
            canvas.drawRect(rect, backgroundPaint);
            return;
        }

        // if all the corners are the same, use draw round rect
        if (radiusTL == radiusTR && radiusTL == radiusBL && radiusTL == radiusBR) {
            RectF rect = new RectF(0, 0, getWidth(), getHeight());
            canvas.drawRoundRect(rect, radiusTL, radiusTL, backgroundPaint);
            return;
        }

        // otherwise draw the path
        canvas.drawPath(backgroundPath, backgroundPaint);
    }

    private void drawShadow(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();

        int sw = shadowBitmap.getWidth();
        int sh = shadowBitmap.getHeight();

        int padding = (int) (shadowSpread + shadowRadius);

        Rect srcTL = new Rect(0     , 0     , sw / 2, sh / 2);
        Rect srcTR = new Rect(sw / 2, 0     , sw,     sh / 2);
        Rect srcBL = new Rect(0     , sw / 2, sw / 2, sh    );
        Rect srcBR = new Rect(sw / 2, sw / 2, sw    , sh    );

        Rect srcT = new Rect(sw / 2 - 1, 0         , sw / 2 + 1, sh / 2    );
        Rect srcB = new Rect(sw / 2 - 1, sh / 2    , sw / 2 + 1, sh        );
        Rect srcL = new Rect(0         , sh / 2 - 1, sw / 2    , sh / 2 + 1);
        Rect srcR = new Rect(sw / 2    , sh / 2 - 1, sw        , sh / 2 + 1);

        Rect srcC = new Rect(sw / 2 - 1, sh / 2 - 1, sw / 2 + 1, sh / 2 + 1);

        Rect dst = new Rect(
                (int) shadowOffsetX - padding,
                (int) shadowOffsetY - padding,
                getWidth() + padding + (int) shadowOffsetX,
                getHeight() + padding + (int) shadowOffsetY);

        Rect dstTL = new Rect(dst.left          , dst.top            , dst.left + sw / 2, dst.top + sh / 2);
        Rect dstTR = new Rect(dst.right - sw / 2, dst.top            , dst.right        , dst.top + sh / 2);
        Rect dstBL = new Rect(dst.left          , dst.bottom - sh / 2, dst.left + sw / 2, dst.bottom      );
        Rect dstBR = new Rect(dst.right - sw / 2, dst.bottom - sh / 2, dst.right        , dst.bottom      );

        Rect dstT = new Rect(
                dst.left + sw / 2,
                dst.top,
                dst.right - sw / 2,
                dst.top + sh / 2);

        Rect dstB = new Rect(
                dst.left + sw / 2,
                dst.bottom - sh / 2,
                dst.right - sw / 2,
                dst.bottom);

        Rect dstL = new Rect(
                dst.left,
                dst.top + sh / 2,
                dst.left + sw / 2,
                dst.bottom - sh / 2);

        Rect dstR = new Rect(
                dst.right - sw / 2,
                dst.top + sh / 2,
                dst.right,
                dst.bottom - sh / 2);

        Rect dstC = new Rect(
                dst.left + sw / 2,
                dst.top + sh / 2,
                dst.right - sw / 2,
                dst.bottom - sh / 2);

        canvas.drawBitmap(shadowBitmap, srcTL, dstTL, null);
        canvas.drawBitmap(shadowBitmap, srcTR, dstTR, null);
        canvas.drawBitmap(shadowBitmap, srcBL, dstBL, null);
        canvas.drawBitmap(shadowBitmap, srcBR, dstBR, null);

        canvas.drawBitmap(shadowBitmap, srcT, dstT, null);
        canvas.drawBitmap(shadowBitmap, srcB, dstB, null);
        canvas.drawBitmap(shadowBitmap, srcL, dstL, null);
        canvas.drawBitmap(shadowBitmap, srcR, dstR, null);

        float r = Math.max(
                Math.max(radiusTL, radiusTR),
                Math.max(radiusBL, radiusBR));

        if (dstC.left < r / 2
                || dstC.top < r / 2
                || dstC.right > getWidth() - r / 2
                || dstC.bottom > getHeight() - r / 2)
        {
            canvas.drawBitmap(shadowBitmap, srcC, dstC, null);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (!overflowVisible) {
            canvas.clipPath(clipPath);
        }

        super.dispatchDraw(canvas);
    }

    private void computePaths(int w, int h) {
        computeBackgroundPath(w, h);
        computeBorderPath(w, h);
        computeClipPath(w, h);
    }

    private void computeBackgroundPath(int w, int h) {
        if (!drawBackground) {
            return;
        }

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
    }

    private void computeBorderPath(int w, int h) {
        if (!drawBorder) {
            return;
        }

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
    }

    private void computeClipPath(int w, int h) {
        if (overflowVisible) {
            return;
        }

        float tl = radiusTL - Math.max(borderT, borderL);
        float tr = radiusTR - Math.max(borderT, borderR);
        float br = radiusBR - Math.max(borderB, borderR);
        float bl = radiusBL - Math.max(borderB, borderL);

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
    }

    private void generateShadow(int w, int h, boolean force) {
        float radius = Math.max(
                Math.max(radiusTL, radiusTR),
                Math.max(radiusBL, radiusBR));

        radius = Math.min(radius, Math.min(w, h) / 2.0f);

        int size = (int) (radius * 2.0f);
        int padding = (int) (shadowRadius + shadowSpread) * 2;

        if (size + padding <= 0) {
            return;
        }

        if (shadowBitmap != null && !force
                && shadowBitmap.getWidth() == size + padding
                && shadowBitmap.getHeight() == size + padding) {
            return;
        }

        shadowBitmap = Bitmap.createBitmap(
                size + padding, size + padding,
                Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(shadowBitmap);

        shadowPath.reset();
        shadowPath.addRoundRect(
                -shadowSpread, -shadowSpread,
                size + shadowSpread, size + shadowSpread,
                new float[] {
                        radiusTL, radiusTL,
                        radiusTR, radiusTR,
                        radiusBR, radiusBR,
                        radiusBL, radiusBL
                }, Path.Direction.CW);

        canvas.translate(shadowRadius + shadowSpread, shadowRadius + shadowSpread);
        canvas.drawPath(shadowPath, shadowPaint);

        shadowBitmap = blurRenderScript(shadowBitmap);
    }

    @SuppressWarnings("deprecation")
    private Bitmap blurRenderScript(Bitmap image) {
        if (shadowRadius <= 0.0f) return image;

        float radius = (float) Math.min(shadowRadius, 25.0f);
        float scale = shadowRadius / radius;

        Bitmap input = Bitmap.createScaledBitmap(
                image,
                (int) (image.getWidth() / scale),
                (int) (image.getHeight() / scale),
                true);

        Bitmap output = Bitmap.createBitmap(
                input.getWidth(),
                input.getHeight(),
                Bitmap.Config.ARGB_8888);

        RenderScript rs = RenderScript.create(getContext());

        Allocation inputAlloc = Allocation.createFromBitmap(rs, input);
        Allocation outputAlloc = Allocation.createFromBitmap(rs, output);

        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        blurScript.setRadius(radius);

        blurScript.setInput(inputAlloc);
        blurScript.forEach(outputAlloc);

        outputAlloc.copyTo(output);

        inputAlloc.destroy();
        outputAlloc.destroy();
        blurScript.destroy();
        rs.destroy();

        return Bitmap.createScaledBitmap(
                output,
                image.getWidth(),
                image.getHeight(),
                true);
    }
}
