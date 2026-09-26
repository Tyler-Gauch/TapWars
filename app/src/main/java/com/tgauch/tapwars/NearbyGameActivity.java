package com.tgauch.tapwars;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.tgauch.tapwars.enums.Player;
import com.tgauch.tapwars.game.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Two phones, one player each, connected with Nearby Connections (Bluetooth / Wi-Fi, no internet
 * needed). Each player just taps anywhere on their own screen.
 *
 * <p>Both phones advertise and discover at the same time. To avoid both sides requesting a
 * connection at once, only the phone with the smaller random token asks; that phone becomes the
 * host (player ONE) and owns the game state. See {@link Message}.
 */
public class NearbyGameActivity extends GameActivity {

    private static final String TAG = "NearbyGameActivity";
    private static final String SERVICE_ID = "com.tgauch.tapwars";
    private static final Strategy STRATEGY = Strategy.P2P_POINT_TO_POINT;
    private static final char TOKEN_SEPARATOR = '#';
    private static final int REQUEST_PERMISSIONS = 1;

    private ConnectionsClient connections;
    private String myName;
    private final String myToken = UUID.randomUUID().toString().substring(0, 8);

    private String pendingEndpointId;
    private String opponentId;
    private String opponentName;
    private boolean isHost;
    private boolean iWantRematch;
    private boolean opponentWantsRematch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myName = MainActivity.getPlayerName(this);
        connections = Nearby.getConnectionsClient(this);
        battleGround.setPlayers(
                myName, ContextCompat.getColor(this, R.color.player_one),
                "", ContextCompat.getColor(this, R.color.player_two));

        String[] missing = missingPermissions();
        if (missing.length == 0) {
            startSearching();
        } else {
            showStatus(getString(R.string.need_permissions));
            ActivityCompat.requestPermissions(this, missing, REQUEST_PERMISSIONS);
        }
    }

    @Override
    protected void onDestroy() {
        connections.stopAdvertising();
        connections.stopDiscovery();
        connections.stopAllEndpoints();
        super.onDestroy();
    }

    // ---------------------------------------------------------------------------------------------
    // Permissions

    private static String[] requiredPermissions() {
        if (Build.VERSION.SDK_INT >= 33) {
            return new String[] {
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.NEARBY_WIFI_DEVICES,
            };
        } else if (Build.VERSION.SDK_INT >= 31) {
            return new String[] {
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
            };
        } else if (Build.VERSION.SDK_INT >= 29) {
            return new String[] {Manifest.permission.ACCESS_FINE_LOCATION};
        } else {
            return new String[] {Manifest.permission.ACCESS_COARSE_LOCATION};
        }
    }

    private String[] missingPermissions() {
        List<String> missing = new ArrayList<>();
        for (String permission : requiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                missing.add(permission);
            }
        }
        return missing.toArray(new String[0]);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_PERMISSIONS) {
            return;
        }
        if (missingPermissions().length == 0) {
            startSearching();
        } else {
            Toast.makeText(this, R.string.permissions_denied, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Finding and connecting to the other phone

    private void startSearching() {
        showStatus(getString(R.string.searching));
        String endpointName = myName + TOKEN_SEPARATOR + myToken;

        connections.startAdvertising(
                        endpointName,
                        SERVICE_ID,
                        lifecycleCallback,
                        new AdvertisingOptions.Builder().setStrategy(STRATEGY).build())
                .addOnFailureListener(e -> fail("Advertising failed", e));
        connections.startDiscovery(
                        SERVICE_ID,
                        discoveryCallback,
                        new DiscoveryOptions.Builder().setStrategy(STRATEGY).build())
                .addOnFailureListener(e -> fail("Discovery failed", e));
    }

    private final EndpointDiscoveryCallback discoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(
                        @NonNull String endpointId, @NonNull DiscoveredEndpointInfo info) {
                    if (opponentId != null || pendingEndpointId != null) {
                        return;
                    }
                    // Only one side asks, otherwise both requests collide.
                    if (myToken.compareTo(tokenOf(info.getEndpointName())) < 0) {
                        pendingEndpointId = endpointId;
                        connections.requestConnection(
                                        myName + TOKEN_SEPARATOR + myToken,
                                        endpointId,
                                        lifecycleCallback)
                                .addOnFailureListener(e -> {
                                    Log.w(TAG, "requestConnection failed", e);
                                    pendingEndpointId = null;
                                });
                    }
                }

                @Override
                public void onEndpointLost(@NonNull String endpointId) {}
            };

    private final ConnectionLifecycleCallback lifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(
                        @NonNull String endpointId, @NonNull ConnectionInfo info) {
                    if (opponentId != null
                            || (pendingEndpointId != null && !pendingEndpointId.equals(endpointId))) {
                        connections.rejectConnection(endpointId);
                        return;
                    }
                    pendingEndpointId = endpointId;
                    confirmOpponent(endpointId, info);
                }

                @Override
                public void onConnectionResult(
                        @NonNull String endpointId, @NonNull ConnectionResolution result) {
                    if (!endpointId.equals(pendingEndpointId)) {
                        return;
                    }
                    pendingEndpointId = null;
                    if (result.getStatus().isSuccess()) {
                        onOpponentConnected(endpointId);
                    } else {
                        opponentName = null;
                        showStatus(getString(R.string.searching));
                    }
                }

                @Override
                public void onDisconnected(@NonNull String endpointId) {
                    if (endpointId.equals(opponentId)) {
                        opponentId = null;
                        onOpponentLeft();
                    }
                }
            };

    private void confirmOpponent(String endpointId, ConnectionInfo info) {
        String name = nameOf(info.getEndpointName());
        String token = tokenOf(info.getEndpointName());
        new AlertDialog.Builder(this)
                .setTitle(R.string.found_player)
                .setMessage(getString(
                        R.string.confirm_opponent, name, info.getAuthenticationDigits()))
                .setCancelable(false)
                .setPositiveButton(R.string.play, (dialog, which) -> {
                    opponentName = name;
                    isHost = myToken.compareTo(token) < 0;
                    showStatus(getString(R.string.waiting_for, name));
                    connections.acceptConnection(endpointId, payloadCallback);
                })
                .setNegativeButton(R.string.no_thanks, (dialog, which) -> {
                    connections.rejectConnection(endpointId);
                    pendingEndpointId = null;
                    showStatus(getString(R.string.searching));
                })
                .show();
    }

    private void onOpponentConnected(String endpointId) {
        opponentId = endpointId;
        connections.stopAdvertising();
        connections.stopDiscovery();

        // Everyone sees themselves at the bottom of their own screen.
        Player me = isHost ? Player.ONE : Player.TWO;
        battleGround.setGame(game, me);
        battleGround.setPlayers(
                myName, ContextCompat.getColor(this, me == Player.ONE ? R.color.player_one : R.color.player_two),
                opponentName, ContextCompat.getColor(this, me == Player.ONE ? R.color.player_two : R.color.player_one));

        if (isHost) {
            beginRound();
        } else {
            showStatus(getString(R.string.get_ready));
        }
    }

    private void onOpponentLeft() {
        if (isFinishing()) {
            return;
        }
        handler.removeCallbacksAndMessages(null);
        hideEndButtons();
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.opponent_left,
                        opponentName == null ? getString(R.string.your_opponent) : opponentName))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> finish())
                .show();
    }

    private void fail(String what, Exception e) {
        Log.w(TAG, what, e);
        if (isFinishing()) {
            return;
        }
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.nearby_error, e.getMessage()))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> finish())
                .show();
    }

    // ---------------------------------------------------------------------------------------------
    // Playing

    private final PayloadCallback payloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
                    Message message = Message.decode(payload.asBytes());
                    if (message == null) {
                        Log.w(TAG, "Ignoring unknown payload");
                        return;
                    }
                    onMessage(message);
                }

                @Override
                public void onPayloadTransferUpdate(
                        @NonNull String endpointId, @NonNull PayloadTransferUpdate update) {}
            };

    private void onMessage(Message message) {
        switch (message.type) {
            case START:
                if (!isHost) {
                    beginRound();
                }
                break;
            case TAP:
                if (isHost) {
                    applyTap(Player.TWO);
                }
                break;
            case STATE:
                if (!isHost) {
                    game.setState(message.position, message.winner);
                    battleGround.invalidate();
                    if (message.winner != null) {
                        endRound(message.winner);
                    }
                }
                break;
            case REMATCH:
                opponentWantsRematch = true;
                if (!iWantRematch) {
                    showStatus(getString(R.string.wants_rematch, opponentName));
                }
                maybeStartRematch();
                break;
        }
    }

    private void send(Message message) {
        if (opponentId != null) {
            connections.sendPayload(opponentId, Payload.fromBytes(message.encode()));
        }
    }

    private void beginRound() {
        iWantRematch = false;
        opponentWantsRematch = false;
        if (isHost) {
            send(Message.start());
        }
        startRound();
    }

    /** Host only: applies a tap and tells the guest the new state. */
    private void applyTap(Player player) {
        if (!isPlaying()) {
            return;
        }
        Player winner = game.tap(player);
        send(Message.state(game.getPosition(), winner));
        battleGround.invalidate();
        if (winner != null) {
            endRound(winner);
        }
    }

    @Override
    public void onSideTapped(boolean bottomHalf) {
        if (!isPlaying()) {
            return;
        }
        if (isHost) {
            applyTap(Player.ONE);
        } else {
            send(Message.tap());
        }
    }

    @Override
    protected void onRoundEnded(Player winner) {
        boolean iWon = winner == (isHost ? Player.ONE : Player.TWO);
        String text = iWon ? getString(R.string.you_win) : getString(R.string.you_lose);
        battleGround.setMessages(text, null, false);
    }

    @Override
    protected void onRematchClicked() {
        iWantRematch = true;
        setRematchEnabled(false);
        send(Message.rematch());
        showStatus(getString(R.string.waiting_for, opponentName));
        maybeStartRematch();
    }

    private void maybeStartRematch() {
        if (isHost && iWantRematch && opponentWantsRematch) {
            beginRound();
        }
    }

    // ---------------------------------------------------------------------------------------------

    private void showStatus(String status) {
        battleGround.setMessages(status, null, false);
    }

    private static String nameOf(String endpointName) {
        int separator = endpointName.lastIndexOf(TOKEN_SEPARATOR);
        return separator < 0 ? endpointName : endpointName.substring(0, separator);
    }

    private static String tokenOf(String endpointName) {
        int separator = endpointName.lastIndexOf(TOKEN_SEPARATOR);
        return separator < 0 ? "" : endpointName.substring(separator + 1);
    }
}
