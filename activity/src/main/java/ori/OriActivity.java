package ori;

import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.Choreographer;
import android.widget.ScrollView;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.util.DisplayMetrics;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Rect;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Layout;
import android.text.TextPaint;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.StaticLayout;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;

import androidx.core.view.ViewCompat;
import androidx.appcompat.app.AppCompatActivity;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import java.util.Map;
import java.util.ArrayDeque;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.HashMap;
import java.lang.Long;
import java.io.ByteArrayInputStream;

public class OriActivity extends AppCompatActivity {
    DisplayMetrics metrics;
    OriGroup root;
    boolean isAnimating = false;
    long lastFrameTime = 0;

    Map<Long, View> views = new HashMap();
    Map<Long, TextLayout> textLayout = new HashMap();
    Map<Long, TextInputLayout> textInputLayout = new HashMap();
    Map<Long, ImageLayout> imageLayout = new HashMap();

    @Override
    public void onCreate(Bundle savedInstantanceState) {
        super.onCreate(savedInstantanceState);

        metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        root = new OriGroup(this);
        setContentView(root);

        main();
    }

    native void main();

    static {
        System.loadLibrary("native");
    }

    public void removeView(long id) {
        textLayout.remove(id);
        textInputLayout.remove(id);
        imageLayout.remove(id);

        queueUiTask(() -> {
            View view = views.remove(id);
            ViewGroup parent = (ViewGroup) view.getParent();

            if (parent != null) {
                parent.removeView(view);
            }
        });
    }

    /* ---------- UNITS ---------- */

    public int px(float logical) {
        return (int) Math.round(logical * (float) metrics.density);
    }

    public float lc(int px) {
        return px / (float) metrics.density;
    }

    /* ---------- WINDOW ---------- */

    public void windowSetContents(long contents) {
        queueUiTask(() -> {
            root.removeAllViews();
            root.addView(views.get(contents));
        });
    }

    public void windowSetContentLayout(float x, float y, float width, float height) {
        queueUiTask(() -> {
            OriGroup.LayoutParams lp = new OriGroup.LayoutParams(
                    px(width), px(height), px(x), px(y));

            root.getChildAt(0).setLayoutParams(lp);
        });
    }

    public int windowGetWidth() {
        return (int) Math.round(lc(metrics.widthPixels));
    }

    public int windowGetHeight() {
        return (int) Math.round(lc(metrics.heightPixels));
    }

    public void windowStartAnimating() {
        queueUiTask(() -> {
            if (!isAnimating) {
                isAnimating = true;
                Choreographer.getInstance()
                        .postFrameCallback(this::frameCallback);
            }
        });
    }

    public void windowStopAnimating() {
        isAnimating = false;
        lastFrameTime = 0;
    }

    void frameCallback(long frameTime) {
        if (!isAnimating)
            return;

        if (lastFrameTime == 0)
            lastFrameTime = frameTime;

        long duration = frameTime - lastFrameTime;
        lastFrameTime = frameTime;
        onAnimationFrame(duration);

        Choreographer.getInstance()
                .postFrameCallback(this::frameCallback);
    }

    static native void onAnimationFrame(long duration);

    /* ---------- GROUP ---------- */

    public void createGroup(long id) {
        queueUiTask(() -> {
            OriGroup view = new OriGroup(this);
            views.put(id, view);
        });
    }

    public void groupInsert(long id, int index, long child) {
        queueUiTask(() -> {
            OriGroup view = (OriGroup) views.get(id);
            view.addView(views.get(child), index);
        });
    }

    public void groupRemove(long id, int index) {
        queueUiTask(() -> {
            OriGroup view = (OriGroup) views.get(id);
            view.removeViewAt(index);
        });
    }

    public void groupSwap(long id, int indexA, int indexB) {
        queueUiTask(() -> {
            OriGroup view = (OriGroup) views.get(id);

            int first = Math.min(indexA, indexB);
            int last = Math.max(indexA, indexB);

            View firstView = view.getChildAt(first);
            View lastView = view.getChildAt(last);

            view.removeViewAt(last);
            view.removeViewAt(first);
            view.addView(lastView, first);
            view.addView(firstView, last);
        });
    }

    public void groupSetChildLayout(long id, int index,
            float x, float y,
            float width, float height) {
        queueUiTask(() -> {
            OriGroup view = (OriGroup) views.get(id);
            View child = view.getChildAt(index);

            OriGroup.LayoutParams lp = new OriGroup.LayoutParams(
                    px(width), px(height),
                    px(x), px(y));

            child.setLayoutParams(lp);
            child.layout(px(x), px(y), px(width), px(height));
        });
    }

    public void groupSetBackgroundColor(long id, float r, float g, float b, float a) {
        queueUiTask(() -> {
            OriGroup view = (OriGroup) views.get(id);

            int color = rgba(r, g, b, a);
            view.setBackgroundColor(color);
        });
    }

    public void groupSetCornerRadii(long id, float tl, float tr, float br, float bl) {
        queueUiTask(() -> {
            OriGroup view = (OriGroup) views.get(id);
            view.setCornerRadii(px(tl), px(tr), px(br), px(bl));
        });
    }

    public void groupSetBorderWidth(long id, float t, float r, float b, float l) {
        queueUiTask(() -> {
            OriGroup view = (OriGroup) views.get(id);
            view.setBorderWidth(px(t), px(r), px(b), px(l));
        });
    }

    public void groupSetBorderColor(long id, float r, float g, float b, float a) {
        queueUiTask(() -> {
            OriGroup view = (OriGroup) views.get(id);

            int color = rgba(r, g, b, a);
            view.setBorderColor(color);
        });
    }

    public void groupSetOverflow(long id, boolean visible) {
        queueUiTask(() -> {
            OriGroup view = (OriGroup) views.get(id);
            view.setOverflow(visible);
        });
    }

    public void groupSetShadow(long id,
            float r, float g, float b, float a,
            float dx, float dy,
            float blur, float spread) {
        queueUiTask(() -> {
            OriGroup view = (OriGroup) views.get(id);
            int color = rgba(r, g, b, a);
            view.setShadow(color, dx, dy, blur, spread);
        });
    }

    /* ---------- PRESSABLE ---------- */

    public void createPressable(long id) {
        queueUiTask(() -> {
            OriPressable view = new OriPressable(this, id);
            views.put(id, view);
        });
    }

    public void pressableSetContents(long id, long contents) {
        queueUiTask(() -> {
            OriPressable view = (OriPressable) views.get(id);
            view.removeAllViews();
            view.addView(views.get(contents));
        });
    }

    public void pressableSetContentSize(long id, float width, float height) {
        queueUiTask(() -> {
            OriPressable view = (OriPressable) views.get(id);
            OriPressable.LayoutParams lp = new OriPressable.LayoutParams(
                    px(width), px(height));

            view.getChildAt(0).setLayoutParams(lp);
        });
    }

    /* ---------- SCROLL ---------- */

    public void createScroll(long id) {
        queueUiTask(() -> {
            FrameLayout view = new FrameLayout(this);
            ScrollView scroll = new ScrollView(this);
            OriGroup group = new OriGroup(this);

            scroll.addView(group);
            view.addView(scroll);

            views.put(id, view);
        });
    }

    public void scrollSetContents(long id, long contents) {
        queueUiTask(() -> {
            FrameLayout view = (FrameLayout) views.get(id);
            ViewGroup scroll = (ViewGroup) view.getChildAt(0);
            OriGroup group = (OriGroup) scroll.getChildAt(0);
            group.removeAllViews();
            group.addView(views.get(contents));
        });
    }

    public void scrollSetContentSize(long id, float width, float height) {
        queueUiTask(() -> {
            FrameLayout view = (FrameLayout) views.get(id);
            ViewGroup scroll = (ViewGroup) view.getChildAt(0);
            OriGroup group = (OriGroup) scroll.getChildAt(0);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    px(width), px(height));

            group.setLayoutParams(lp);
        });
    }

    public void scrollSetContentLayout(long id,
            float x, float y,
            float width, float height) {
        queueUiTask(() -> {
            FrameLayout view = (FrameLayout) views.get(id);
            ViewGroup scroll = (ViewGroup) view.getChildAt(0);
            OriGroup group = (OriGroup) scroll.getChildAt(0);
            OriGroup.LayoutParams lp = new OriGroup.LayoutParams(
                    px(width), px(height), px(x), px(y));

            group.getChildAt(0).setLayoutParams(lp);
        });
    }

    public void scrollSetVertical(long id, boolean vertical) {
        queueUiTask(() -> {
            FrameLayout view = (FrameLayout) views.get(id);
            ViewGroup scroll = (ViewGroup) view.getChildAt(0);
            OriGroup group = (OriGroup) scroll.getChildAt(0);

            if (vertical && scroll instanceof HorizontalScrollView) {
                ScrollView newScroll = new ScrollView(this);
                scroll.removeView(group);
                newScroll.addView(group);

                view.removeAllViews();
                view.addView(newScroll);
            } else if (!vertical && scroll instanceof ScrollView) {
                HorizontalScrollView newScroll = new HorizontalScrollView(this);
                scroll.removeView(group);
                newScroll.addView(group);

                view.removeAllViews();
                view.addView(newScroll);
            }
        });
    }

    /* ---------- TRANSFORM ---------- */

    public void createTransform(long id) {
        queueUiTask(() -> {
            FrameLayout view = new FrameLayout(this);
            views.put(id, view);
        });
    }

    public void transformSetContents(long id, long contents) {
        queueUiTask(() -> {
            FrameLayout view = (FrameLayout) views.get(id);
            view.removeAllViews();
            view.addView(views.get(contents));
        });
    }

    public void transformSetTransform(long id,
            float width, float height,
            float x, float y,
            float angle,
            float sx, float sy) {
        queueUiTask(() -> {
            FrameLayout view = (FrameLayout) views.get(id);
            ViewCompat.setPivotX(view, width / 2.0f);
            ViewCompat.setPivotY(view, height / 2.0f);
            ViewCompat.setTranslationX(view, x);
            ViewCompat.setTranslationY(view, y);
            ViewCompat.setRotation(view, angle);
            ViewCompat.setScaleX(view, sx);
            ViewCompat.setScaleY(view, sy);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    px(width), px(height));
            view.getChildAt(0).setLayoutParams(lp);
        });
    }

    /* ---------- TEXT ---------- */

    public void createText(long id) {
        queueUiTask(() -> {
            TextView view = new TextView(this);
            views.put(id, view);
        });

        textLayout.put(id, new TextLayout());
    }

    public void textSetText(long id, String text, int wrap) {
        TextLayout layout = textLayout.get(id);
        layout.text = new SpannableString(text);
    }

    public void textSetSpan(long id,
            int start, int end,
            float size,
            String family,
            int weight,
            int stretch,
            boolean italic,
            boolean strikethrough,
            float r,
            float g,
            float b,
            float a) {
        TextLayout layout = textLayout.get(id);
        Typeface typeface = createTypeface(family, weight, italic);

        layout.text.setSpan(
                new OriTypefaceSpan(typeface),
                start, end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        layout.text.setSpan(
                new AbsoluteSizeSpan(px(size)),
                start, end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        if (strikethrough) {
            layout.text.setSpan(
                    new StrikethroughSpan(),
                    start, end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        int color = rgba(r, g, b, a);
        layout.text.setSpan(
                new ForegroundColorSpan(color),
                start, end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        queueUiTask(() -> {
            TextView view = (TextView) views.get(id);
            view.setText(layout.text);
        });
    }

    public float textMeasureWidth(long id, float maxWidth) {
        TextLayout layout = textLayout.get(id);

        StaticLayout staticLayout = new StaticLayout(
                layout.text,
                new TextPaint(),
                px(maxWidth),
                Layout.Alignment.ALIGN_NORMAL,
                1.0f,
                0.0f,
                true);

        float width = 0.0f;

        for (int i = 0; i < staticLayout.getLineCount(); i++) {
            width = Math.max(width, staticLayout.getLineWidth(i));
        }

        return width / metrics.density;
    }

    public float textMeasureHeight(long id, float maxWidth) {
        TextLayout layout = textLayout.get(id);

        StaticLayout staticLayout = new StaticLayout(
                layout.text,
                new TextPaint(),
                px(maxWidth),
                Layout.Alignment.ALIGN_NORMAL,
                1.0f,
                0.0f,
                true);

        return lc(staticLayout.getHeight());
    }

    static class TextLayout {
        public SpannableString text;
    }

    /* ---------- TEXT INPUT ---------- */

    public void createTextInput(long id) {
        queueUiTask(() -> {
            OriEditText view = new OriEditText(this, id);
            views.put(id, view);
        });

        textInputLayout.put(id, new TextInputLayout());
    }

    public void textInputSetSingleLine(long id, boolean singleline) {
        queueUiTask(() -> {
            OriEditText view = (OriEditText) views.get(id);
            view.setSingleLine(singleline);
        });
    }

    public void textInputSetText(long id, String text) {
        queueUiTask(() -> {
            OriEditText view = (OriEditText) views.get(id);
            view.setText(text);
        });
    }

    public void textInputSetFont(long id,
            float textSize,
            String family,
            int weight,
            int stretch,
            boolean italic,
            boolean strikethrough,
            float r,
            float g,
            float b,
            float a) {
        Typeface typeface = createTypeface(family, weight, italic);
        TextInputLayout layout = textInputLayout.get(id);
        layout.typeface = typeface;
        layout.textSize = textSize;

        queueUiTask(() -> {
            OriEditText view = (OriEditText) views.get(id);

            view.setTypeface(typeface);
            view.setTextSize(textSize);

            int color = rgba(r, g, b, a);
            view.setTextColor(color);
        });
    }

    public void textInputSetPlaceholderText(long id, String text) {
        queueUiTask(() -> {
            OriEditText view = (OriEditText) views.get(id);
            view.setPlaceholderText(text);
        });
    }

    public void textInputSetPlaceholderFont(long id,
            float textSize,
            String family,
            int weight,
            int stretch,
            boolean italic,
            boolean strikethrough,
            float r,
            float g,
            float b,
            float a) {
        Typeface typeface = createTypeface(family, weight, italic);
        TextInputLayout layout = textInputLayout.get(id);
        layout.placeholderTypeface = typeface;
        layout.placeholderTextSize = textSize;

        queueUiTask(() -> {
            OriEditText view = (OriEditText) views.get(id);

            int color = rgba(r, g, b, a);

            view.setPlaceholderFont(typeface, px(textSize), color);
        });
    }

    public float textInputMeasureHeight(long id) {
        TextInputLayout layout = textInputLayout.get(id);

        TextPaint paint = new TextPaint();
        paint.setTextSize(px(layout.textSize));
        paint.setTypeface(layout.typeface);

        TextPaint placeholderPaint = new TextPaint();
        placeholderPaint.setTextSize(px(layout.placeholderTextSize));
        placeholderPaint.setTypeface(layout.placeholderTypeface);

        var fm = paint.getFontMetrics();
        var pfm = placeholderPaint.getFontMetrics();

        float height = fm.descent - fm.ascent;
        float placeholderHeight = pfm.descent - pfm.ascent;

        EditText probe = new EditText(this);
        int padding = probe.getPaddingTop() + probe.getPaddingBottom();

        return (Math.max(height, placeholderHeight) + padding) / metrics.density;
    }

    static class TextInputLayout {
        public Typeface typeface;
        public float textSize;

        public Typeface placeholderTypeface;
        public float placeholderTextSize;
    }

    /* ---------- IMAGE ---------- */

    public void createImage(long id) {
        queueUiTask(() -> {
            OriImage image = new OriImage(this);
            views.put(id, image);
        });

        imageLayout.put(id, new ImageLayout());
    }

    public void imageLoadSvg(long id, byte[] data) {
        ImageLayout layout = imageLayout.get(id);

        try {
            ByteArrayInputStream stream = new ByteArrayInputStream(data);
            SVG svg = SVG.getFromInputStream(stream);

            layout.width = Math.max(svg.getDocumentWidth(), 0.0f);
            layout.height = Math.max(svg.getDocumentHeight(), 0.0f);

            queueUiTask(() -> {
                OriImage view = (OriImage) views.get(id);
                view.setSvg(svg);
            });
        } catch (SVGParseException e) {
            throw new RuntimeException(e);
        }
    }

    public void imageLoadBitmap(long id, byte[] data) {
        ImageLayout layout = imageLayout.get(id);

        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

        if (bitmap == null) {
            return;
        }

        layout.width = (float) bitmap.getWidth();
        layout.height = (float) bitmap.getHeight();

        queueUiTask(() -> {
            OriImage view = (OriImage) views.get(id);
            view.setBitmap(bitmap);
        });
    }

    public void imageSetTint(long id,
            boolean isSet,
            float r, float g, float b, float a) {
        queueUiTask(() -> {
            OriImage view = (OriImage) views.get(id);
            int color = rgba(r, g, b, a);
            view.setTint(color, isSet);
        });
    }

    public float imageGetWidth(long id) {
        ImageLayout layout = imageLayout.get(id);
        return layout.width;
    }

    public float imageGetHeight(long id) {
        ImageLayout layout = imageLayout.get(id);
        return layout.height;
    }

    static class ImageLayout {
        public float width;
        public float height;
    }

    /* ---------- HELPER ---------- */

    ArrayDeque<Runnable> uiTasks = new ArrayDeque<>();

    public void queueUiTask(Runnable task) {
        uiTasks.addLast(task);
    }

    public void runUiTasks() {
        runOnUiThread(() -> {
            root.suppressLayout(true);

            try {
                while (!uiTasks.isEmpty()) {
                    uiTasks.removeFirst().run();
                }
            } finally {
                root.suppressLayout(false);
            }
        });
    }

    public static int rgba(float r, float g, float b, float a) {
        a = (float) Math.pow(a, 0.45f);

        return Color.argb(
                (int) Math.round(a * 255.0f),
                (int) Math.round(r * a * 255.0f),
                (int) Math.round(g * a * 255.0f),
                (int) Math.round(b * a * 255.0f));
    }

    public static Typeface createTypeface(String family, int weight, boolean italic) {
        Typeface base = Typeface.create(family, Typeface.NORMAL);

        if (base == null) {
            base = Typeface.DEFAULT;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return Typeface.create(base, weight, italic);
        } else {
            int style = Typeface.NORMAL;

            if (weight >= 700 && !italic) {
                style = Typeface.BOLD;
            } else if (weight >= 700 && italic) {
                style = Typeface.BOLD_ITALIC;
            } else if (weight < 700 && italic) {
                style = Typeface.ITALIC;
            }

            return Typeface.create(base, style);
        }
    }
}
