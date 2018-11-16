package edu.stlawu.finalproject;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;

public class PlayingActivity extends AppCompatActivity {

    // Client ID
    private static final String CLIENT_ID = "377538ebcf9e4cdb9c4b5373e62a53a3";
    private static final String REDIRECT_URI = "FinalProjectCS450://callback";

    // TextViews
    private TextView currentsong;
    // Buttons
    private ImageButton playpausebutton;
    private Button homebutton, searchbutton, librarybutton, playingbutton;
    // Tracker for song playing status (play or pause)
    private String currenttracker = "play";
    // ImageViews
    private ImageView song_iv;

    private Track track;
    // Volume Controls
    private SeekBar volumeSeekbar;
    private AudioManager audioManager;
    private SpotifyAppRemote mSpotifyAppRemote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playing);

        /**
         * Controls for Volume Bar
         */
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        initControls();


    }

    @Override
    protected void onStart() {
        super.onStart();

        init();

        connectToRemote();


    }
    private void init() {
        // Find views
        currentsong = findViewById(R.id.songName);
        song_iv = findViewById(R.id.songPicture);
        playpausebutton = findViewById(R.id.playingPauseStop);

        // Find all the buttons for the bottom menu
        homebutton = findViewById(R.id.homebtn);
        searchbutton = findViewById(R.id.searchbtn);
        librarybutton = findViewById(R.id.librarybtn);
        playingbutton = findViewById(R.id.playingbtn);

        // Home button to create a new activity
        homebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(PlayingActivity.this, MainActivity.class);
                PlayingActivity.this.startActivity(myIntent);
            }
        });

        // Search button to create a new activity
        searchbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(PlayingActivity.this, SearchActivity.class);
                PlayingActivity.this.startActivity(myIntent);
            }
        });

        // Libray button to create a new activity
        librarybutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(PlayingActivity.this, LibraryActivity.class);
                PlayingActivity.this.startActivity(myIntent);
            }
        });

        playingbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(PlayingActivity.this, PlayingActivity.class);
                PlayingActivity.this.startActivity(myIntent);
            }
        });

    }

    private void initControls() {
        try
        {
            volumeSeekbar = (SeekBar)findViewById(R.id.volumebar);
            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            volumeSeekbar.setMax(audioManager
                    .getStreamMaxVolume(AudioManager.STREAM_MUSIC));
            volumeSeekbar.setProgress(audioManager
                    .getStreamVolume(AudioManager.STREAM_MUSIC));


            volumeSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
            {
                @Override
                public void onStopTrackingTouch(SeekBar arg0)
                {
                }

                @Override
                public void onStartTrackingTouch(SeekBar arg0)
                {
                }

                @Override
                public void onProgressChanged(SeekBar arg0, int progress, boolean arg2)
                {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                            progress, 0);
                }
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void connectToRemote() {
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

    private void connected() {

        /**
         * Subscribe to playerstate in order to get song information
         */
        mSpotifyAppRemote.getPlayerApi().subscribeToPlayerState()
                .setEventCallback(new Subscription.EventCallback<PlayerState>()
             {
                 @Override
                 public void onEvent(PlayerState playerState) {
                     track = playerState.track;
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
             }

            );

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
}
