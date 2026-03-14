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

    Paint tintPaint;

    public OriImage(Context context) {
        super(context);
        setWillNotDraw(false);
    }

    public void setSvg(SVG svg) {
        this.svg = svg;
        this.bitmap = null;
        invalidate();
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
        this.svg = null;
        invalidate();
    }

    public void setTint(int color, boolean isSet) {
        if (isSet) {
            this.tintPaint = new Paint();
            this.tintPaint.setColorFilter(
                    new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        } else {
            this.tintPaint = null;
        }

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getWidth() == 0 || getHeight() == 0) {
            return;
        }

        if (svg != null) {
            if (bitmap == null
                    || bitmap.getWidth() != getWidth()
                    || bitmap.getHeight() != getHeight()) {
                bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
                Canvas cacheCanvas = new Canvas(bitmap);

                if (svg != null) {
                    svg.setDocumentWidth(getWidth());
                    svg.setDocumentHeight(getHeight());
                    svg.renderToCanvas(cacheCanvas);
                }
            }
        }

        if (tintPaint != null) {
            canvas.drawBitmap(bitmap, 0, 0, tintPaint);
        } else {
            canvas.drawBitmap(bitmap, 0, 0, null);
        }
    }
}
