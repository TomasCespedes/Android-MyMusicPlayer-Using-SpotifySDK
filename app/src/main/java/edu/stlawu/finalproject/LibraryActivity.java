package edu.stlawu.finalproject;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.PlaylistsPager;
import kaaes.spotify.webapi.android.models.SavedTrack;
import retrofit.client.Response;

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

    // Web API connection
    private SpotifyApi api = new SpotifyApi();
    private SpotifyService spotifyWebService;
    private String accessToken;
    // Client ID and Redirect URI gotten from Spotify
    private static final String CLIENT_ID = "377538ebcf9e4cdb9c4b5373e62a53a3";
    private static final String REDIRECT_URI = "FinalProjectCS450://callback";
    // Request code will be used to verify if result comes from the login activity. Can be set to any integer.
    private static final int REQUEST_CODE = 1337;

    // My Playlists array
    public List<PlaylistSimple> myPlaylists;
    public LinearLayout myPlaylistsView;


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

        getToken();


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

    // Method to get the authentication token to be able
    // to use the web API wrapped
    private void getToken() {
        AuthenticationRequest.Builder builder =
                new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI);

        builder.setScopes(new String[]{"streaming"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);

    }

    /**
     * onActivityResult from Login Activity.
     * Used for getting authentication token to use Web API.
     * @param requestCode
     * @param resultCode
     * @param intent
     */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);

            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    // Handle successful response

                    // Get the Access Token from the response
                    accessToken = response.getAccessToken();
                    Log.d("Token", accessToken);

                    // Set the Access token so we can use API
                    api.setAccessToken(accessToken);
                    spotifyWebService = api.getService();


                    // Get user's playlists
                    spotifyWebService.getMyPlaylists(new SpotifyCallback<Pager<PlaylistSimple>>() {
                        // Request failed
                        @Override
                        public void failure(SpotifyError spotifyError) {
                            Log.e("error-library-playlists", spotifyError.getMessage());
                        }

                        // Request was Successful
                        @Override
                        public void success(Pager<PlaylistSimple> playlistSimplePager, Response response) {
                            // Save the user's playlists
                            myPlaylists = playlistSimplePager.items;
                            init_playlists();
                        }
                    });

                    // TODO For some reason mysavedtracks returns an error
                    // TODO Insufficent Client Scope 403 is error
                    spotifyWebService.getMySavedTracks(new SpotifyCallback<Pager<SavedTrack>>() {
                        @Override
                        public void failure(SpotifyError spotifyError) {
                            Log.e("error-library-songs", spotifyError.toString());
                        }

                        @Override
                        public void success(Pager<SavedTrack> savedTrackPager, Response response) {
                            Log.e("Savedsongs", response.getReason());
                            Log.e("Savedsongs", savedTrackPager.items.toString());
                        }
                    });

                    break;

                // Auth flow returned an error
                case ERROR:
                    // Handle error response
                    Log.e("SpotifyErrors", response.getError());
                    break;

                // Most likely auth flow was cancelled
                default:
                    // Handle other cases
                    Log.e("SpotifyErrors", "WHY YOU CANCEL?");
            }
        }
    }


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

    private void init_playlists() {
        // Find playlist view
        myPlaylistsView = findViewById(R.id.users_playlists);

        // Dynamically add buttons for each playlist
        for (int i = 0; i <= myPlaylists.size() - 1; i++) {
            // New button
            Button myButton = new Button (this);
            // Set name of playlist to new button
            myButton.setText(myPlaylists.get(i).name);
            // Make button size 300x300
            myButton.setLayoutParams(new LinearLayout.LayoutParams(300, 300));
//            Log.d("urls", myPlaylists.get(i).id);

            // Add button to view
            myPlaylistsView.addView(myButton);
        }
    }

}
