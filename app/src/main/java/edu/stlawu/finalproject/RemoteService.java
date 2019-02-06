package edu.stlawu.finalproject;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

public class RemoteService extends Service {
    // Client ID and Redirect URI gotten from Spotify
    private static final String CLIENT_ID = "377538ebcf9e4cdb9c4b5373e62a53a3";
    private static final String REDIRECT_URI = "FinalProjectCS450://callback";

    // App remote for Spotify to control song playback
    public SpotifyAppRemote mSpotifyAppRemote;

    // Binder for communication channel
    public IBinder myBinder = new MyBinder();

    public RemoteService() {

    }

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
        /**
         * Set the connection parameters
         */
        ConnectionParams connectionParams =
                new ConnectionParams.Builder(CLIENT_ID)
                        .setRedirectUri(REDIRECT_URI)
                        .showAuthView(true)
                        .build();

        /**
         * Connect app to remote
         */
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
        // Play a playlist
        //mSpotifyAppRemote.getPlayerApi().play("spotify:track:5274I4mUMnYczyeXkGDWZN");

        Log.e("Hello!", "connected");

//        // Button for playing/pausing the current song
//        playpausebutton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                // If song is currently playing, pause it
//                if (currenttracker == "play") {
//                    playpausebutton.setImageResource(R.drawable.playbutton);
//                    currenttracker = "pause";
//                    mSpotifyAppRemote.getPlayerApi().pause();
//                }
//                // If song is currently paused, play it
//                else if (currenttracker == "pause") {
//                    playpausebutton.setImageResource(R.drawable.pausebutton);
//                    currenttracker = "play";
//                    mSpotifyAppRemote.getPlayerApi().resume();
//
//                }
//            }
//        });
    }

    public void pause() {
        mSpotifyAppRemote.getPlayerApi().pause();
    }

    public void resume() {
        mSpotifyAppRemote.getPlayerApi().resume();
    }

    public void next() {
        mSpotifyAppRemote.getPlayerApi().skipNext();
    }

    public void previous() {
        mSpotifyAppRemote.getPlayerApi().skipPrevious();
    }

    public void play(String songuri) {
        mSpotifyAppRemote.getPlayerApi().play(songuri);
    }




}
