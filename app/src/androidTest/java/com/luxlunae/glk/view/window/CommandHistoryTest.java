package com.luxlunae.glk.view.window;

import android.app.Application;
import android.content.Context;
import android.test.InstrumentationTestCase;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import com.luxlunae.fabularium.R;
import com.luxlunae.glk.GLKCharsetManager;
import com.luxlunae.glk.GLKConstants;
import com.luxlunae.glk.model.GLKModel;
import com.luxlunae.glk.model.stream.window.GLKTextBufferM;
import com.luxlunae.glk.model.style.GLKStyleSpan;
import com.luxlunae.glk.view.keyboard.GLKInputConnection;
import java.nio.ByteBuffer;
import java.util.UUID;

// History storage and recall without starting an interpreter.
public class CommandHistoryTest extends InstrumentationTestCase {
    public void testPersistenceNavigationAndBoundedUnicodeHistory() throws Throwable {
        runTestOnUiThread(new Runnable() {
            @Override public void run() {
                Context context = new ContextThemeWrapper(getInstrumentation().getTargetContext(),
                        R.style.Theme_Fabularium);
                GLKModel host = new GLKModel((Application) context.getApplicationContext());
                host.mIFID = "history-check-" + UUID.randomUUID();
                host.mCharsetMgr = new GLKCharsetManager(GLKCharsetManager.ENDIAN.BIG_FIXED, "UTF-8");
                GLKTextBufferM model = model(host, context);
                model.requestLineEvent(ByteBuffer.allocate(1024), 0, false);
                remember(model, "open trunk");
                remember(model, "get key");
                remember(model, "get key");
                remember(model, "   ");
                assertEquals(2, model.getCommandHistory().size());
                assertEquals("Oldest command comes first", "open trunk", model.getCommandHistory().get(0));
                assertEquals("Newest command comes last", "get key", model.getCommandHistory().get(1));
                assertEquals(model.getCommandHistory(), model(host, context).getCommandHistory());
                model.getCommandHistory().clear();
                assertEquals("Caller must not mutate stored history", 2, model.getCommandHistory().size());

                GLKTextBufferV view = new GLKTextBufferV(context, model, new GLKScreen(context),
                        null, null, 0, 0, false);
                model.resize(600, 400);
                view.layout(0, 0, 600, 400);
                view.updateContents();
                view.updateInputState();
                view.replaceInput("unfinished draft");
                GLKInputConnection connection = new GLKInputConnection(view);
                assertTrue(connection.sendKeyEvent(key(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP)));
                assertEquals("get key", view.getInputBuffer());
                connection.sendKeyEvent(key(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP));
                assertEquals("get key", view.getInputBuffer());
                view.sendKey(GLKConstants.keycode_Up);
                assertEquals("open trunk", view.getInputBuffer());
                view.sendKey(GLKConstants.keycode_Up);
                assertEquals("open trunk", view.getInputBuffer());
                view.sendKey(GLKConstants.keycode_Down);
                assertEquals("get key", view.getInputBuffer());
                view.sendKey(GLKConstants.keycode_Down);
                assertEquals("unfinished draft", view.getInputBuffer());
                view.sendKey(GLKConstants.keycode_Down);
                assertEquals("unfinished draft", view.getInputBuffer());
                model.setTerminators(new long[]{GLKConstants.keycode_Up});
                view.updateContents();
                assertFalse(view.onInputKey(key(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP)));
                model.setTerminators(null);
                view.updateContents();

                model.mIs32Bit = true;
                remember(model, "caf\u00e9 \uD83D\uDE00");
                assertEquals("caf\u00e9 \uD83D\uDE00", model.getCommandHistory().get(2));
                for (int i = 0; i < 205; i++) remember(model, "command " + i);
                assertEquals(200, model.getCommandHistory().size());
                assertEquals("command 5", model.getCommandHistory().get(0));
                assertEquals("command 204", model.getCommandHistory().get(199));
                host.mIFID = "other-history-" + UUID.randomUUID();
                assertTrue(model(host, context).getCommandHistory().isEmpty());
            }
        });
    }
    private static void remember(GLKTextBufferM model, String command) {
        model.mInputBB.clear();
        int length = model.mCharsetMgr.putGLKString(command, model.mInputBB, model.mIs32Bit, false);
        model.rememberCommand(length);
    }
    private static GLKTextBufferM model(GLKModel host, Context context) {
        GLKStyleSpan[] styles = new GLKStyleSpan[GLKConstants.style_NUMSTYLES];
        GLKStyleSpan.getDefaultStyles(styles, false, 16, context);
        GLKTextBufferM.GLKTextBufferBuilder builder = new GLKTextBufferM.GLKTextBufferBuilder();
        builder.setModel(host);
        builder.setDefaultStyles(styles);
        builder.setLeadingMultiplier(1.2f);
        GLKTextBufferM model = builder.build();
        model.setRock(1);
        return model;
    }
    private static KeyEvent key(int action, int code) { return new KeyEvent(action, code); }
}
