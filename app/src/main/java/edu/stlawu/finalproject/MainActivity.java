package edu.stlawu.finalproject;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
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

    // Client ID and Redirect URI gotten from Spotify
    private static final String CLIENT_ID = "377538ebcf9e4cdb9c4b5373e62a53a3";
    private static final String REDIRECT_URI = "FinalProjectCS450://callback";

    // Request code will be used to verify if result comes from the login activity. Can be set to any integer.
    private static final int REQUEST_CODE = 1337;

    // App remote for Spotify to control song playback
    public static Track track;
    private String accessToken;

    // TextViews
    public static TextView currentsong;

    // Buttons
    private ImageButton playpausebutton;
    private Button homebutton, searchbutton, librarybutton, playingbutton;

    // ImageViews
    private ImageView song_iv;

    private SpotifyApi api = new SpotifyApi();
    private SpotifyService spotifyService;

    // ServiceConnection
    RemoteService remoteService;
    boolean isBound = false;

    // Remote service connection set up
    public ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            RemoteService.MyBinder binder = (RemoteService.MyBinder) service;
            remoteService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            remoteService = null;
            isBound = false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Connect to the Remote Service
        Intent intent = new Intent(this, RemoteService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        // Initialize all the views and buttons
        init();
    }


    @Override
    protected void onStart() {
        super.onStart();

        // TODO Foreground service is apparently what we want here.
        // TODO http://codetheory.in/understanding-android-started-bound-services/

        // Call the method to get the token
        getToken();


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

    /**
     * Method to open the Login Activity and get the Token
     */
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

                    // Set the Access token so we can use API
                    api.setAccessToken(accessToken);
                    spotifyService = api.getService();

                    // Get "MY" playlists
                    spotifyService.getMyPlaylists(new SpotifyCallback<Pager<PlaylistSimple>>() {
                        // Request failed
                        @Override
                        public void failure(SpotifyError spotifyError) {
                            Log.e("playlists", spotifyError.getMessage());
                        }

                        // Request was Successful
                        @Override
                        public void success(Pager<PlaylistSimple> playlistSimplePager, Response response) {
                            // TODO Get Playlists and Show them on Main Page
                            Log.e("Playlists", response.getReason());
                            Log.e("Playlists", String.valueOf(playlistSimplePager.items.get(0).tracks));

                            Log.e("References", playlistSimplePager.items.get(0).snapshot_id);



                        }
                    });

                    // TODO For some reason mysavedtracks returns an error
                    // TODO Insufficent Client Scope 403 is error
//                    spotifyService.getMySavedTracks(new SpotifyCallback<Pager<SavedTrack>>() {
//                        @Override
//                        public void failure(SpotifyError spotifyError) {
//                            Log.e("SpotifyErrorz", spotifyError.toString());
//                        }
//
//                        @Override
//                        public void success(Pager<SavedTrack> savedTrackPager, Response response) {
//                            Log.e("Savedsongs", response.getReason());
//                        }
//                    });

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


//    /**
//     * Our app is connected to spotify and streaming
//     * can now happen.
//     */
//    private void connected() {
//        // Play a playlist
//        mSpotifyAppRemote.getPlayerApi().play("spotify:track:0VgkVdmE4gld66l8iyGjgx");
//
//
//        // Call the subscribe to Playerstate Method
//        subscribetoPlayerState();
//
//    }


    /**
     *Subscribe to PlayerState and get current playing information
     * Uses the Spotify Remote
     */
//    private void subscribetoPlayerState() {
//
//        // Subscribe to PlayerState
//
//        mSpotifyAppRemote.getPlayerApi()
//                .subscribeToPlayerState()
//                .setEventCallback(new Subscription.EventCallback<PlayerState>() {
//
//
//                    // If a song is playing get the track name and artist name
//                    public void onEvent(PlayerState playerState) {
//                        track = playerState.track;
//
//                        if (track != null) {
//                            /**
//                             * Set data to views (track name by track artist)
//                             */
//                            currentsong.setText((track.name + " by " + track.artist.name));
//                            /**
//                             * Get Image for the track
//                             */
//
//
//                            mSpotifyAppRemote.getImagesApi().getImage(track.imageUri)
//                                    .setResultCallback(new CallResult.ResultCallback<Bitmap>() {
//                                        @Override
//                                        public void onResult(Bitmap bitmap) {
//                                            Drawable d = new BitmapDrawable(getResources(), bitmap);
//                                            song_iv.setImageDrawable(d);
//                                        }
//                                    });
//                        }
//                    }
//                });
//   }

    /**
     * When app stops, disconnect app from Spotify
     */
    @Override
    protected void onStop() {
        super.onStop();

        // Disconnect from app
        remoteService.disconnect();
    }

    /**
     * Initialize all views and fields
     */
    private void init() {

        // Find all the views for current song, play/pause button,
        // and the song image view
        currentsong = findViewById(R.id.current_song);
        currentsong.setSelected(true);
        playpausebutton = findViewById(R.id.current_button);
        song_iv = findViewById(R.id.song_iv);


        // Get all the Button views for the navigation menu at bottom
        homebutton = findViewById(R.id.homebtn);
        searchbutton = findViewById(R.id.searchbtn);
        librarybutton = findViewById(R.id.librarybtn);
        playingbutton = findViewById(R.id.playingbtn);

        // Home button switch activity
        homebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(MainActivity.this, MainActivity.class);
                MainActivity.this.startActivity(myIntent);
            }
        });

        // Search button switch activity
        searchbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(MainActivity.this, SearchActivity.class);
                MainActivity.this.startActivity(myIntent);
            }
        });

        // Library button switch activity
        librarybutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(MainActivity.this, LibraryActivity.class);
                MainActivity.this.startActivity(myIntent);
            }
        });

        // Playing button switch activity
        playingbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(MainActivity.this, PlayingActivity.class);
                MainActivity.this.startActivity(myIntent);
            }
        }); 


    }

    public void getSongPlaying() {
        track = remoteService.getTrack();
    }
}

