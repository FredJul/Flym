/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fred.feedex;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.TimeUnit;

public class AskDataService extends IntentService implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = AskDataService.class.getSimpleName();

    // Timeout for making a connection to GoogleApiClient (in milliseconds).
    private static final long CONNECTION_TIME_OUT_MS = 1000;
    private GoogleApiClient mGoogleApiClient;

    public AskDataService() {
        super(AskDataService.class.getSimpleName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "AskDataService.onCreate");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        ConnectionResult connResult = mGoogleApiClient.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);

        Log.d(TAG, "AskDataService.onHandleIntent " + connResult);

        if (mGoogleApiClient.isConnected()) {
            sendStartMessage();
        } else {
            Log.e(TAG, "Failed to ask data - Client disconnected from Google Play Services");
        }
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "AskDataService.onConnected");
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "AskDataService.onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d(TAG, "AskDataService.onConnectionFailed " + result);
    }

    private void sendStartMessage() {
        NodeApi.GetConnectedNodesResult rawNodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        for (final Node node : rawNodes.getNodes()) {
            Log.v(TAG, "Node: " + node.getId());
            Wearable.MessageApi.sendMessage(
                    mGoogleApiClient,
                    node.getId(),
                    "/ask-data",
                    null
            );
        }
    }
}
