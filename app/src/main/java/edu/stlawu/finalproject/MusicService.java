package edu.stlawu.finalproject;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.IBinder;

import java.io.File;
import java.io.IOException;

/**
 * Created by ehar on 11/17/2016.
 *
 * A music service to play a song in the background
 *
 */
public class MusicService extends Service
        implements MediaPlayer.OnPreparedListener {

    MediaPlayer player = null;

    // We only want one instance of this class so
    // make it a Singleton (https://en.wikipedia.org/wiki/Singleton_pattern)
    private static MusicService instance = null;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    // restart the song from the beginning
    public void restart() {
        player.seekTo(0);
    }

    // Return the instance of the singleton class.
    public static MusicService getInstance() {
        return instance;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int i = super.onStartCommand(intent, flags, startId);

        player = new MediaPlayer();

//        // Get the path to the music directory
//        File path = Environment.getExternalStoragePublicDirectory(
//                Environment.DIRECTORY_MUSIC
//        );
//
//        // Set the media play song path to where the
//        // mps file is on external storage
//        try {
//            player.setDataSource(path.getPath() + "/" + "my_old_friend.mp3");
//        } catch (IOException e) {
//            e.printStackTrace();
//            return i;
//        }


        // This is so that the song does not play
        // in the same thread as the UI thread.
        player.setOnPreparedListener(this);
        player.prepareAsync();

        return i;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        player.stop();
        player.release();
        instance = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mediaPlayer.start();
    }
}
