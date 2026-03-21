package ori;

import android.content.Context;
import android.view.KeyEvent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextWatcher;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.widget.AppCompatEditText;

import java.lang.CharSequence;

public class OriEditText extends AppCompatEditText {
    long id;

    String placeholderText = "";
    Typeface placeholderTypeface;
    int placeholderTextSize;
    int placeholderColor;

    boolean isSingline = false;
    boolean isSetting = false;

    public OriEditText(Context context, long id) {
        super(context);

        this.id = id;

        setBackgroundColor(Color.TRANSPARENT);
        setPadding(0, 0, 0, 0);

        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable e) {
                if (!isSetting) {
                    onChange(id, e.toString());
                }
            }
        });

        setImeOptions(EditorInfo.IME_ACTION_DONE);

        setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                onSubmit(id, getText().toString());
                return true;
            }

            if (event != null) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER && isSingline) {
                    onSubmit(id, getText().toString());
                    return true;
                }
            }

            return false;
        });
    }

    @Override
    public boolean onKeyPreIme(int keycode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            clearFocus();
        }

        return super.onKeyPreIme(keycode, event);
    }

    public void setText(String text) {
        isSetting = true;
        super.setText(text);
        isSetting = false;
    }

    public void setSingleLine(boolean singleline) {
        isSingline = singleline;
        super.setSingleLine(singleline);

        if (singleline) {
            setMaxLines(1);
        } else {
            setMaxLines(-1);
        }
    }

    public void setPlaceholderText(String text) {
        placeholderText = text;

        updatePlaceholder();
    }

    public void setPlaceholderFont(Typeface typeface, int textSize, int color) {
        placeholderTypeface = typeface;
        placeholderTextSize = textSize;
        placeholderColor = color;

        updatePlaceholder();
    }

    public Typeface getPlaceholderTypeface() {
        return placeholderTypeface;
    }

    public int getPlaceholderTextSize() {
        return placeholderTextSize;
    }

    void updatePlaceholder() {
        SpannableString text = new SpannableString(placeholderText);
        int start = 0;
        int end = text.length();

        text.setSpan(
                new OriTypefaceSpan(placeholderTypeface),
                start, end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        text.setSpan(
                new AbsoluteSizeSpan(placeholderTextSize),
                start, end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        text.setSpan(
                new ForegroundColorSpan(placeholderColor),
                start, end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        setHint(text);
    }

    static native void onChange(long id, String text);

    static native void onSubmit(long id, String text);
}
