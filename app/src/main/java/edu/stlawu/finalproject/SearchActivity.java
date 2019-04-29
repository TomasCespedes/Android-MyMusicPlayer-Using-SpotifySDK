package edu.stlawu.finalproject;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
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
import kaaes.spotify.webapi.android.models.SavedTrack;
import kaaes.spotify.webapi.android.models.Track;
import retrofit.client.Response;

public class SearchActivity extends AppCompatActivity {

    // Client ID and Redirect URI gotten from Spotify
    private static final String CLIENT_ID = "377538ebcf9e4cdb9c4b5373e62a53a3";
    private static final String REDIRECT_URI = "FinalProjectCS450://callback";
    // Request code will be used to verify if result comes from the login activity. Can be set to any integer.
    private static final int REQUEST_CODE = 1337;

    // Search song variables
    public String songtosearch;
    public List<Track> songquery;
    public LinearLayout searchedsongs_view;

    // TextViews
    private TextView currentsong;
    // Buttons
    private ImageButton playpausebutton;
    private Button homebutton, searchbutton, librarybutton, playingbutton;

    // GestorDetector for next/previous song
    private GestureDetector nextPrevSong;

    // Web API connection
    private SpotifyApi api = new SpotifyApi();
    private SpotifyService spotifyWebService;
    private String accessToken;

    // Read user input
    Button songsearch_btn;
    EditText mEdit;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

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
        });

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
        currentsong.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                nextPrevSong.onTouchEvent(event);
                return true;
            }
        });
    }

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

        // Button to submit a song search
        songsearch_btn.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("StaticFieldLeak")
            public void onClick(View view) {
                try {
                    InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                } catch (Exception e) {
                    // TODO: handle exception
                }
                // Get the text that the user submitted
                songtosearch = mEdit.getText().toString();
                // Remove all previous views from possible previous searches
                searchedsongs_view.removeAllViews();

                // If the input is not empty
                if (songtosearch != null) {
                    // Only do this if the service is already connected
                    if (remoteService.connected) {
                        // Create a new AsyncTask to handle network request
                        new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected Void doInBackground(Void... voids) {
                                // Get all the results of the search
                                try {
                                    songquery = spotifyWebService.searchTracks(songtosearch).tracks.items;
                                    // Song search was successful (Some results were found)
                                    if (songquery.size() > 0) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                // Show a message saying results are being searched for
                                                Toast toast=Toast.makeText(getApplicationContext(),"Searching for results.",Toast.LENGTH_SHORT);
                                                // Center toast
                                                toast.setGravity(Gravity.TOP|Gravity.LEFT,600,200);
                                                toast.show();
                                            }
                                        });

                                    }
                                    // Song search was not successful (No results were found)
                                    else {
                                        // Must run toasts on a Main UI thread
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                // Show a message saying results not found
                                                Toast toast = Toast.makeText(getApplicationContext(),"No songs found :(",Toast.LENGTH_SHORT);
                                                // Center toast
                                                toast.setGravity(Gravity.TOP|Gravity.LEFT,600,200);
                                                toast.show();
                                            }
                                        });

                                    }
                                } catch (RuntimeException asnyctask) {
                                    Log.d("Caught", "Exception caught on song query");
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            // Search was empty, send a message saying to input something valid
                                            Toast toast=Toast.makeText(getApplicationContext(),"Please input text.",Toast.LENGTH_SHORT);
                                            // Center toast
                                            toast.setGravity(Gravity.TOP|Gravity.LEFT,600,200);
                                            toast.show();
                                        }
                                    });
                                }

                                return null;
                            }
                        }.execute();

                        // Need to delay this because code above needs to get information
                        // From the internet so it takes time
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                // If the song search did not come back as empty
                                if (songquery != null) {
                                    // Loop through all the results
                                    for (int i = 0; i < songquery.size() - 1; i++) {
                                        // Get a track from the results
                                        final Track trackcanplay = songquery.get(i);
                                        // New button for that track
                                        Button myButton = new Button(SearchActivity.this);
                                        // Make button size 300x40
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
                                        searchedsongs_view.addView(myButton);
                                    }
                                }
                            }
                        }, 1000);
                    }
                }

                // Reset all fields for next search
                mEdit.setText("");
                songtosearch = null;
                songquery = null;
            }
        });


    }


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

    @Override
    protected void onStop() {
        super.onStop();
        // Disconnect from app
        remoteService.disconnect();
    }


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

                    // Do what we need here


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

    public void init() {
        // Find all the views for current song, play/pause button,
        // and the song image view
        currentsong = findViewById(R.id.current_songsearch);
        currentsong.setSelected(true);
        playpausebutton = findViewById(R.id.current_button);

        // Text edit input
        mEdit = findViewById(R.id.editText1);
        mEdit.setCursorVisible(false);

        // Vertical scroll view for all the searched songs
        searchedsongs_view = findViewById(R.id.scrollview_songs);

        // Button view for submitting a search
        songsearch_btn = findViewById(R.id.songsearch_button);


        // Find all the buttons for the bottom menu
        homebutton = findViewById(R.id.homebtn);
        searchbutton = findViewById(R.id.searchbtn);
        librarybutton = findViewById(R.id.librarybtn);
        playingbutton = findViewById(R.id.playingbtn);


        // Home button to create a new activity
        homebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(SearchActivity.this, MainActivity.class);
                SearchActivity.this.startActivity(myIntent);
                finish();
            }
        });

        // Search button to create a new activity
        searchbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Do nothing
            }
        });

        // Libray button to create a new activity
        librarybutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(SearchActivity.this, LibraryActivity.class);
                SearchActivity.this.startActivity(myIntent);
                finish();
            }
        });

        // Playing Button to create a new activity
        playingbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(SearchActivity.this, PlayingActivity.class);
                SearchActivity.this.startActivity(myIntent);
                finish();
            }
        });
    }


}
