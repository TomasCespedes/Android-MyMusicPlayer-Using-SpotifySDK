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
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
import kaaes.spotify.webapi.android.models.SavedTrack;
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
    public SpotifyAppRemote mSpotifyAppRemote;
    private Track track;

    // TextViews
    private TextView currentsong;

    // Buttons
    private ImageButton playpausebutton;
    private Button homebutton, searchbutton, librarybutton, playingbutton;

    // Tracker for song playing status (play or pause)
    private String currenttracker = "play";

    // ImageViews
    private ImageView song_iv;

    private SpotifyApi api = new SpotifyApi();
    private SpotifyService spotifyService;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }


    @Override
    protected void onStart() {
        super.onStart();

        // Call the method to get the token
        getToken();

        // Connect to the remote
        connectToRemote();

    }

    private void getToken() {
        AuthenticationRequest.Builder builder =
                new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI);

        builder.setScopes(new String[]{"streaming"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    // Receive authentication result which contains the token
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
                    api.setAccessToken(response.getAccessToken());
                    spotifyService = api.getService();
                    Log.e("Tokens" , response.getAccessToken());

                    spotifyService.getMySavedTracks(new SpotifyCallback<Pager<SavedTrack>>() {
                        @Override
                        public void failure(SpotifyError spotifyError) {
                            Log.e("SpotifyErrors", spotifyError.toString());
                        }

                        @Override
                        public void success(Pager<SavedTrack> savedTrackPager, Response response) {
                            Log.e("Savedsongs", response.getReason());
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


    /**
     * Connect to the remote so user can
     * interact with songs
     */
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


    /**
     * Our app is connected to spotify and streaming
     * can now happen.
     */
    private void connected() {
        // Play a playlist
        mSpotifyAppRemote.getPlayerApi().play("spotify:track:0VgkVdmE4gld66l8iyGjgx");


        // Call the subscribe to Playerstate Method to get
        // current Playerstate
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


    /**
     *Subscribe to PlayerState and get current playing information
     * Uses the Spotify Remote
     */
    private void subscribetoPlayerState() {
        // Subscribe to PlayerState

        mSpotifyAppRemote.getPlayerApi()
                .subscribeToPlayerState()
                .setEventCallback(new Subscription.EventCallback<PlayerState>() {


                    // If a song is playing get the track name and artist name
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

    /**
     * Initialize all views and fields
     */
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
                Intent myIntent = new Intent(MainActivity.this, MainActivity.class);
                MainActivity.this.startActivity(myIntent);
            }
        });

        /**
         * Search button to create a new activity
         */
        searchbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(MainActivity.this, SearchActivity.class);
                MainActivity.this.startActivity(myIntent);
            }
        });

        /**
         * Libray button to create a new activity
         */
        librarybutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(MainActivity.this, LibraryActivity.class);
                MainActivity.this.startActivity(myIntent);
            }
        });

        /**
         * Playing Button to create a new activity
         */
        playingbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(MainActivity.this, PlayingActivity.class);
                MainActivity.this.startActivity(myIntent);
            }
        });


    }


}

