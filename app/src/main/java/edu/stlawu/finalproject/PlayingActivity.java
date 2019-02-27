package edu.stlawu.finalproject;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.types.Track;

import java.util.ArrayList;

public class PlayingActivity extends AppCompatActivity {

    // Client ID
    // TODO do we need?
    private static final String CLIENT_ID = "377538ebcf9e4cdb9c4b5373e62a53a3";
    private static final String REDIRECT_URI = "FinalProjectCS450://callback";

    // TextViews
    private TextView currentsong;

    // TODO
    // Buttons
    private ImageButton playpausebutton, nextbutton, previousbutton;
    private Button homebutton, searchbutton, librarybutton, playingbutton;

    // TODO something with song view for cover art
    // ImageViews
    private ImageView song_iv;

    // Volume Controls
    private SeekBar volumeSeekbar;
    private AudioManager audioManager;
    private SpotifyAppRemote mSpotifyAppRemote;

    // ServiceConnection
    RemoteService remoteService;
    boolean service_isBound = false;
    boolean register_isBound = false;

    // Remote service connection set up
    public ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            RemoteService.MyBinder binder = (RemoteService.MyBinder) service;
            remoteService = binder.getService();
            service_isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            remoteService = null;
            service_isBound = false;
        }
    };

    // Song information trackers
    private String tempsong, tempart;

    // handler for received Intents for the "my-event" event
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            ArrayList<String> message = intent.getStringArrayListExtra("playing");
            Log.d("receiver-playing", "Got message: " + message);

            // Save the song's information
            tempsong = message.get(0);
            tempart = message.get(1);
            currentsong.setText((tempsong + " by " + tempart));
        }

    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playing);

        // Volume bar controls
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Initialize all variables
        initControls();

        // Connect to the Remote Service
        bindService(new Intent(this, RemoteService.class), serviceConnection, Context.BIND_AUTO_CREATE);



    }

    @Override
    protected void onStart() {
        super.onStart();

        init();
        initButtons();

        //connectToRemote();


    }


    @Override
    protected void onResume() {
        super.onResume();

        // Register mMessageReceiver to receive messages.
        // This is to send information from service.
        if (!register_isBound) {
            LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                    new IntentFilter("playing-activity"));
            register_isBound = true;
        }

        // Register the service to control the spotify remote
        if (!service_isBound) {
            // Connect to the Remote Service
            bindService(new Intent(this, RemoteService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister message receiver to prevent data leaks
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        register_isBound = false;

        // Unbind the service to prevent data leaks
        unbindService(serviceConnection);
        service_isBound = false;
    }


    private void init() {
        // Find views
            // Song
        currentsong = findViewById(R.id.songName);
        song_iv = findViewById(R.id.songPicture);

            // Buttons
        playpausebutton = findViewById(R.id.playingPauseStop);
        nextbutton = findViewById(R.id.nextSong);
        previousbutton = findViewById(R.id.prevSong);

        // Find all the buttons for the bottom menu
        homebutton = findViewById(R.id.homebtn);
        searchbutton = findViewById(R.id.searchbtn);
        librarybutton = findViewById(R.id.librarybtn);
        playingbutton = findViewById(R.id.playingbtn);

        // Home button to create a new activity
        homebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Send information to the main activity and start the activity
                main_sendMessage();
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

        // Playing Activity button
        playingbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(PlayingActivity.this, PlayingActivity.class);
                PlayingActivity.this.startActivity(myIntent);
            }
        });

    }

    // Volume bar and audio manager setup
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

    private void initButtons() {
        // Previous song button
        previousbutton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                remoteService.previous();
            }
        });

        // Next song button
        nextbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                remoteService.next();
            }
        });

        // Play / Pause button
        playpausebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Song is playing, so pause it
                if (remoteService.songstatus()) {
                    remoteService.pause();
                    playpausebutton.setImageResource(R.drawable.playbutton);
                }
                // Song is paused, so resume it
                else if (!remoteService.songstatus()) {
                    remoteService.resume();
                    playpausebutton.setImageResource(R.drawable.pausebutton);
                }
            }
        });


    }

    // Send message to the main-activity
    private void main_sendMessage() {
        // Create a new main-activity intent
        Intent intent = new Intent("main-activity");

        // Array to hold data
        ArrayList<String> songinformation = new ArrayList<String>();

        // Add song and artist names
        songinformation.add(tempsong);
        songinformation.add(tempart);

        // Send the message
        intent.putStringArrayListExtra("track-info",  songinformation);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

//    private void connectToRemote() {
//        /**
//         * Set the connection parameters
//         */
//        ConnectionParams connectionParams =
//                new ConnectionParams.Builder(CLIENT_ID)
//                        .setRedirectUri(REDIRECT_URI)
//                        .showAuthView(true)
//                        .build();
//
//        /**
//         * Connect app to remote
//         */
//        SpotifyAppRemote.connect(this, connectionParams,
//                new Connector.ConnectionListener() {
//
//                    @Override
//                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {
//                        mSpotifyAppRemote = spotifyAppRemote;
//                        Log.d("MainActivity", "Connected! Yay!");
//
//                        // Now you can start interacting with App Remote
//                        connected();
//                    }
//
//                    @Override
//                    public void onFailure(Throwable throwable) {
//                        Log.e("MainActivity", throwable.getMessage(), throwable);
//
//                        // Something went wrong when attempting to connect! Handle errors here
//                    }
//                });
//    }
//
//    private void connected() {
//
//        /**
//         * Subscribe to playerstate in order to get song information
//         */
//        mSpotifyAppRemote.getPlayerApi().subscribeToPlayerState()
//                .setEventCallback(new Subscription.EventCallback<PlayerState>()
//             {
//                 @Override
//                 public void onEvent(PlayerState playerState) {
//                     track = playerState.track;
//                     if (track != null) {
//                         /**
//                          * Set data to views (track name by track artist)
//                          */
//                         currentsong.setText((track.name + " by " + track.artist.name));
//                         /**
//                          * Get Image for the track
//                          */
//                         mSpotifyAppRemote.getImagesApi().getImage(track.imageUri)
//                                 .setResultCallback(new CallResult.ResultCallback<Bitmap>() {
//                                     @Override
//                                     public void onResult(Bitmap bitmap) {
//                                         Drawable d = new BitmapDrawable(getResources(), bitmap);
//                                         song_iv.setImageDrawable(d);
//                                     }
//                                 });
//                     }
//                 }
//             }
//
//            );
//
//        /**
//         * Button to play and pause the song
//         */
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
//
//                    currenttracker = "play";
//                    mSpotifyAppRemote.getPlayerApi().resume();
//
//                }
//            }
//        });
//
//        /**
//         * Button to play the next song
//         */
//
//        nextbutton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mSpotifyAppRemote.getPlayerApi().skipNext();
//            }
//        });
//    }
}
