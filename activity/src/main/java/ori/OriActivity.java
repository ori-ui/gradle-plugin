package ori;

import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.Choreographer;
import android.view.WindowInsets;
import android.widget.ScrollView;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.util.DisplayMetrics;
import android.util.TypedValue;
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
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.graphics.Insets;
import androidx.window.layout.WindowMetrics;
import androidx.window.layout.WindowMetricsCalculator;
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
    private DisplayMetrics metrics;
    private WindowMetrics windowMetrics;
    private OriGroup root;
    private boolean isAnimating = false;
    private long lastFrameTime = 0;

    private int statusBarColor = 0;
    private int navigationBarColor = 0;

    private Map<Long, View> views = new HashMap<>();
    private Map<Long, TextLayout> textLayout = new HashMap<>();
    private Map<Long, TextInputLayout> textInputLayout = new HashMap<>();
    private Map<Long, ImageLayout> imageLayout = new HashMap<>();

    @Override
    @SuppressWarnings("deprecation")
    public void onCreate(Bundle savedInstantanceState) {
        super.onCreate(savedInstantanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        metrics = getResources().getDisplayMetrics();

        windowMetrics = WindowMetricsCalculator.getOrCreate()
            .computeCurrentWindowMetrics(this);

        TypedValue value = new TypedValue();

        getTheme().resolveAttribute(android.R.attr.statusBarColor, value, true);
        statusBarColor = value.data;

        getTheme().resolveAttribute(android.R.attr.navigationBarColor, value, true);
        navigationBarColor = value.data;

        root = new OriGroup(this);
        setContentView(root);

        main();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onResume() {
        super.onResume();

        getWindow().setStatusBarColor(statusBarColor);
        getWindow().setNavigationBarColor(navigationBarColor);
        getWindow().getDecorView().post(this::attachInsetsListener);
    }

    void attachInsetsListener() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        ViewCompat.setOnApplyWindowInsetsListener(
                getWindow().getDecorView(), 
                (view, insets) -> {
                    Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                    Insets gestures = insets.getInsets(WindowInsets.Type.systemGestures());
                    Insets cutout = insets.getInsets(WindowInsets.Type.displayCutout());

                    float top = Math.max(bars.top, cutout.top);
                    float bottom = Math.max(bars.bottom, gestures.bottom);

                    float left = Math.max(bars.left, Math.max(gestures.left, cutout.left));
                    float right = Math.max(bars.right, Math.max(gestures.right, cutout.right));

                    onInsetsChanged(
                            top / metrics.density,
                            right / metrics.density,
                            bottom / metrics.density,
                            left / metrics.density);

                    return insets;
                });
    }

    native void main();

    static native void onInsetsChanged(float top, float right, float bottom, float left);

    static {
        System.loadLibrary("native");
    }

    private void removeView(long id) {
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

    private int px(float logical) {
        return (int) Math.floor(logical * metrics.density);
    }

    private float lc(int px) {
        return px / (float) metrics.density;
    }

    /* ---------- VIEW ---------- */

    private void viewSetHardwareLayer(long id, boolean enabled) {
        queueUiTask(() -> {
            View view = views.get(id);

            if (enabled) {
                view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            } else {
                view.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        });
    }

    /* ---------- WINDOW ---------- */

    private void windowSetContents(long contents) {
        queueUiTask(() -> {
            root.removeAllViews();
            root.addView(views.get(contents));
        });
    }

    private void windowSetContentLayout(float x, float y, float width, float height) {
        queueUiTask(() -> {
            View child = root.getChildAt(0);
            OriGroup.LayoutParams lp = (OriGroup.LayoutParams) child.getLayoutParams();
            lp.width = px(width);
            lp.height = px(height);
            lp.x = px(x);
            lp.y = px(y);

            child.requestLayout();
        });
    }

    @SuppressWarnings("deprecation")
    private void windowSetStatusBar(
            boolean isLight,
            boolean setColor,
            float r,
            float g,
            float b,
            float a) {
        queueUiTask(() -> {
            new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(isLight);

            if (setColor) {
                statusBarColor = rgba(r, g, b, a);
            } else {
                TypedValue value = new TypedValue();
                getTheme().resolveAttribute(android.R.attr.statusBarColor, value, true);
                statusBarColor = value.data;
            }

            getWindow().setStatusBarColor(statusBarColor);
        });
    }

    @SuppressWarnings("deprecation")
    private void windowSetNavigationBar(
            boolean isLight,
            boolean setColor,
            float r,
            float g,
            float b,
            float a) {
        queueUiTask(() -> {
            new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightNavigationBars(isLight);

            if (setColor) {
                navigationBarColor = rgba(r, g, b, a);
            } else {
                TypedValue value = new TypedValue();
                getTheme().resolveAttribute(android.R.attr.navigationBarColor, value, true);
                navigationBarColor = value.data;
            }

            getWindow().setNavigationBarColor(navigationBarColor);
        });
    }

    private int windowGetWidth() {
        return (int) Math.round(lc(windowMetrics.getBounds().width()));
    }

    private int windowGetHeight() {
        return (int) Math.round(lc(windowMetrics.getBounds().height()));
    }

    private void windowStartAnimating() {
        queueUiTask(() -> {
            if (!isAnimating) {
                isAnimating = true;
                Choreographer.getInstance()
                        .postFrameCallback(this::frameCallback);
            }
        });
    }

    private void windowStopAnimating() {
        if (isAnimating) {
            isAnimating = false;
            lastFrameTime = 0;
        }
    }

    void frameCallback(long frameTime) {
        if (!isAnimating)
            return;

        if (lastFrameTime == 0)
            lastFrameTime = frameTime;

        long duration = frameTime - lastFrameTime;
        lastFrameTime = frameTime;
        onAnimationFrame(duration);

        executeUiTasks();

        Choreographer.getInstance()
                .postFrameCallback(this::frameCallback);
    }

    static native void onAnimationFrame(long duration);

    /* ---------- GROUP ---------- */

    private void createGroup(long id) {
        queueUiTask(() -> {
            OriGroup view = new OriGroup(this);
            views.put(id, view);
        });
    }

    private void groupInsert(long id, int index, long child) {
        queueUiTask(() -> {
            OriGroup view = (OriGroup) views.get(id);
            view.addView(views.get(child), index);
        });
    }

    private void groupRemove(long id, int index) {
        queueUiTask(() -> {
            OriGroup view = (OriGroup) views.get(id);
            view.removeViewAt(index);
        });
    }

    private void groupSwap(long id, int indexA, int indexB) {
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

    private void groupSetChildLayout(long id, int index,
            float x, float y,
            float width, float height) {
        queueUiTask(() -> {
            OriGroup view = (OriGroup) views.get(id);
            View child = view.getChildAt(index);

            OriGroup.LayoutParams lp = (OriGroup.LayoutParams) child.getLayoutParams();
            lp.width = px(width);
            lp.height = px(height);
            lp.x = px(x);
            lp.y = px(y);

            child.requestLayout();
        });
    }

    private void groupSetBackgroundColor(long id, float r, float g, float b, float a) {
        queueUiTask(() -> {
            OriGroup view = (OriGroup) views.get(id);

            int color = rgba(r, g, b, a);
            view.setBackgroundColor(color);
        });
    }

    private void groupSetCornerRadii(long id, float tl, float tr, float br, float bl) {
        queueUiTask(() -> {
            OriGroup view = (OriGroup) views.get(id);
            view.setCornerRadii(px(tl), px(tr), px(br), px(bl));
        });
    }

    private void groupSetBorderWidth(long id, float t, float r, float b, float l) {
        queueUiTask(() -> {
            OriGroup view = (OriGroup) views.get(id);
            view.setBorderWidth(px(t), px(r), px(b), px(l));
        });
    }

    private void groupSetBorderColor(long id, float r, float g, float b, float a) {
        queueUiTask(() -> {
            OriGroup view = (OriGroup) views.get(id);

            int color = rgba(r, g, b, a);
            view.setBorderColor(color);
        });
    }

    private void groupSetOverflow(long id, boolean visible) {
        queueUiTask(() -> {
            OriGroup view = (OriGroup) views.get(id);
            view.setOverflow(visible);
        });
    }

    private void groupSetShadow(long id,
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

    private void createPressable(long id) {
        queueUiTask(() -> {
            OriPressable view = new OriPressable(this, id);
            views.put(id, view);
        });
    }

    private void pressableSetContents(long id, long contents) {
        queueUiTask(() -> {
            OriPressable view = (OriPressable) views.get(id);
            view.removeAllViews();
            view.addView(views.get(contents));
        });
    }

    private void pressableSetContentSize(long id, float width, float height) {
        queueUiTask(() -> {
            OriPressable view = (OriPressable) views.get(id);
            View child = view.getChildAt(0);

            OriPressable.LayoutParams lp = (OriPressable.LayoutParams) child.getLayoutParams();
            lp.width = px(width);
            lp.height = px(height);

            child.requestLayout();
        });
    }

    /* ---------- SCROLL ---------- */

    private void createScroll(long id) {
        queueUiTask(() -> {
            FrameLayout view = new FrameLayout(this);
            ScrollView scroll = new ScrollView(this);
            OriGroup group = new OriGroup(this);

            scroll.addView(group);
            view.addView(scroll);

            views.put(id, view);
        });
    }

    private void scrollSetContents(long id, long contents) {
        queueUiTask(() -> {
            FrameLayout view = (FrameLayout) views.get(id);
            ViewGroup scroll = (ViewGroup) view.getChildAt(0);
            OriGroup group = (OriGroup) scroll.getChildAt(0);
            group.removeAllViews();
            group.addView(views.get(contents));
        });
    }

    private void scrollSetContentSize(long id, float width, float height) {
        queueUiTask(() -> {
            FrameLayout view = (FrameLayout) views.get(id);
            ViewGroup scroll = (ViewGroup) view.getChildAt(0);
            OriGroup group = (OriGroup) scroll.getChildAt(0);

            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) group.getLayoutParams();
            lp.width = px(width);
            lp.height = px(height);

            group.requestLayout();
        });
    }

    private void scrollSetContentLayout(long id,
            float x, float y,
            float width, float height) {
        queueUiTask(() -> {
            FrameLayout view = (FrameLayout) views.get(id);
            ViewGroup scroll = (ViewGroup) view.getChildAt(0);
            OriGroup group = (OriGroup) scroll.getChildAt(0);
            View child = group.getChildAt(0);

            OriGroup.LayoutParams lp = (OriGroup.LayoutParams) child.getLayoutParams();
            lp.width = px(width);
            lp.height = px(height);
            lp.x = px(x);
            lp.y = px(y);

            child.requestLayout();
        });
    }

    private void scrollSetVertical(long id, boolean vertical) {
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

    private void createTransform(long id) {
        queueUiTask(() -> {
            FrameLayout view = new FrameLayout(this);
            views.put(id, view);
        });
    }

    private void transformSetContents(long id, long contents) {
        queueUiTask(() -> {
            FrameLayout view = (FrameLayout) views.get(id);
            view.removeAllViews();
            view.addView(views.get(contents));
        });
    }

    @SuppressWarnings("deprecation")
    private void transformSetTransform(long id,
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

    private void createText(long id) {
        queueUiTask(() -> {
            TextView view = new TextView(this);
            views.put(id, view);
        });

        textLayout.put(id, new TextLayout());
    }

    private void textSetText(long id, String text, int wrap) {
        TextLayout layout = textLayout.get(id);
        layout.text = new SpannableString(text);
    }

    private void textSetSpan(long id,
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

    private float textMeasureWidth(long id, float maxWidth) {
        TextLayout layout = textLayout.get(id);

        StaticLayout staticLayout = createStaticLayout(layout.text, maxWidth);

        float width = 0.0f;

        for (int i = 0; i < staticLayout.getLineCount(); i++) {
            width = Math.max(width, staticLayout.getLineWidth(i));
        }

        return width / metrics.density;
    }

    private float textMeasureHeight(long id, float maxWidth) {
        TextLayout layout = textLayout.get(id);

        StaticLayout staticLayout = createStaticLayout(layout.text, maxWidth);

        return lc(staticLayout.getHeight());
    }

    private StaticLayout createStaticLayout(
            SpannableString text,
            float maxWidth) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return StaticLayout.Builder.obtain(
                    text,
                    0,
                    text.length(),
                    new TextPaint(),
                    px(maxWidth))
                    .build();
        } else {
            @SuppressWarnings("deprecation")
            StaticLayout layout = new StaticLayout(
                text,
                new TextPaint(),
                px(maxWidth),
                Layout.Alignment.ALIGN_NORMAL,
                1.0f,
                0.0f,
                true);

            return layout;
        }
    }

    private static class TextLayout {
        private SpannableString text;
    }

    /* ---------- TEXT INPUT ---------- */

    private void createTextInput(long id) {
        queueUiTask(() -> {
            OriEditText view = new OriEditText(this, id);
            views.put(id, view);
        });

        textInputLayout.put(id, new TextInputLayout());
    }

    private void textInputSetSingleLine(long id, boolean singleline) {
        queueUiTask(() -> {
            OriEditText view = (OriEditText) views.get(id);
            view.setSingleLine(singleline);
        });
    }

    private void textInputSetText(long id, String text) {
        queueUiTask(() -> {
            OriEditText view = (OriEditText) views.get(id);
            view.setText(text);
        });
    }

    private void textInputSetFont(long id,
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
        layout.paint.setTypeface(typeface);
        layout.paint.setTextSize(px(textSize));

        queueUiTask(() -> {
            OriEditText view = (OriEditText) views.get(id);

            view.setTypeface(typeface);
            view.setTextSize(textSize);

            int color = rgba(r, g, b, a);
            view.setTextColor(color);
        });
    }

    private void textInputSetPlaceholderText(long id, String text) {
        queueUiTask(() -> {
            OriEditText view = (OriEditText) views.get(id);
            view.setPlaceholderText(text);
        });
    }

    private void textInputSetPlaceholderFont(long id,
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
        layout.placeholderPaint.setTypeface(typeface);
        layout.placeholderPaint.setTextSize(px(textSize));

        queueUiTask(() -> {
            OriEditText view = (OriEditText) views.get(id);

            int color = rgba(r, g, b, a);
            view.setPlaceholderFont(typeface, px(textSize), color);
        });
    }

    private float textInputMeasureHeight(long id) {
        TextInputLayout layout = textInputLayout.get(id);

        var fm = layout.paint.getFontMetrics();
        var pfm = layout.placeholderPaint.getFontMetrics();

        float height = fm.descent - fm.ascent;
        float placeholderHeight = pfm.descent - pfm.ascent;

        return Math.max(height, placeholderHeight) / metrics.density + 4.0f;
    }

    private static class TextInputLayout {
        private TextPaint paint = new TextPaint();
        private TextPaint placeholderPaint = new TextPaint();
    }

    /* ---------- IMAGE ---------- */

    private void createImage(long id) {
        queueUiTask(() -> {
            OriImage image = new OriImage(this);
            views.put(id, image);
        });

        imageLayout.put(id, new ImageLayout());
    }

    private void imageLoadSvg(long id, byte[] data) {
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

    private void imageLoadBitmap(long id, byte[] data) {
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

    private void imageSetTint(long id,
            boolean isSet,
            float r, float g, float b, float a) {
        queueUiTask(() -> {
            OriImage view = (OriImage) views.get(id);
            int color = rgba(r, g, b, a);
            view.setTint(color, isSet);
        });
    }

    private float imageGetWidth(long id) {
        ImageLayout layout = imageLayout.get(id);
        return layout.width;
    }

    private float imageGetHeight(long id) {
        ImageLayout layout = imageLayout.get(id);
        return layout.height;
    }

    static class ImageLayout {
        private float width;
        private float height;
    }

    /* ---------- HELPER ---------- */

    private ArrayDeque<Runnable> uiTasks = new ArrayDeque<>();

    private void queueUiTask(Runnable task) {
        uiTasks.addLast(task);
    }

    private int runUiTasks() {
        int count = uiTasks.size();

        runOnUiThread(() -> {
            executeUiTasks();
        });

        return count;
    }

    private void executeUiTasks() {
        root.suppressLayout(true);

        try {
            while (!uiTasks.isEmpty()) {
                uiTasks.removeFirst().run();
            }
        } finally {
            root.suppressLayout(false);
        }
    }

    private static int rgba(float r, float g, float b, float a) {
        return Color.argb(
                (int) Math.round(a * 255.0f),
                (int) Math.round(r * 255.0f),
                (int) Math.round(g * 255.0f),
                (int) Math.round(b * 255.0f));
    }

    private static Typeface createTypeface(String family, int weight, boolean italic) {
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
