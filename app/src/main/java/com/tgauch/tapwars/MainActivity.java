package com.tgauch.tapwars;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.Payload;
import com.tgauch.tapwars.enums.NearbyConnectionHelperState;
import com.tgauch.tapwars.enums.Player;
import com.tgauch.tapwars.helpers.NearbyConnectionHelper;
import com.tgauch.tapwars.interfaces.BattleGroundEventListener;
import com.tgauch.tapwars.interfaces.NearbyConnectionHelperListener;
import com.tgauch.tapwars.views.BattleGroundView;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.initViews();
    }

    private void initViews() {

        this.findMatchButton = (Button) findViewById(R.id.findMatch);
        this.findMatchButton.setEnabled(false);
        this.findMatchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startAdvertising();
                startDiscovering();
            }
        });

        this.startMatchButton = (Button) findViewById(R.id.startMatch);
        this.startMatchButton.setVisibility(View.INVISIBLE);
        this.startMatchButton.setEnabled(false);
        this.startMatchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rollDice();
            }
        });

        this.stopMatchButton = (Button) findViewById(R.id.stopMatch);
        this.stopMatchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopAdvertising();
                stopDiscovering();
                disconnectFromAllEndpoints();
                toggleButtons(true);
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
        this.findMatchButton.setEnabled(true);
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
    protected void onConnectionInitiated(Endpoint endpoint, ConnectionInfo connectionInfo) {
        // We accept the connection immediately.
        acceptConnection(endpoint);
    }

    @Override
    protected void onEndpointConnected(Endpoint endpoint) {
        Toast.makeText(
                this, "Connected to " + endpoint.getName(), Toast.LENGTH_SHORT)
                .show();
        toggleButtons(false);
    }

    @Override
    protected void onEndpointDisconnected(Endpoint endpoint) {
        Toast.makeText(
                this, "Disconnected from " + endpoint.getName(), Toast.LENGTH_SHORT)
                .show();
        toggleButtons(true);
    }

    @Override
    protected void onReceive(Endpoint endpoint, Payload payload) {
        String message = new String(payload.asBytes());
        Toast.makeText(this, "Received: " + message, Toast.LENGTH_SHORT).show();
        if (message.startsWith("Dice_Roll:")) {
            int roll = Integer.parseInt(message.substring("Dice_Roll:".length()));
            if (roll > lastDiceRoll) {
                localPlayer = Player.TWO;
                Toast.makeText(this, "You are Player two!", Toast.LENGTH_LONG).show();
            } else if (roll < lastDiceRoll) {
                localPlayer = Player.ONE;
                Toast.makeText(this, "You are Player one!", Toast.LENGTH_SHORT).show();
                suggestStartTime();
            } else {
                Toast.makeText(this, "Rolls were same: " + roll + ", " + lastDiceRoll + " rerolling...", Toast.LENGTH_LONG);
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
        }
    }

    private void toggleButtons(boolean showFind) {
        if (showFind) {
            this.findMatchButton.setVisibility(View.VISIBLE);
            this.findMatchButton.setEnabled(true);
            this.startMatchButton.setVisibility(View.INVISIBLE);
            this.startMatchButton.setEnabled(false);
        } else {
            this.findMatchButton.setVisibility(View.INVISIBLE);
            this.findMatchButton.setEnabled(false);
            this.startMatchButton.setVisibility(View.VISIBLE);
            this.startMatchButton.setEnabled(true);
        }
    }

    @Override
    protected String getName() {
        return UUID.randomUUID().toString();
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
        this.battleGroundView = new BattleGroundView(this, Color.RED, Color.BLUE, this, this.startTime);
        setContentView(this.battleGroundView);
        this.battleGroundView.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Toast.makeText(this, "Clicked", Toast.LENGTH_SHORT).show();
        if (battleStarted) {
            Toast.makeText(this, "true", Toast.LENGTH_SHORT).show();
            send(Payload.fromBytes("Tap".getBytes()));
            if (localPlayer == Player.ONE) {
                battleGroundView.player1Attack();
            } else {
                battleGroundView.player2Attack();
            }
        } else {
            Toast.makeText(this, "false", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onVictory(Player winner) {
        Intent victoryIntent = new Intent(this, VictoryActivity.class);
        victoryIntent.putExtra("winner", winner);
        victoryIntent.putExtra("player", localPlayer);
        startActivity(victoryIntent);
        finish();
    }

    @Override
    public void onBattleStart() {
        this.battleStarted = true;
    }
}
