package edu.stlawu.finalproject;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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

import com.spotify.protocol.client.CallResult;

import java.util.ArrayList;

public class PlayingActivity extends AppCompatActivity {

    // TextViews
    private TextView currentsong;

    // Buttons
    private ImageButton playpausebutton, nextbutton, previousbutton;
    private Button homebutton, searchbutton, librarybutton, playingbutton;

    // ImageViews
    private ImageView song_iv;

    // Volume Controls
    private SeekBar volumeSeekbar;
    private AudioManager audioManager;

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
            // Extract data included in the Intent (for track information)
            ArrayList<String> message = intent.getStringArrayListExtra("track-info");
            Log.d("receiver-playing", "Got message: " + message);

            // Save the song's information
            tempsong = message.get(0);
            tempart = message.get(1);
            currentsong.setText((tempsong + "\n by " + tempart));

            // Update the song view for the new cover of song.
            remoteService.getImageBitmap().setResultCallback(new CallResult.ResultCallback<Bitmap>() {
                @Override
                public void onResult(Bitmap bitmap) {
                    Drawable d = new BitmapDrawable(getResources(), bitmap);
                    song_iv.setImageDrawable(d);


                }
            });
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playing);

        // Volume bar controls
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Initialize all variables
        initVolumeControls();

        // Connect to the Remote Service
        bindService(new Intent(this, RemoteService.class), serviceConnection, Context.BIND_AUTO_CREATE);

        // Register the receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("main-activity"));
        register_isBound = true;


    }

    @Override
    protected void onStart() {
        super.onStart();

        init();
        initButtons();

    }


    @Override
    protected void onResume() {
        super.onResume();

        //init();
        //initButtons();

        // Register mMessageReceiver to receive messages.
        // This is to send information from service.
        if (!register_isBound) {
            LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                    new IntentFilter("main-activity"));
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
                //sendMessage("main-activity", "track-info");
                Intent myIntent = new Intent(PlayingActivity.this, MainActivity.class);
                PlayingActivity.this.startActivity(myIntent);
                finish();
            }
        });

        // Search button to create a new activity
        searchbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //sendMessage("main-activity", "track-info");
                Intent myIntent = new Intent(PlayingActivity.this, SearchActivity.class);
                PlayingActivity.this.startActivity(myIntent);
                finish();
            }
        });

        // Libray button to create a new activity
        librarybutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //sendMessage("main-activity", "track-info");
                Intent myIntent = new Intent(v.getContext(), LibraryActivity.class);
                PlayingActivity.this.startActivity(myIntent);
                finish();
            }
        });

        // Playing Activity button
        playingbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Do nothing :D
            }
        });

    }

    // Volume bar and audio manager setup
    private void initVolumeControls() {
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

    // Old send message function
    // Send message to the main-activity
//    private void sendMessage(String action, String name) {
//        // Create a new main-activity intent
//        Intent intent = new Intent(action);
//
//        // Array to hold data
//        ArrayList<String> songinformation = new ArrayList<String>();
//
//        // Add song and artist names
//        songinformation.add(tempsong);
//        songinformation.add(tempart);
//
//        // Send the message
//        intent.putStringArrayListExtra(name,  songinformation);
//        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
//    }

}
