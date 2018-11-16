package edu.stlawu.finalproject;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.spotify.android.appremote.api.SpotifyAppRemote;

public class LibraryActivity extends AppCompatActivity {


    // TextViews
    private TextView currentsong;

    // Buttons
    private ImageButton playpausebutton;
    private Button homebutton, searchbutton, librarybutton, playingbutton;

    // Tracker for song playing status (play or pause)
    private String currenttracker = "play";

    // ImageViews
    private ImageView song_iv;

    private SpotifyAppRemote mSpotifyAppRemote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Find views
        currentsong = findViewById(R.id.current_song);
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
                Intent myIntent = new Intent(LibraryActivity.this, MainActivity.class);
                LibraryActivity.this.startActivity(myIntent);
            }
        });

        // Search button to create a new activity
        searchbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(LibraryActivity.this, SearchActivity.class);
                LibraryActivity.this.startActivity(myIntent);
            }
        });

        // Libray button to create a new activity
        librarybutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(LibraryActivity.this, LibraryActivity.class);
                LibraryActivity.this.startActivity(myIntent);
            }
        });

        // Playing Button to create a new activity
        playingbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(LibraryActivity.this, PlayingActivity.class);
                LibraryActivity.this.startActivity(myIntent);
            }
        });

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
