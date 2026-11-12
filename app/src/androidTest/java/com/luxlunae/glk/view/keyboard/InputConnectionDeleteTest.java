package com.luxlunae.glk.view.keyboard;

import android.app.Application;
import android.content.Context;
import android.test.InstrumentationTestCase;
import android.text.Editable;
import android.text.Selection;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;

import com.luxlunae.fabularium.R;
import com.luxlunae.glk.GLKConstants;
import com.luxlunae.glk.model.GLKModel;
import com.luxlunae.glk.model.stream.window.GLKTextBufferM;
import com.luxlunae.glk.model.style.GLKStyleSpan;
import com.luxlunae.glk.view.window.GLKScreen;
import com.luxlunae.glk.view.window.GLKTextBufferV;
import java.lang.reflect.Method;

// Android DEL is Backspace; FORWARD_DEL is Delete.
public class InputConnectionDeleteTest extends InstrumentationTestCase {
    public void testDeletionPathsAndKeyDirections() throws Throwable {
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Context context = new ContextThemeWrapper(getInstrumentation().getTargetContext(),
                            R.style.Theme_Fabularium);
                    GLKModel model = new GLKModel((Application) context.getApplicationContext());
                    GLKStyleSpan[] styles = new GLKStyleSpan[GLKConstants.style_NUMSTYLES];
                    GLKStyleSpan.getDefaultStyles(styles, false, 16, context);
                    GLKTextBufferM.GLKTextBufferBuilder builder = new GLKTextBufferM.GLKTextBufferBuilder();
                    builder.setModel(model);
                    builder.setDefaultStyles(styles);
                    GLKTextBufferM buffer = builder.build();
                    GLKTextBufferV view = new GLKTextBufferV(context, buffer, new GLKScreen(context),
                            null, null, 0, 0, false);
                    GLKInputConnection connection = new GLKInputConnection(view);
                    Method getter = GLKInputConnection.class.getDeclaredMethod("getEditable");
                    getter.setAccessible(true);
                    Editable text = (Editable) getter.invoke(connection);

                    for (int modifiers : new int[] {0, KeyEvent.META_SHIFT_ON}) {
                        text.replace(0, text.length(), "abc");
                        Selection.setSelection(text, 1);
                        assertTrue(connection.sendKeyEvent(key(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL, modifiers)));
                        assertEquals("bc", text.toString());
                        connection.sendKeyEvent(key(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL, modifiers));
                        assertEquals("Key-up must not delete twice", "bc", text.toString());

                        text.replace(0, text.length(), "abc");
                        Selection.setSelection(text, 1);
                        connection.sendKeyEvent(key(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_FORWARD_DEL, modifiers));
                        assertEquals("ac", text.toString());
                    }
                    text.replace(0, text.length(), "abc");
                    Selection.setSelection(text, 3, 1);
                    connection.sendKeyEvent(key(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL, 0));
                    assertEquals("a", text.toString());

                    text.replace(0, text.length(), "a\uD83D\uDE00b");
                    Selection.setSelection(text, 3);
                    connection.sendKeyEvent(key(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL, 0));
                    assertEquals("ab", text.toString());
                    text.replace(0, text.length(), "a\uD83D\uDE00b");
                    Selection.setSelection(text, 1);
                    connection.sendKeyEvent(key(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_FORWARD_DEL, 0));
                    assertEquals("ab", text.toString());

                    text.replace(0, text.length(), "abc");
                    Selection.setSelection(text, 3);
                    connection.deleteSurroundingText(1, 0);
                    assertEquals("ab", text.toString());
                    text.replace(0, text.length(), "a\uD83D\uDE00b");
                    Selection.setSelection(text, 3);
                    connection.deleteSurroundingTextInCodePoints(1, 0);
                    assertEquals("ab", text.toString());

                    text.replace(0, text.length(), "abc");
                    Selection.setSelection(text, 1);
                    assertFalse(connection.sendKeyEvent(key(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL, KeyEvent.META_CTRL_ON)));
                    assertEquals("abc", text.toString());
                } catch (Exception error) {
                    throw new AssertionError(error);
                }
            }
        });
    }

    private static KeyEvent key(int action, int code, int modifiers) {
        return new KeyEvent(0, 0, action, code, 0, modifiers);
    }
}
