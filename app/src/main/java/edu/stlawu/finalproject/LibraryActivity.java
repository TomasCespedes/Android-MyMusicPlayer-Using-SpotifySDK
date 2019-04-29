package edu.stlawu.finalproject;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import java.util.ArrayList;
import java.util.List;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.Playlist;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import kaaes.spotify.webapi.android.models.SavedTrack;
import kaaes.spotify.webapi.android.models.Track;
import retrofit.client.Response;

public class LibraryActivity extends AppCompatActivity {

    // TextViews
    private TextView currentsong;

    // Songs
    List<SavedTrack> likedsongs;

    // Buttons
    private ImageButton playpausebutton;
    private Button homebutton, searchbutton, librarybutton, playingbutton;

    // GestorDetector for next/previous song
    private GestureDetector nextPrevSong;

    // ServiceConnection
    RemoteService remoteService;
    boolean service_isBound = false;
    boolean register_isBound = false;

    // View for liked songs
    public LinearLayout songs_view;


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
            // Set the text to the information above
            currentsong.setText((tempsong + " by " + tempart));
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        // Initialize all views
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

        // Call getToken method to get authentication token for Web API
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
        }});

        // Swipe left gesture to skip track
        nextPrevSong = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener(){
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
            {
                // Next track if velX < 0 else previous track
                if (velocityX < 0){
                    remoteService.next();
                } else {
                    remoteService.previous();
                }
                return false;
            }
        });

        currentsong.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                nextPrevSong.onTouchEvent(event);
                return true;
            }
        });

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

    // Method to get the authentication token to be able
    // to use the web API wrapped
    private void getToken() {
        AuthenticationRequest.Builder builder =
                new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI);

        // Set the scopes so Web API can use all parameters
        builder.setScopes(new String[]{"streaming", "user-library-read", "playlist-read-private", "user-read-private"});
        // Build request
        AuthenticationRequest request = builder.build();
        // Open login activity
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
                    // Get the Web API service
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
                            // Initialize all playlists with this result
                            init_playlists();
                        }
                    });

                    // Get users liked songs
                    spotifyWebService.getMySavedTracks(new SpotifyCallback<Pager<SavedTrack>>() {
                        // Request failed (Most likely due to authentication limits)
                        @Override
                        public void failure(SpotifyError spotifyError) {
                            Log.d("Song_Error", spotifyError.getMessage());
                        }

                        // Request was successful
                        @Override
                        public void success(Pager<SavedTrack> savedTrackPager, Response response) {
                            Log.d("User_Songs", savedTrackPager.items.toString());
                            likedsongs = savedTrackPager.items;
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
                    Log.e("SpotifyErrors", "Authentication Flow was canceled?");
            }
        }
    }

    // Initialize all the views and permanent buttons
    private void init() {
        // Find views
        currentsong = findViewById(R.id.current_song2);
        currentsong.setSelected(true);
        playpausebutton = findViewById(R.id.current_button);

        // Find all the buttons for the bottom menu
        homebutton = findViewById(R.id.homebtn);
        searchbutton = findViewById(R.id.searchbtn);
        librarybutton = findViewById(R.id.librarybtn);
        playingbutton = findViewById(R.id.playingbtn);

        // Scroll View for portraying songs within each playlist
        songs_view = findViewById(R.id.scrollview_likedsongs);

        //Buttons for bottom menu to create new activities
        // Home button to create a new activity
        homebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(LibraryActivity.this, MainActivity.class);
                LibraryActivity.this.startActivity(myIntent);
                finish();
            }
        });

        // Search button to create a new activity
        searchbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                Intent myIntent = new Intent(LibraryActivity.this, PlayingActivity.class);
                LibraryActivity.this.startActivity(myIntent);
                finish();
            }
        });
    }

    // Initialize all the playlists and their songs
    private void init_playlists() {
        // Find playlist view
        myPlaylistsView = findViewById(R.id.users_playlists);

        // Create a new button for liked songs for playlist view
        Button likedsongs_button = new Button(LibraryActivity.this);
        // Make button size 300x300
        likedsongs_button.setLayoutParams(new LinearLayout.LayoutParams(300, 300));
        // Set text of playlist to Liked Songs
        likedsongs_button.setText("Liked Songs");
        // Add new button to the playlists view
        myPlaylistsView.addView(likedsongs_button);

        // User clicks liked Songs button
        // Hard-coded because everyone has a liked songs "playlist"
        likedsongs_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                songs_view.removeAllViews();
                // Loop through all the results to portray liked songs
                for (int i = 0; i < likedsongs.size() - 1; i++) {
                    // Get a track from the results
                    final Track trackcanplay = likedsongs.get(i).track;
                    // New button for that track
                    Button myButton = new Button(LibraryActivity.this);
                    // Make button size 1280x110
                    myButton.setLayoutParams(new LinearLayout.LayoutParams(1280, 110));
                    // Set background of button
                    myButton.setBackgroundResource(R.drawable.buttonshape);
                    // Set text of button to name and artist of song
                    myButton.setText((trackcanplay.name + " By " + trackcanplay.artists.get(0).name));
                    // If the button is clicked on, play the song that was chosen
                    myButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Play song
                            remoteService.play(trackcanplay.uri);
                        }
                    });
                    // Add the button to the linear layout of the scrollview
                    songs_view.addView(myButton);
                }
            }
        });

        // Dynamically add buttons for each playlist
        for (int i = 0; i <= myPlaylists.size() - 1; i++) {
            final PlaylistSimple currentplaylist = myPlaylists.get(i);
            // New button for each playlist
            Button myButton = new Button (this);
            // Set name of playlist to new button
            myButton.setText(currentplaylist.name);
            // Make button size 300x300
            myButton.setLayoutParams(new LinearLayout.LayoutParams(300, 300));

            // When a playlist button is clicked, show all songs within playlist
            myButton.setOnClickListener(new View.OnClickListener() {
                @SuppressLint("StaticFieldLeak")
                @Override
                public void onClick(View v) {
                    // Remove all views previously there
                    songs_view.removeAllViews();

                    // Start a new Asynctask because we need to find playlist online
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            // Get the playlist (Async required for this)
                            Playlist quickplaylist = spotifyWebService.getPlaylist(currentplaylist.href, currentplaylist.id);
                            // Get the songs within the playlist
                            List<PlaylistTrack> actualplaylist = quickplaylist.tracks.items;
                            // Go through every song
                            for (int j = 0; j < actualplaylist.size() - 1; j++) {
                                // Get a track from the results
                                final Track trackcanplay = actualplaylist.get(j).track;
                                // New button for that track
                                final Button myButton = new Button(LibraryActivity.this);
                                // Make button size 1280x110
                                myButton.setLayoutParams(new LinearLayout.LayoutParams(1280, 110));
                                // Set background of button
                                myButton.setBackgroundResource(R.drawable.buttonshape);
                                // Set text of button to name and artist of song
                                myButton.setText((trackcanplay.name + " By " + trackcanplay.artists.get(0).name));
                                // If the button is clicked on, play the song that was chosen
                                myButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        // Play song
                                        remoteService.play(trackcanplay.uri);
                                    }
                                });
                                // Must create a new UI thread runnable because
                                // we are inside the async task
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        // Add the button to the linear layout of the scrollview
                                        songs_view.addView(myButton);
                                    }
                                });
                            }
                            return null;
                        }
                    }.execute();
                }
            });

            // Add button to view
            myPlaylistsView.addView(myButton);

        }
    }

}
