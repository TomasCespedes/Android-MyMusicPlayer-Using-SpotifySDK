package edu.stlawu.finalproject;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.client.Result;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.Capabilities;
import com.spotify.protocol.types.Image;
import com.spotify.protocol.types.ImageUri;
import com.spotify.protocol.types.LibraryState;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;
import com.spotify.protocol.types.Uri;
import com.spotify.protocol.types.UriWithOptionExtras;
import com.spotify.protocol.types.Uris;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

import static android.icu.lang.UCharacter.GraphemeClusterBreak.T;

/**
 * Source to set up the code to connect to Spotify
 * https://developer.spotify.com/documentation/android/quick-start/#next-steps
 */

public class MainActivity extends AppCompatActivity {

    private static final String CLIENT_ID = "377538ebcf9e4cdb9c4b5373e62a53a3";
    private static final String REDIRECT_URI = "FinalProjectCS450://callback";
    private SpotifyAppRemote mSpotifyAppRemote;


    // TextViews
    private TextView currentsong;

    // Buttons
    private ImageButton playpausebutton;
    private Button homebutton, searchbutton, librarybutton, playingbutton;

    // Tracker for song playing status (play or pause)
    private String currenttracker = "play";

    // ImageViews
    private ImageView song_iv;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    @Override
    protected void onStart() {
        super.onStart();

        /**
         * Set the connection parameters
         */
        ConnectionParams connectionParams =
                new ConnectionParams.Builder(CLIENT_ID)
                        .setRedirectUri(REDIRECT_URI)
                        .showAuthView(true).setPreferredImageSize(200)
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
    private void connected() {
        // Play a playlist
        mSpotifyAppRemote.getPlayerApi().play("spotify:track:0VgkVdmE4gld66l8iyGjgx");

        subscribetoPlayerState();

        /**
         * Button to play and pause the song
         */
        playpausebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // If song is currently playing, pause it
                if (currenttracker == "play") {
                    playpausebutton.setImageResource(R.drawable.playbutton);
                    currenttracker = "pause";
                    mSpotifyAppRemote.getPlayerApi().pause();
                }
                // If song is currently paused, play it
                else if (currenttracker == "pause") {
                    playpausebutton.setImageResource(R.drawable.pausebutton);
                    currenttracker = "play";
                    mSpotifyAppRemote.getPlayerApi().resume();

                }
            }
        });
    }

    private void subscribetoPlayerState() {
        // Subscribe to PlayerState
        mSpotifyAppRemote.getPlayerApi()
                .subscribeToPlayerState()
                .setEventCallback(new Subscription.EventCallback<PlayerState>() {


                    // If a song is playing get the track name and artist name
                    public void onEvent(PlayerState playerState) {
                        final Track track = playerState.track;
                        if (track != null) {
                            /**
                             * Set data to views (track name by track artist)
                             */
                            currentsong.setText((track.name + " by " + track.artist.name));
                            /**
                             * Get Image for the track
                             */
                            mSpotifyAppRemote.getImagesApi().getImage(track.imageUri)
                                    .setResultCallback(new CallResult.ResultCallback<Bitmap>() {
                                        @Override
                                        public void onResult(Bitmap bitmap) {
                                            Drawable d = new BitmapDrawable(getResources(), bitmap);
                                            song_iv.setImageDrawable(d);
                                        }
                                    });
                        }
                    }
                });
    }

    /**
     * When app stops, disconnect app from Spotify
     */
    @Override
    protected void onStop() {
        super.onStop();

        // Disconnect from app
        SpotifyAppRemote.disconnect(mSpotifyAppRemote);
    }

    private void init() {

        /**
         * Find views for current song, play/pause button
         */
        currentsong = findViewById(R.id.current_song);
        currentsong.setSelected(true);
        playpausebutton = findViewById(R.id.current_button);
        song_iv = findViewById(R.id.song_iv);


        /**
         * Find all the buttons for the bottom menu
         */
        homebutton = findViewById(R.id.homebtn);
        searchbutton = findViewById(R.id.searchbtn);
        librarybutton = findViewById(R.id.librarybtn);
        playingbutton = findViewById(R.id.playingbtn);

        /**
         * Home button to create a new activity
         */
        homebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, MainActivity.class));
            }
        });

        /**
         * Search button to create a new activity
         */
        searchbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SearchActivity.class));
            }
        });

        /**
         * Libray button to create a new activity
         */
        librarybutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, LibraryActivity.class));
            }
        });

        /**
         * Playing Button to create a new activity
         */
        playingbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, PlayingActivity.class));
            }
        });


    }
}

