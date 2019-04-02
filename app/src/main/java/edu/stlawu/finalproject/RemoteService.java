package edu.stlawu.finalproject;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.ImageView;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.ImageUri;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;

import java.util.ArrayList;

public class RemoteService extends Service {
    // Client ID and Redirect URI gotten from Spotify
    private static final String CLIENT_ID = "377538ebcf9e4cdb9c4b5373e62a53a3";
    private static final String REDIRECT_URI = "FinalProjectCS450://callback";

    // App remote for Spotify to control song playback
    public SpotifyAppRemote mSpotifyAppRemote;

    // Track variable
    public Track track;

    // Binder for communication channel
    public IBinder myBinder = new MyBinder();

    // Trackers
    // Whether song is playing or not
    public Boolean songstatustracker = false;
    // Whether the remote is connected or not
    public Boolean connected = false;

    // Constructor
    public RemoteService() {

    }

    // Methods

    @Override
    public void onCreate() {
        super.onCreate();
        connectToRemote();

    }

    @Override
    public boolean onUnbind(Intent intent) {
        return false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public class MyBinder extends Binder {
        RemoteService getService() {
            return RemoteService.this;
        }
    }


    /**
     * Connect to the remote so user can
     * interact with songs
     */
    public void connectToRemote() {
        // Set the connection parameters
        ConnectionParams connectionParams =
                new ConnectionParams.Builder(CLIENT_ID)
                        .setRedirectUri(REDIRECT_URI)
                        .showAuthView(true)
                        .build();

        // Connect to the app remote
        SpotifyAppRemote.connect(this, connectionParams,
                new Connector.ConnectionListener() {

                    @Override
                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                        mSpotifyAppRemote = spotifyAppRemote;
                        Log.d("MainActivity", "Connected! Yay!");

                        // Now you can start interacting with App Remote
                        connected();
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        Log.e("MainActivity", throwable.getMessage(), throwable);

                        // Something went wrong when attempting to connect! Handle errors here
                    }
                });

    }


    /**
     * Our app is connected to spotify and streaming
     * can now happen.
     */
    public void connected() {
        // Play a song
        connected = true;

        subscribetoPlayerState();



    }

    // Subscribe to the player's state
    // Updates us when there is a change in the song and sends it over to the main activity.
    private void subscribetoPlayerState() {
        mSpotifyAppRemote.getPlayerApi().subscribeToPlayerState().setEventCallback(
                new Subscription.EventCallback<PlayerState>() {
                    @Override
                    public void onEvent(PlayerState playerState) {
                        track = playerState.track;

                        // Send song information to MainActivity when it
                        if (track != null) {
                            main_sendMessage();
                        }
                    }
                }
        );
    }

    // Send a message to main-activity
    private void main_sendMessage() {
        Intent intent = new Intent("main-activity");
        // add data
        ArrayList<String> songinformation = new ArrayList<String>();

        songinformation.add(track.name);
        songinformation.add(track.artist.name);

        intent.putStringArrayListExtra("track-info",  songinformation);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // Pause the song
    public void pause() {
        mSpotifyAppRemote.getPlayerApi().pause();
        songstatustracker = false;
    }

    // Resume the song
    public void resume() {
        mSpotifyAppRemote.getPlayerApi().resume();
        songstatustracker = true;
    }

    // Skip to the next song
    public void next() {
        mSpotifyAppRemote.getPlayerApi().skipNext();
        songstatustracker = true;

    }

    // Go to previous song
    public void previous() {
        mSpotifyAppRemote.getPlayerApi().skipPrevious();
        songstatustracker = true;
    }

    // Play any song
    public void play(String songuri) {
        mSpotifyAppRemote.getPlayerApi().play(songuri);
        songstatustracker = true;
    }

    // Return whether a song is playing or not
    public boolean songstatus() {
        return songstatustracker;
    }

    // Disconnect from the app
    public void disconnect() {
        // Disconnect from app
        SpotifyAppRemote.disconnect(mSpotifyAppRemote);
    }

    public CallResult<Bitmap> getImageBitmap() {
        return mSpotifyAppRemote.getImagesApi().getImage(track.imageUri);

    }



}
