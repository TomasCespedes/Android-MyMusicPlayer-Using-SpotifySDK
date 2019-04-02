package edu.stlawu.finalproject;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.spotify.android.appremote.api.SpotifyAppRemote;

import java.util.ArrayList;

public class LibraryActivity extends AppCompatActivity {


    // TextViews
    private TextView currentsong;

    // Buttons
    private ImageButton playpausebutton;
    private Button homebutton, searchbutton, librarybutton, playingbutton;

    // ImageViews
    private ImageView song_iv;

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
            ArrayList<String> message = intent.getStringArrayListExtra("track-info");
            // Log message
            Log.d("receiver-library", "Got message: " + message);

            // Save the song's information
            tempsong = message.get(0);
            tempart = message.get(1);
            currentsong.setText((tempsong + " by " + tempart));
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        init();

        // Connect to the Remote Service
        bindService(new Intent(this, RemoteService.class), serviceConnection, Context.BIND_AUTO_CREATE);

        // Register the receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("main-activity"));
        // Update boolean tracker
        register_isBound = true;
    }

    @Override
    protected void onStart() {
        super.onStart();


        /**
         * Button to play and pause the song
         */
        // Playing / Pause buttton listener on main page
        playpausebutton.setOnClickListener(new View.OnClickListener() {
                                               @Override
                                               public void onClick(View v) {
           // Song is playing, so pause it
           if (remoteService.songstatus()) {
               // Pause song through remote
               remoteService.pause();
               // Change the picture to a play button
               playpausebutton.setImageResource(R.drawable.playbutton);
               // Change the tracker
               remoteService.songstatustracker = false;
           }
           // Song is paused, so resume it
           else {
               // Resume song through remote
               remoteService.resume();
               // Change the picture to a pause button
               playpausebutton.setImageResource(R.drawable.pausebutton);
               // Change the tracker to true
               remoteService.songstatustracker = true;
           }
       }
    }
        );
    }


    @Override
    protected void onResume() {
        super.onResume();


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
            service_isBound = true;
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

    @Override
    protected void onStop() {
        super.onStop();

        // Unregister message receiver to prevent data
        if (register_isBound) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
            register_isBound = false;
        }

        // Unbind the service to prevent data leaks
        if (service_isBound) {
            unbindService(serviceConnection);
            service_isBound = false;
        }
    }

    // Old send message function
    // Send message to the activities about the song that is playing
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

    private void init() {
        // Find views
        currentsong = findViewById(R.id.current_song2);
        currentsong.setSelected(true);
        playpausebutton = findViewById(R.id.current_button);
        song_iv = findViewById(R.id.song_iv);

        // Find all the buttons for the bottom menu
        homebutton = findViewById(R.id.homebtn);
        searchbutton = findViewById(R.id.searchbtn);
        librarybutton = findViewById(R.id.librarybtn);
        playingbutton = findViewById(R.id.playingbtn);

        /**
         * Buttons for bottom menu to create new activities
         */
        // Home button to create a new activity
        homebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //sendMessage("main-activity", "track-info");
                Intent myIntent = new Intent(LibraryActivity.this, MainActivity.class);
                LibraryActivity.this.startActivity(myIntent);
                finish();
            }
        });

        // Search button to create a new activity
        searchbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //sendMessage("main-activity", "track-info");
                Intent myIntent = new Intent(LibraryActivity.this, SearchActivity.class);
                LibraryActivity.this.startActivity(myIntent);
                finish();
            }
        });

        // Libray button to create a new activity
        librarybutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Do nothing
            }
        });

        // Playing Button to create a new activity
        playingbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //sendMessage("main-activity", "track-info");
                Intent myIntent = new Intent(LibraryActivity.this, PlayingActivity.class);
                LibraryActivity.this.startActivity(myIntent);
                finish();
            }
        });
    }






}
