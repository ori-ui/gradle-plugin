package ori;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.widget.AppCompatEditText;

import java.lang.CharSequence;

public class OriEditText extends AppCompatEditText {
    long id;

    OriActivity activity;

    boolean isSingline = false;
    boolean isSetting = false;

    public OriEditText(OriActivity context, long id) {
        super(context);

        this.id = id;
        this.activity = context;

        setBackgroundColor(Color.TRANSPARENT);
        setPadding(0, 0, 0, 0);

        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

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
                submit();
                return true;
            }

            if (event != null) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER && isSingline) {
                    submit();
                    return true;
                }
            }

            return false;
        });

        setOnFocusChangeListener((v, focused) -> {
            onFocus(id, focused);
        });
    }

    void submit() {
        if ((getImeOptions() & EditorInfo.IME_ACTION_NEXT) == 0) {
            View next = focusSearch(View.FOCUS_FORWARD);

            if (next != null || next == this) {
                next.requestFocus();
            }
        }

        onSubmit(id, getText().toString());
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
            setMaxLines(Integer.MAX_VALUE);
        }
    }

    static native void onChange(long id, String text);

    static native void onSubmit(long id, String text);

    static native void onFocus(long id, boolean focused);
}
