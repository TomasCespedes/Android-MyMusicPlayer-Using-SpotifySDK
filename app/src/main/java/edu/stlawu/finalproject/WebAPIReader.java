package edu.stlawu.finalproject;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class WebAPIReader implements Runnable{
    private String url;
    private String token;

    public WebAPIReader(String url, String token){
        this.url = url;
        this.token = token;
    }

    @Override
    public void run() {
        try {

            URL url = new URL(this.url);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", token);

            if (conn.getResponseCode() != 200) {
                Log.e("ERRORCODE", conn.getResponseMessage());
                Log.e("ERRORCODE", Integer.toString(conn.getResponseCode()));
                Log.e("ERRORCODE", conn.getRequestMethod());
                //throw new RuntimeException("Failed : HTTP error code : "
                        //+ conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));

            String output;
            Log.i("READER","Output from Server .... \n");
            while ((output = br.readLine()) != null) {
                Log.i("READER", output);
            }

            conn.disconnect();

        } catch (MalformedURLException e) {

            e.printStackTrace();

        } catch (IOException e) {

            e.printStackTrace();

        }
    }
}
