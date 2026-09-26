package com.tgauch.tapwars.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.tgauch.tapwars.enums.Player;
import com.tgauch.tapwars.game.TugOfWar;

/**
 * Draws the battlefield: the bottom player's colour fills the screen from the bottom, the top
 * player's colour from the top, and the border between them moves as the players tap.
 *
 * <p>When {@link #setMirrored(boolean) mirrored}, everything in the top half is drawn upside down
 * so two people can share one phone lying flat between them.
 */
public class BattleGroundView extends View {

    public interface Listener {
        /** @param bottomHalf true if the tap landed in the bottom half of the screen. */
        void onSideTapped(boolean bottomHalf);
    }

    private final Paint bottomPaint = new Paint();
    private final Paint topPaint = new Paint();
    private final Paint borderPaint = new Paint();
    private final Paint namePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bigTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint smallTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private TugOfWar game;
    private Player bottomPlayer = Player.ONE;
    private boolean mirrored = false;
    private String bottomName = "";
    private String topName = "";
    private String bottomMessage = null;
    private String topMessage = null;
    private boolean messagesAreBig = true;
    private Listener listener;

    public BattleGroundView(Context context) {
        this(context, null);
    }

    public BattleGroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
        bottomPaint.setColor(Color.RED);
        topPaint.setColor(Color.BLUE);
        borderPaint.setColor(Color.WHITE);

        for (Paint paint : new Paint[] {namePaint, bigTextPaint, smallTextPaint}) {
            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setShadowLayer(dp(4), 0, dp(2), 0x80000000);
        }
        namePaint.setTextSize(sp(22));
        bigTextPaint.setTextSize(sp(96));
        smallTextPaint.setTextSize(sp(24));
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setGame(TugOfWar game, Player bottomPlayer) {
        this.game = game;
        this.bottomPlayer = bottomPlayer;
        invalidate();
    }

    public void setPlayers(String bottomName, int bottomColor, String topName, int topColor) {
        this.bottomName = bottomName;
        this.topName = topName;
        bottomPaint.setColor(bottomColor);
        topPaint.setColor(topColor);
        invalidate();
    }

    public void setMirrored(boolean mirrored) {
        this.mirrored = mirrored;
        invalidate();
    }

    /**
     * Shows a message to each player (null hides it). When the view isn't mirrored only the
     * bottom message is drawn, in the middle of the screen.
     *
     * @param big true for short countdown-style text, false for longer status text.
     */
    public void setMessages(String bottomMessage, String topMessage, boolean big) {
        this.bottomMessage = bottomMessage;
        this.topMessage = topMessage;
        this.messagesAreBig = big;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            // Every finger counts, so both players can hammer the screen at the same time.
            float y = event.getY(event.getActionIndex());
            if (listener != null) {
                listener.onSideTapped(y >= getHeight() / 2f);
            }
            performClick();
            return true;
        }
        return action == MotionEvent.ACTION_MOVE
                || action == MotionEvent.ACTION_UP
                || action == MotionEvent.ACTION_POINTER_UP
                || action == MotionEvent.ACTION_CANCEL;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();

        float bottomShare = game == null ? 0.5f : game.getShare(bottomPlayer);
        float border = height * (1f - bottomShare);

        canvas.drawRect(0, 0, width, border, topPaint);
        canvas.drawRect(0, border, width, height, bottomPaint);
        canvas.drawRect(0, border - dp(3), width, border + dp(3), borderPaint);

        float nameMargin = dp(48);
        canvas.drawText(bottomName, width / 2f, height - nameMargin, namePaint);
        if (mirrored) {
            drawUpsideDown(canvas, topName, width / 2f, nameMargin, namePaint);
        } else {
            canvas.drawText(topName, width / 2f, nameMargin + namePaint.getTextSize(), namePaint);
        }

        Paint messagePaint = messagesAreBig ? bigTextPaint : smallTextPaint;
        float centerOffset = (messagePaint.descent() + messagePaint.ascent()) / 2f;
        if (!mirrored) {
            if (bottomMessage != null) {
                canvas.drawText(bottomMessage, width / 2f, height / 2f - centerOffset, messagePaint);
            }
        } else {
            if (bottomMessage != null) {
                canvas.drawText(bottomMessage, width / 2f, height * 0.75f - centerOffset, messagePaint);
            }
            if (topMessage != null) {
                drawUpsideDown(canvas, topMessage, width / 2f, height * 0.25f + centerOffset, messagePaint);
            }
        }
    }

    /** Draws text rotated 180 degrees so it reads correctly from the other end of the phone. */
    private void drawUpsideDown(Canvas canvas, String text, float x, float y, Paint paint) {
        canvas.save();
        canvas.rotate(180, x, y);
        canvas.drawText(text, x, y, paint);
        canvas.restore();
    }

    private float dp(float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private float sp(float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, value, getResources().getDisplayMetrics());
    }
}
