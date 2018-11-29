package edu.stlawu.finalproject;

import com.spotify.protocol.*;
import com.spotify.protocol.types.Uri;

public class SpotifySongSimilarity {
    // The URI of the song we are currently comparing to other
    private Uri uri;

    SpotifySongSimilarity(Uri song){
        this.uri = song;
    }


}
