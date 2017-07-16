package com.tgauch.tapwars.helpers;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Connections;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.tgauch.tapwars.enums.NearbyConnectionHelperState;
import com.tgauch.tapwars.interfaces.NearbyConnectionHelperListener;

import java.util.UUID;

public class NearbyConnectionHelper implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener
{

    ////////////////////////////////////////////////////////
    //
    //  These are static class constants
    //
    ////////////////////////////////////////////////////////
    private static final String TAG = "NearbyConnectionHelper";
    private static final String SERVICE_ID = "TapWars";

    ////////////////////////////////////////////////////////
    //
    //  These are private class variables
    //
    ////////////////////////////////////////////////////////
    private GoogleApiClient googleApiClient;
    private Context context;
    private NearbyConnectionHelperListener listener;
    private boolean isHost = false;
    private NearbyConnectionHelperState state = NearbyConnectionHelperState.UNKNOWN;
    private String name;
    private String connectedEndpoingId;

    //////////////////////////////////////////////////////////////////////////
    //
    //   The following are class objects used for callbacks to the nearby api
    //
    /////////////////////////////////////////////////////////////////////////
    private final ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(final String endpointId, ConnectionInfo connectionInfo) {
            Toast.makeText(context, "Connection with: " + connectionInfo.getEndpointName() + " endpointId: " + endpointId, Toast.LENGTH_LONG).show();
            connectedEndpoingId = endpointId;

            setState(NearbyConnectionHelperState.PLAYER_CONNECTED);
            Nearby.Connections.acceptConnection(googleApiClient, connectionInfo.getEndpointName(), payloadCallback)
            .setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(@NonNull Status status) {
                if (status.isSuccess()) {
                    Toast.makeText(context, "Connection accepted", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, "Connection rejected: " + status.toString(), Toast.LENGTH_LONG).show();
                }
                }
            });
        }

        @Override
        public void onConnectionResult(String s, ConnectionResolution connectionResolution) {
            Log.d(TAG, new Object(){}.getClass().getEnclosingMethod().getName());
            setState(NearbyConnectionHelperState.UNKNOWN);
        }

        @Override
        public void onDisconnected(String s) {
            Log.d(TAG, new Object(){}.getClass().getEnclosingMethod().getName());
            setState(NearbyConnectionHelperState.UNKNOWN);
        }
    };

    private final EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(final String endpointId, DiscoveredEndpointInfo discoveredEndpointInfo) {

            if (state.equals(NearbyConnectionHelperState.CONNECTING) || state.equals(NearbyConnectionHelperState.PLAYER_CONNECTED)) {
                return;
            }

            state.equals(NearbyConnectionHelperState.CONNECTING);
            // If we find an endpoint for a Tap war try to connect to the game
            if (SERVICE_ID.equals(discoveredEndpointInfo.getServiceId())) {
                Nearby.Connections.requestConnection(
                        googleApiClient,
                        name,
                        endpointId,
                        connectionLifecycleCallback)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        Toast.makeText(context, "Request status to: " + endpointId + ": " + status.toString(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        }

        @Override
        public void onEndpointLost(String s) {
            Toast.makeText(context, "Lost Endpoint " + s, Toast.LENGTH_LONG).show();
        }
    };

    private PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(String endpointId, Payload payload) {
            Toast.makeText(context, "Received: " + payload.toString() + " from: " + endpointId, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onPayloadTransferUpdate(String s, PayloadTransferUpdate payloadTransferUpdate) {

        }
    };

    ////////////////////////////////////////////////////////
    //
    //  These are class methods
    //
    ////////////////////////////////////////////////////////

    /**
     * The constructor for the class handles intialization
     *
     * @param context
     * @param listener
     */
    public NearbyConnectionHelper(Context context, NearbyConnectionHelperListener listener) {
        this.context = context;

        this.name = UUID.randomUUID().toString();

        this.googleApiClient = new GoogleApiClient.Builder(this.context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Nearby.CONNECTIONS_API)
                .build();

        this.listener = listener;
    }

    /**
     * Handles returning the api client
     *
     * @return GoogleApiClient
     */
    public GoogleApiClient getGoogleApiClient() {
        return this.googleApiClient;
    }

    /**
     * Starts the nearby api advertising for other devices to find it
     */
    public void startAdvertising() {
        Nearby.Connections.startAdvertising(
            this.googleApiClient,
                name,
                SERVICE_ID,
            this.connectionLifecycleCallback,
            new AdvertisingOptions(Strategy.P2P_CLUSTER))
            .setResultCallback(new ResultCallback<Connections.StartAdvertisingResult>() {
                @Override
                public void onResult(@NonNull Connections.StartAdvertisingResult startAdvertisingResult) {
                    setState(NearbyConnectionHelperState.LOOKING_FOR_MATCH);
                }
            });
    }

    /**
     * Stops the nearby api advertising
     */
    public void stopAdvertising() {
        Nearby.Connections.stopAdvertising(this.googleApiClient);
    }

    /**
     * Starts discovery of advertised connections
     */
    public void startDiscovery() {
        Nearby.Connections.startDiscovery(
            this.googleApiClient,
                SERVICE_ID,
            this.endpointDiscoveryCallback,
            new DiscoveryOptions(Strategy.P2P_CLUSTER))
            .setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(@NonNull Status status) {
                    if (status.isSuccess()) {
                        setState(NearbyConnectionHelperState.SEARCHING_FOR_PLAYER);
                    }
                }
            });
    }

    /**
     * Stops the nearby api from discovering connections
     */
    public void stopDiscovery() {
        Nearby.Connections.stopDiscovery(this.googleApiClient);
    }


    public void disconnect() {
        Nearby.Connections.disconnectFromEndpoint(googleApiClient, connectedEndpoingId);
    }

    /**
     * Called when the GoogleApiClient is connected
     *
     * @param bundle
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        this.setState(NearbyConnectionHelperState.GOOGLE_API_CONNECTED);
    }

    /**
     * Called when the GoogleApiClient is suspended
     * @param i
     */
    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, new Object(){}.getClass().getEnclosingMethod().getName());
        setState(NearbyConnectionHelperState.UNKNOWN);
    }

    /**
     * Called when a connection to the GoogleApiClient failed
     *
     * @param connectionResult
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, new Object(){}.getClass().getEnclosingMethod().getName());
        setState(NearbyConnectionHelperState.UNKNOWN);
    }

    /**
     * Sets the state of the connection
     *
     * @param state
     */
    private void setState(NearbyConnectionHelperState state) {
        this.state = state;
        this.listener.onStateChanged(this.state);
    }
}
