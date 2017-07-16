package com.tgauch.tapwars.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.CountDownTimer;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View;
import android.widget.Toast;

import com.tgauch.tapwars.enums.BattleGroundState;
import com.tgauch.tapwars.enums.Player;
import com.tgauch.tapwars.interfaces.BattleGroundEventListener;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by tag10 on 7/15/2017.
 */

public class BattleGroundView extends View implements View.OnTouchListener {

    private static final String TAG = "BattleGroundView";

    private Paint player1Paint;
    private Paint player2Paint;
    private TextPaint victoryPaint;
    private int middleLine; // this is where the two rectangles meet
    private BattleGroundState state;
    private int width;
    private int height;
    private int attackPower = 100;
    private Player winner;
    private BattleGroundEventListener listener = null;
    private Date startTime;
    private Context context;

    public BattleGroundView(final Context context, int player1Color, int player2Color, BattleGroundEventListener listener, Date startTime) {
        super(context);
        this.listener = listener;
        this.startTime = startTime;
        this.context = context;

        this.initPlayer1(player1Color);
        this.initPlayer2(player2Color);

        this.victoryPaint = new TextPaint();
        this.victoryPaint.setColor(Color.BLACK);
        this.victoryPaint.setTextAlign(Paint.Align.CENTER);
        this.victoryPaint.setTextSize(100);
        this.victoryPaint.setStyle(Paint.Style.FILL);

        this.state = BattleGroundState.ORGANIZING;

        Date now = Calendar.getInstance().getTime();
        new CountDownTimer(startTime.getTime() - now.getTime(), 1000) {

            public void onTick(long millisUntilFinished) {
                Toast.makeText(context, "seconds remaining: " + millisUntilFinished / 1000, Toast.LENGTH_LONG).show();
            }

            public void onFinish() {
                commenceBattle();
            }
        }.start();
    }

    private void initPlayer1(int player1Color) {
        this.player1Paint = new Paint();
        this.player1Paint.setColor(player1Color);
        this.player1Paint.setStyle(Paint.Style.FILL);
    }

    private void initPlayer2(int player2Color) {
        this.player2Paint = new Paint();
        this.player2Paint.setColor(player2Color);
        this.player2Paint.setStyle(Paint.Style.FILL);
    }

    public void commenceBattle() {
        this.state = BattleGroundState.BATTLE;
        Toast.makeText(context, "TAP!", Toast.LENGTH_SHORT).show();
        this.listener.onBattleStart();
    }

    public void player1Attack() {
        if (this.isVictory()) {
            return;
        }
        this.middleLine+=this.attackPower;
        this.invalidate();
    }

    public void player2Attack() {
        if (this.isVictory()) {
            return;
        }

        this.middleLine-=this.attackPower;
        this.invalidate();
    }

    public boolean isVictory() {
        return this.state == BattleGroundState.VICTORY;
    }

    private void declareVictory(Player winner) {
        this.state = BattleGroundState.VICTORY;
        this.listener.onVictory(winner);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        if (this.state == BattleGroundState.ORGANIZING) {
            this.middleLine = width/2;
            this.width = width;
            this.height = height;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawRect(0, 0, this.middleLine, this.height, this.player1Paint);
        canvas.drawRect(this.middleLine, 0, this.width, this.height, this.player2Paint);

        if (this.middleLine >= this.width) {
            this.declareVictory(Player.ONE);
        } else if (this.middleLine <= 0) {
            this.declareVictory(Player.TWO);
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return false;
    }
}
