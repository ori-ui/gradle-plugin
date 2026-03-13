package ori;

import android.content.Context;
import android.view.View;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Picture;

import com.caverock.androidsvg.SVG;

public class OriImage extends View {
    SVG svg;
    Bitmap bitmap;
    Bitmap cache;

    Paint tintPaint;

    public OriImage(Context context) {
        super(context);
        setWillNotDraw(false);
    }

    public void setSvg(SVG svg) {
        this.svg = svg;
        this.bitmap = null;
        this.cache = null;
        invalidate();
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
        this.svg = null;
        this.cache = null;
        invalidate();
    }

    public void setTint(int color) {
        if (color == 0) {
            this.tintPaint = null;
        } else {
            this.tintPaint = new Paint();
            this.tintPaint.setColorFilter(
                    new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        }

        this.cache = null;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getWidth() == 0 || getHeight() == 0) {
            return;
        }

        if (cache == null
                || cache.getWidth() != getWidth()
                || cache.getHeight() != getHeight()) {
            cache = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            Canvas cacheCanvas = new Canvas(cache);

            int save = -1;

            if (tintPaint != null) {
                save = cacheCanvas.saveLayer(null, tintPaint);
            }

            if (svg != null) {
                svg.setDocumentWidth(getWidth());
                svg.setDocumentHeight(getHeight());
                svg.renderToCanvas(cacheCanvas);
            }

            if (bitmap != null) {
                cacheCanvas.drawBitmap(bitmap, 0, 0, null);
            }

            if (tintPaint != null) {
                cacheCanvas.restoreToCount(save);
            }
        }

        canvas.drawBitmap(cache, 0, 0, null);
    }
}
