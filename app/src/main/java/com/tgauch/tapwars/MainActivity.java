package com.tgauch.tapwars;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.icu.util.Output;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.Payload;
import com.tgauch.tapwars.enums.MainState;
import com.tgauch.tapwars.enums.Player;
import com.tgauch.tapwars.interfaces.BattleGroundEventListener;
import com.tgauch.tapwars.interfaces.LoaderCallback;
import com.tgauch.tapwars.views.BattleGroundView;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainActivity extends ConnectionsActivity implements BattleGroundEventListener, View.OnClickListener {

    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
    private Button findMatchButton;
    private Button startMatchButton;
    private Button stopMatchButton;
    private Player localPlayer;
    private int lastDiceRoll;
    private Date startTime;
    private boolean ready = false;
    private BattleGroundView battleGroundView;
    private boolean battleStarted = false;
    private String name;
    private String otherPlayerName;
    private boolean isFullScreen = false;
    private MainState state = MainState.UNKNOWN;
    private ProgressDialog loadingDialog;
    private CountDownTimer loadingTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadingDialog = new ProgressDialog(this);
        loadingDialog.setCancelable(false);

        loadMainMenu();

        AccountManager manager = (AccountManager) getSystemService(ACCOUNT_SERVICE);
        Account[] list = manager.getAccounts();
        if (list.length > 0) {
            this.name = list[0].name;
            if (name.contains("@")) {
                name = name.substring(0, name.indexOf("@"));
            }
        } else {
            this.name = UUID.randomUUID().toString();
            name = "guest" + name.substring(0, name.indexOf("-"));
        }
    }

    private void loadMainMenu() {
        setState(MainState.MAIN_MENU_WAITING);
        setContentView(R.layout.activity_main);
        if (isFullScreen) {
            toggleSystemUI();
        }

        this.findMatchButton = (Button) findViewById(R.id.findMatch);

        if (!isApiConnected()) {
            this.findMatchButton.setVisibility(View.INVISIBLE);
        }

        this.findMatchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setState(MainState.LOOKING_FOR_PLAYER);
                startAdvertising();
                startDiscovering();
                showSpinner("Searching for player", 30000, new LoaderCallback() {
                    @Override
                    public void onStart() {
                    }

                    @Override
                    public void onTick() {
                    }

                    @Override
                    public void onFinish(boolean wasTimeout) {
                        if (wasTimeout) {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setMessage("Unable to find a player :(").show();
                        }

                        disconnect();
                    }
                });
            }
        });
    }

    private void showSpinner(String message, int timeout, final LoaderCallback  callback) {
        // if we are starting a spinner make sure there isn't one already running
        stopSpinner();
        callback.onStart();
        loadingDialog.setMessage(message);
        loadingDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                stopSpinner();
                callback.onFinish(false);
            }
        });
        loadingDialog.show();
        this.loadingTimer = new CountDownTimer(timeout, 1000) {

            public void onTick(long millisUntilFinished) {
                callback.onTick();
            }

            public void onFinish() {
                stopSpinner();
                callback.onFinish(true);
            }
        }.start();
    }

    private void stopSpinner() {
        if (loadingDialog != null) {
            loadingDialog.dismiss();
        }
        if (loadingTimer != null) {
            loadingTimer.cancel();
        }
    }

    private void loadGameEndScreen(Player winner) {
        setState(MainState.END_GAME_WAITING);
        if (isFullScreen) {
            toggleSystemUI();
        }
        setContentView(R.layout.victory_layout);
        TextView victoryText = (TextView) findViewById(R.id.victoryText);
        victoryText.setText((winner == localPlayer ? name : otherPlayerName) + " is victorious!");

        Button mainMenu = (Button) findViewById(R.id.mainMenu);
        mainMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnect();
                loadMainMenu();
            }
        });

        Button rematch = (Button) findViewById(R.id.rematch);
        rematch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setState(MainState.ASKING_FOR_REMATCH);
                suggestStartTime();
            }
        });

        Button quit = (Button) findViewById(R.id.quit);
        quit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    /**
     * We've connected to Nearby Connections. We can now start calling {@link #startDiscovering()} and
     * {@link #startAdvertising()}.
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        super.onConnected(bundle);
        this.findMatchButton.setVisibility(View.VISIBLE);
    }

    /** We were disconnected! Halt everything! */
    @Override
    public void onConnectionSuspended(int reason) {
        super.onConnectionSuspended(reason);
    }

    @Override
    protected void onEndpointDiscovered(Endpoint endpoint) {
        // We found an advertiser!
        connectToEndpoint(endpoint);
    }

    @Override
    protected void onConnectionInitiated(final Endpoint endpoint, ConnectionInfo connectionInfo) {

        stopSpinner();
        new AlertDialog.Builder(this)
            .setTitle("We found a player!")
            .setCancelable(false)
            .setMessage("Game request from " + endpoint.getName() + ". Would you like to accept?")
            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    showSpinner("Waiting for " + endpoint, 60000, new LoaderCallback() {
                        @Override
                        public void onStart() {
                        }

                        @Override
                        public void onTick() {}

                        @Override
                        public void onFinish(boolean wasTimeout) {
                            if (wasTimeout) {
                                new AlertDialog.Builder(MainActivity.this)
                                        .setMessage(endpoint + " failed to respond.")
                                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                                            @Override
                                            public void onDismiss(DialogInterface dialog) {
                                                findMatchButton.performClick();
                                            }
                                        });
                            } else {
                                disconnect();
                            }
                        }
                    });
                    acceptConnection(endpoint);
                }
            })
            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    rejectConnection(endpoint);
                    disconnect();
                }
            }).show();
    }

    @Override
    protected void onEndpointConnected(Endpoint endpoint) {
        stopSpinner();
        send(Payload.fromBytes(("Username:" + this.name).getBytes()));
        rollDice();
    }

    @Override
    protected void onEndpointDisconnected(Endpoint endpoint) {
        setState(MainState.DISCONNECTED);
    }

    @Override
    protected void onReceive(Endpoint endpoint, Payload payload) {
        String message = new String(payload.asBytes());
        if (message.startsWith("Dice_Roll:")) {
            int roll = Integer.parseInt(message.substring("Dice_Roll:".length()));
            if (roll > lastDiceRoll) {
                localPlayer = Player.TWO;
            } else if (roll < lastDiceRoll) {
                localPlayer = Player.ONE;
                suggestStartTime();
            } else {
                rollDice();
            }
        } else if (message.startsWith("Start_Time:")) {
            String startTime = message.substring("Start_Time:".length());
            try {
                Date startDate = timeFormat.parse(startTime);
                Date now = Calendar.getInstance().getTime();
                // the suggested date is in the past something went wrong lets try again
                if (startDate.compareTo(now) <= 0) {
                    // deny the start date
                    suggestStartTime();
                } else {
                    // accept the start date
                    this.startTime = startDate;
                    ready = true;
                    confirmReady();
                    startGame();
                }

            } catch(Exception e) {}

        } else if (message.equals("Tap")) {

            // other player is attacking us
            if (battleStarted) {
                if (localPlayer == Player.ONE) {
                    battleGroundView.player2Attack();
                } else {
                    battleGroundView.player1Attack();
                }
            }

        } else if (message.equals("Ready")) {
            //something went wrong we need to suggest the time again
            if (this.startTime == null) {
                suggestStartTime();
            } else if (this.startTime != null && !ready) {
                ready = true;
                startGame();
            }
        } else if (message.equals("Victory")) {
            loadGameEndScreen((localPlayer == Player.ONE ? Player.TWO : Player.ONE));
        } else if (message.startsWith("Username:")) {
            otherPlayerName = message.substring("Username:".length());
        }
    }

    @Override
    protected String getName() {
        return name;
    }

    @Override
    protected String getServiceId() {
        return "com.tgauch.tapwars";
    }

    private void rollDice() {
        Random r = new Random();
        r.setSeed(SystemClock.currentThreadTimeMillis());
        lastDiceRoll = r.nextInt();
        send(Payload.fromBytes(("Dice_Roll:"+lastDiceRoll).getBytes()));
    }

    private void suggestStartTime() {
        ready = false;
        Calendar date = Calendar.getInstance();
        date.add(Calendar.SECOND, 5);
        String message = "Start_Time:" + timeFormat.format(date.getTime());
        this.startTime = date.getTime();
        send(Payload.fromBytes(message.getBytes()));
    }

    private void confirmReady() {
        send(Payload.fromBytes("Ready".getBytes()));
    }

    private void startGame() {
        if (!isFullScreen) {
            toggleSystemUI();
        }
        setState(MainState.IN_MATCH);
        this.battleGroundView = new BattleGroundView(this, this, this.startTime);

        if (this.localPlayer == Player.ONE) {
            this.battleGroundView.initPlayer1(name, Color.RED);
            this.battleGroundView.initPlayer2(otherPlayerName, Color.BLUE);
        } else {
            this.battleGroundView.initPlayer1(otherPlayerName, Color.RED);
            this.battleGroundView.initPlayer2(name, Color.BLUE);
        }
        this.battleGroundView.invalidate();

        setContentView(this.battleGroundView);
        this.battleGroundView.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (battleStarted) {
            toggleSystemUI();

            if (localPlayer == Player.ONE) {
                battleGroundView.player1Attack();
            } else {
                battleGroundView.player2Attack();
            }
        }
    }

    @Override
    public void onVictory(Player winner) {
        send(Payload.fromBytes("Victory".getBytes()));
        loadGameEndScreen(winner);
    }

    @Override
    public void onBattleStart() {
        this.battleStarted = true;
    }

    // This snippet hides the system bars.
    private void toggleSystemUI() {
        isFullScreen = !isFullScreen;
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        this.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    private void setState(MainState newState) {
        this.onStateChanged(this.state, newState);
        this.state = newState;
    }

    private void onStateChanged(MainState oldState, MainState newState) {
        switch(newState) {
            case DISCONNECTED:
                disconnect();
                loadMainMenu();
                break;
            case MAIN_MENU_WAITING:
                break;
            case WAITING_FOR_OTHER_PLAYER:


        }
    }
}
