package com.luxlunae.glk.view.window;

import android.app.Application;
import android.content.Context;
import android.test.InstrumentationTestCase;
import android.text.DynamicLayout;

import java.lang.reflect.Field;
import android.view.ContextThemeWrapper;

import com.luxlunae.fabularium.R;
import com.luxlunae.glk.GLKConstants;
import com.luxlunae.glk.GLKCharsetManager;
import java.nio.ByteBuffer;
import com.luxlunae.glk.model.GLKModel;
import com.luxlunae.glk.model.stream.window.GLKTextBufferM;
import com.luxlunae.glk.model.style.GLKStyleSpan;

// Text-buffer geometry and reading-position preservation without an interpreter.
public class TextBufferResizeTest extends InstrumentationTestCase {
    public void testBottomSurvivesResizeAndOutput() throws Throwable {
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                Context context = new ContextThemeWrapper(
                        getInstrumentation().getTargetContext(), R.style.Theme_Fabularium);
                GLKModel model = new GLKModel((Application) context.getApplicationContext());
                model.mCharsetMgr = new GLKCharsetManager(GLKCharsetManager.ENDIAN.BIG_FIXED, "UTF-8");
                GLKStyleSpan[] styles = new GLKStyleSpan[GLKConstants.style_NUMSTYLES];
                GLKStyleSpan.getDefaultStyles(styles, false, 16, context);
                GLKTextBufferM.GLKTextBufferBuilder builder = new GLKTextBufferM.GLKTextBufferBuilder();
                builder.setModel(model);
                builder.setDefaultStyles(styles);
                builder.setLeadingMultiplier(1.2f);
                GLKTextBufferM buffer = builder.build();
                buffer.resize(600, 1600);
                GLKTextBufferV view = new GLKTextBufferV(context, buffer, new GLKScreen(context),
                        null, null, 0, 0, false);
                view.setPadding(10, 10, 10, 10);
                view.layout(0, 0, 600, 1600);
                buffer.getBuffer().append("Room description\nNewest line\n>");
                view.updateContents();
                assertBottomVisible(view);

                // Shrink only the view: the model can still have its old height at IME resize.
                view.layout(0, 0, 600, 100);
                assertBottomVisible(view);
                view.scrollTo(0, view.getChildHeight());
                assertBottomVisible(view);
                view.layout(0, 0, 600, 1600);
                assertBottomVisible(view);

                for (int i = 0; i < 300; i++) buffer.getBuffer().append("Long output line\n");
                buffer.getBuffer().append(">");
                view.updateContents();
                assertBottomVisible(view);
                // A window halved in height must display the bottom half, not the top half.
                view.layout(0, 0, 600, 800);
                assertBottomVisible(view);
                view.layout(0, 0, 600, 100);
                assertBottomVisible(view);

                // Model-first and view-first width changes both keep the bottom visible.
                buffer.resize(300, 100);
                view.updateContents();
                view.layout(0, 0, 300, 100);
                assertBottomVisible(view);
                buffer.resize(600, 100);
                view.layout(0, 0, 600, 100);
                view.updateContents();
                assertBottomVisible(view);

                buffer.requestLineEvent(ByteBuffer.allocate(80), 0, false);
                view.updateInputState();
                view.sendKey('a');
                view.sendKey(GLKConstants.keycode_Return);
                assertBottomVisible(view); // Includes the newline added when composition ends.

                // Repeated line requests start a new composing session without native buffers.
                buffer.mLineEventCount++;
                view.updateInputState();
                for (int i = 0; i < 500; i++) buffer.mInput.append('a');
                view.sendKey('a');
                assertBottomVisible(view);
                view.clearInputBuffer();
                assertBottomVisible(view); // Shrinking/deleting the input also clamps the end.

                assertTrue("History fixture must overflow the viewport",
                        view.getChildHeight() > view.mHeightLessPadding + 200);
                view.scrollTo(0, 100);
                assertEquals(100, view.getScrollY());
                view.updateContents(); // No pending output: leave history alone.
                assertEquals(100, view.getScrollY());
                buffer.getBuffer().append("New output while reading history\n>");
                view.updateContents();
                assertEquals(100, view.getScrollY());
                view.clearInputBuffer();
                assertEquals(100, view.getScrollY());
                view.layout(0, 0, 600, 50);
                assertEquals(100, view.getScrollY());
                view.layout(0, 0, 600, 200);
                assertEquals(100, view.getScrollY());
                buffer.resize(300, 200);
                view.updateContents();
                view.layout(0, 0, 300, 200);
                assertEquals(100, view.getScrollY());

                view.scrollTo(0, view.getChildHeight());
                buffer.getBuffer().append("More output after returning to bottom\n>");
                view.updateContents();
                assertBottomVisible(view);
            }
        });
    }

    private static void assertBottomVisible(GLKTextBufferV view) {
        try {
            Field layoutField = GLKTextBufferV.class.getDeclaredField("mLayout");
            Field offsetField = GLKTextBufferV.class.getDeclaredField("mVOffset");
            layoutField.setAccessible(true);
            offsetField.setAccessible(true);
            DynamicLayout layout = (DynamicLayout) layoutField.get(view);
            int drawnBottom = offsetField.getInt(view) + layout.getHeight();
            assertEquals("Drawn content bottom must equal viewport bottom",
                    view.mHeightLessPadding, drawnBottom - view.getScrollY());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
