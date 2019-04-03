package edu.stlawu.finalproject;

import com.spotify.protocol.types.Track;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
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

import java.util.ArrayList;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import retrofit.client.Response;


/**
 * Source to set up the code to connect to Spotify
 * https://developer.spotify.com/documentation/android/quick-start/#next-steps
 */

public class MainActivity extends AppCompatActivity {

    // App remote for Spotify to control song playback
    public static Track track;

    // TextViews
    public static TextView currentsong;

    public ImageView song_iv;

    // Buttons
    private ImageButton playpausebutton;
    private Button homebutton, searchbutton, librarybutton, playingbutton;

    // Web API
//    private String accessToken;
//    private SpotifyApi api = new SpotifyApi();
//    private SpotifyService spotifyWebService;

    // ServiceConnection
    RemoteService remoteService;
    boolean service_isBound = false;
    boolean reciever_isbound = false;

    // Temp variables for testing
    public String tempsong, tempart;


    // Remote service connection set up
    public ServiceConnection serviceConnection = new ServiceConnection() {

        // Service successfully connected
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            RemoteService.MyBinder binder = (RemoteService.MyBinder) service;
            remoteService = binder.getService();
            service_isBound = true;
        }

        // Service was disconnected
        @Override
        public void onServiceDisconnected(ComponentName name) {
            remoteService = null;
            service_isBound = false;
        }
    };

    // handler for received Intents for the "track-name" event
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            ArrayList<String> message = intent.getStringArrayListExtra("track-info");
            Log.d("receiver", "Got message: " + message);

            // Save the song information
            tempsong = message.get(0);
            tempart = message.get(1);

            // Set the textview to appropriate data values
            currentsong.setText((tempsong + " by " + tempart));
        }

    };

    // OnCreate Method
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize all the views and buttons
        init();

        // Connect to the Remote Service
        bindService(new Intent(this, RemoteService.class), serviceConnection, Context.BIND_AUTO_CREATE);

        // Register the receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("main-activity"));
        // Update boolean tracker
        reciever_isbound = true;


    }


    @Override
    protected void onStart() {
        super.onStart();

        // Call the method to get the token
        //getToken();

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

    // When app gets resumed
    @Override
    protected void onResume() {
        super.onResume();

        // Check if service is bound already to not double call
        // This could lead to a data leak
        if (!service_isBound) {
            // Binds the service so we can use
            bindService(new Intent(this, RemoteService.class), serviceConnection, Context.BIND_AUTO_CREATE);
            // Update boolean tracker
            service_isBound = true;
        }

        // Register mMessageReceiver to receive messages.
        // This is to send information from service.
        if (!reciever_isbound) {
            LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                    new IntentFilter("main-activity"));
            reciever_isbound = true;
        }
    }

    // If activity is paused
    @Override
    protected void onPause() {
        super.onPause();

        // Need to unbind our service to prevent leaks
        unbindService(serviceConnection);
        // update service boolean to false
        service_isBound = false;
        // Need to unbind our reciever to prevent leaks
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        // update register boolean to false
        reciever_isbound = false;
    }



    // App is stopped
    @Override
    protected void onStop() {
        super.onStop();

        // Disconnect from app
        remoteService.disconnect();
    }

    // Initialize all the views and anything else we need
    private void init() {

        // Find all the views for current song, play/pause button,
        // and the song image view
        currentsong = findViewById(R.id.current_song);
        currentsong.setSelected(true);
        playpausebutton = findViewById(R.id.current_button);

        // Get rid of this and it's variables
        song_iv = findViewById(R.id.song_iv);


        // Get all the Button views for the navigation menu at bottom
        homebutton = findViewById(R.id.homebtn);
        homebutton.setEnabled(false);
        searchbutton = findViewById(R.id.searchbtn);
        librarybutton = findViewById(R.id.librarybtn);
        playingbutton = findViewById(R.id.playingbtn);

        // Home button switch activity
        homebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Do nothing :)
            }
        });

        // TODO 1
        // Search button switch activity
        searchbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //sendMessage("main-activity", "track-info");
                Intent myIntent = new Intent(MainActivity.this, SearchActivity.class);
                MainActivity.this.startActivity(myIntent);
                finish();
            }
        });

        // Library button switch activity
        librarybutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //sendMessage("main-activity", "track-info");
                Intent myIntent = new Intent(MainActivity.this, LibraryActivity.class);
                MainActivity.this.startActivity(myIntent);
                finish();
            }
        });

        // Playing button switch activity
        playingbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //sendMessage("main-activity", "track-info");
                Intent myIntent = new Intent(MainActivity.this, PlayingActivity.class);
                MainActivity.this.startActivity(myIntent);
                finish();
            }
        });
    }

    // Old send message function
    // Send a message to the playing activity
//    private void sendMessage(String action, String name) {
//        Intent intent = new Intent(action);
//        // add data
//        ArrayList<String> songinformation = new ArrayList<String>();
//        songinformation.add(remoteService.track.name);
//        songinformation.add(remoteService.track.artist.name);
//
//        intent.putStringArrayListExtra(name,  songinformation);
//        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
//    }
}

