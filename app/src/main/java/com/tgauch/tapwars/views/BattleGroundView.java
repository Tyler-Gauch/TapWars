package com.tgauch.tapwars.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.CountDownTimer;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tgauch.tapwars.R;
import com.tgauch.tapwars.enums.BattleGroundState;
import com.tgauch.tapwars.enums.Player;
import com.tgauch.tapwars.interfaces.BattleGroundEventListener;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by tag10 on 7/15/2017.
 */

public class BattleGroundView extends RelativeLayout implements View.OnTouchListener {

    private static final String TAG = "BattleGroundView";

    private Paint player1Paint;
    private Paint player2Paint;
    private Paint textPaint;
    private int middleLine; // this is where the two rectangles meet
    private BattleGroundState state;
    private int width;
    private int height;
    private int attackPower = 20;
    private Player winner;
    private BattleGroundEventListener listener = null;
    private Date startTime;
    private Context context;
    private TextView player1Name;
    private TextView player2Name;
    private ImageView player1Image;
    private ImageView player2Image;

    public BattleGroundView(final Context context, BattleGroundEventListener listener, Date startTime) {
        super(context);

        this.listener = listener;
        this.startTime = startTime;
        this.context = context;

        this.textPaint = new Paint();
        this.textPaint.setColor(Color.BLACK);
        this.textPaint.setTextAlign(Paint.Align.CENTER);
        this.textPaint.setTextSize(500);
        this.textPaint.setStyle(Paint.Style.FILL);
        this.textPaint.setFakeBoldText(true);

        this.state = BattleGroundState.ORGANIZING;

        this.initPlayerInfo();

        Date now = Calendar.getInstance().getTime();
        new CountDownTimer(startTime.getTime() - now.getTime(), 1000) {

            public void onTick(long millisUntilFinished) {
                invalidate();
            }

            public void onFinish() {
                commenceBattle();
            }
        }.start();
    }

    public void initPlayer1(String playerName, int color) {
        this.player1Paint = new Paint();
        this.player1Paint.setColor(color);
        this.player1Paint.setStyle(Paint.Style.FILL);

        this.player1Name.setText(playerName);
    }

    public void initPlayer2(String playerName, int color) {
        this.player2Paint = new Paint();
        this.player2Paint.setColor(color);
        this.player2Paint.setStyle(Paint.Style.FILL);

        this.player2Name.setText(playerName);
    }

    public void initPlayerInfo() {

        LayoutParams relativeLayout = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        this.setBackgroundColor(Color.TRANSPARENT);
        this.setLayoutParams(relativeLayout);

        Drawable noUser = getResources().getDrawable(R.drawable.no_user, context.getTheme());
        noUser.setBounds(0,0,300,300);

        ImageView vs = new ImageView(context);
        vs.setId(R.id.vs);
        vs.setImageDrawable(getResources().getDrawable(R.drawable.vs, context.getTheme()));
        LayoutParams vsLayout = new LayoutParams(200, 200);
        vsLayout.addRule(ALIGN_PARENT_TOP);
        vsLayout.addRule(CENTER_HORIZONTAL);
        vs.setLayoutParams(vsLayout);
        this.addView(vs);

        player1Image = new ImageView(context);
        player1Image.setId(R.id.player1Image);
        player1Image.setImageDrawable(noUser);
        LayoutParams player1Layout = new LayoutParams(300, 300);
        player1Layout.addRule(ALIGN_PARENT_LEFT);
        player1Layout.addRule(ALIGN_PARENT_TOP);
        player1Image.setLayoutParams(player1Layout);
        this.addView(player1Image);

        player1Name = new TextView(context);
        player1Name.setId(R.id.player1Name);
        LayoutParams player1NameLayout = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 300);
        player1NameLayout.addRule(BELOW, player1Image.getId());
        player1NameLayout.addRule(ALIGN_PARENT_LEFT);
        player1NameLayout.addRule(LEFT_OF, vs.getId());
        player1Name.setText("No Username");
        player1Name.setLayoutParams(player1NameLayout);
        player1Name.setTextAlignment(TEXT_ALIGNMENT_VIEW_START);
        this.addView(player1Name);

        player2Image = new ImageView(context);
        player2Image.setId(R.id.player2Image);
        player2Image.setImageDrawable(noUser);
        LayoutParams player2Layout = new LayoutParams(300, 300);
        player2Layout.addRule(ALIGN_PARENT_RIGHT);
        player2Layout.addRule(ALIGN_PARENT_TOP);
        player2Image.setLayoutParams(player2Layout);
        this.addView(player2Image);

        player2Name = new TextView(context);
        player2Name.setId(R.id.player2Name);
        LayoutParams player2NameLayout = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 300);
        player2NameLayout.addRule(BELOW, player2Image.getId());
        player2NameLayout.addRule(ALIGN_PARENT_RIGHT);
        player2NameLayout.addRule(RIGHT_OF, vs.getId());
        player2Name.setText("No Username");
        player2Name.setLayoutParams(player2NameLayout);
        player2Name.setTextAlignment(TEXT_ALIGNMENT_VIEW_END);
        this.addView(player2Name);
    }

    public void commenceBattle() {
        this.state = BattleGroundState.BATTLE;
        invalidate();
        this.listener.onBattleStart();
    }

    public void player1Attack() {
        if (this.isVictory() || this.state != BattleGroundState.BATTLE) {
            return;
        }
        this.middleLine+=this.attackPower;
        this.invalidate();
    }

    public void player2Attack() {
        if (this.isVictory() || this.state != BattleGroundState.BATTLE) {
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

        if (state == BattleGroundState.ORGANIZING) {
            // draw the count down timer
            Date now = Calendar.getInstance().getTime();
            long secondsLeft = startTime.getTime() - now.getTime();
            canvas.drawText("" + secondsLeft / 1000, this.width/2, this.height/2, textPaint);
        }

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
