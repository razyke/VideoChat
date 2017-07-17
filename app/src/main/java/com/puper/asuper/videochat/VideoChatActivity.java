package com.puper.asuper.videochat;


import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import com.opentok.android.*;
import android.Manifest;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;
import android.widget.FrameLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Daniil Smirnov on 13.07.2017.
 * All copy registered MF.
 */
public class VideoChatActivity extends FragmentActivity implements Session.SessionListener,PublisherKit.PublisherListener{

    private static String API_KEY;
    private static String SESSION_ID;
    private static String TOKEN;
    private static final String LOG_TAG = VideoChatActivity.class.getSimpleName();
    private static final int RC_VIDEO_APP_PERM = 124;
    private final int MAX_NUM_SUBSCRIBERS = 1;
    private final String CREATE_NEW_SESSION = "https://videochat778.herokuapp.com/newsession";
    private final String CONNECT_TO_SESSION = "https://videochat778.herokuapp.com/session";

    private ArrayList<Subscriber> mSubscribers = new ArrayList<>();
    private Session mSession;
    private FrameLayout mPublisherViewContainer;
    private FrameLayout mSubscriberViewContainer;
    private Publisher mPublisher;
    private Subscriber mSubscriber;
    private FragmentManager fm;
    private WaitingFragment wf;
    private boolean active = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        fm = getSupportFragmentManager();
        setContentView(R.layout.videochat_layout);
        mPublisherViewContainer = (FrameLayout)findViewById(R.id.publisher_container);
        mSubscriberViewContainer = (FrameLayout)findViewById(R.id.subscriber_container);

    }

    //Check if there is network connection fine

    @Override
    protected void onResume() {
        super.onResume();
        if (!isOnline()){
            AlertDialog ad = new AlertDialog.Builder(this)
                    .setMessage("Network bad")
                    .setCancelable(false)
                    .setPositiveButton("Try again?", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (isOnline()){
                                if (!active)
                                requestPermissions();
                            }else {
                                Toast.makeText(getApplicationContext(),"Check your internet connection!",Toast.LENGTH_SHORT).show();
                                onResume();
                            }
                        }
                    })
                    .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            android.os.Process.killProcess(android.os.Process.myPid());
                        }
                    })
                    .create();
            setColorForAlert(ad);
            ad.show();
            if (mSession != null) {
                mSession.onResume();
            }
        }else
            if (!active)
            requestPermissions();
    }


    //Connect to our HEROKU server, get from there API, Session and token

    public void fetchSessionConnectionData(String url) {

        RequestQueue reqQueue = Volley.newRequestQueue(this);
        reqQueue.add(new JsonObjectRequest(Request.Method.GET,
                url,
                null, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                try {
                    API_KEY = response.getString("apiKey");
                    SESSION_ID = response.getString("sessionId");
                    TOKEN = response.getString("token");

                    Log.i(LOG_TAG, "API_KEY: " + API_KEY);
                    Log.i(LOG_TAG, "SESSION_ID: " + SESSION_ID);
                    Log.i(LOG_TAG, "TOKEN: " + TOKEN);

                    setWaiting();

                    mSession = new Session.Builder(VideoChatActivity.this, API_KEY, SESSION_ID).build();
                    mSession.setSessionListener(VideoChatActivity.this);
                    mSession.connect(TOKEN);

                } catch (JSONException error) {
                    Log.e(LOG_TAG, "Web Service error: " + error.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "Web Service error: " + error.getMessage());
            }
        }));
    }


    //Use for permissions EasyPermissions

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults,this);
    }


    //If we received permission we connect to session

    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private void requestPermissions(){
        active=true;

        String[] perms = {Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO};
        if (EasyPermissions.hasPermissions(this,perms)){

            fetchSessionConnectionData(CONNECT_TO_SESSION);

        } else {
            EasyPermissions.requestPermissions(this,"This app need access to your camera and mic to make video calls",RC_VIDEO_APP_PERM,perms);
        }
    }


                                    //SessionListener

    //When we connected to session, we add our publish stream

    @Override
    public void onConnected(Session session) {
        Log.i(LOG_TAG, "Session Connected");

        mPublisher = new Publisher.Builder(this).build();
        mPublisher.setPublisherListener(this);

        mPublisherViewContainer.removeAllViews();
        mPublisherViewContainer.addView(mPublisher.getView());
        mSession.publish(mPublisher);
    }

    //When we have disconnected from session

    @Override
    public void onDisconnected(Session session) {
        Log.i(LOG_TAG, "Session Disconnected");
        mSession = null;
    }

    //When we get stream, we check it if there 2 subscribers, if true create new session and connect

    @Override
    public void onStreamReceived(Session session, Stream stream) {
        Log.i(LOG_TAG, "Stream Received");

        if (mSubscribers.size() + 1 > MAX_NUM_SUBSCRIBERS) {
            Log.i(LOG_TAG, "There are 2 people...next! " + mSubscribers.size());
            mSubscriberViewContainer.removeAllViews();
            mSubscribers = new ArrayList<>();
            mSession.disconnect();
            fetchSessionConnectionData(CREATE_NEW_SESSION);
            return;
        }

                mSubscriberViewContainer.removeAllViews();
                mSubscriber=null;
                mSubscriber = new Subscriber.Builder(this, stream).build();
                mSession.subscribe(mSubscriber);
                mSubscribers.add(mSubscriber);
                mSubscriberViewContainer.addView(mSubscriber.getView());
    }

    //When subscriber left our chat we disconnect and connect again to a new session

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        Log.i(LOG_TAG, "Stream Dropped");

        if (mSubscriber != null) {
            mSubscriber = null;
            mSubscribers = new ArrayList<>();
            mSubscriberViewContainer.removeAllViews();
            mSession.disconnect();
            fetchSessionConnectionData(CONNECT_TO_SESSION);

        }

    }

    //If an error has occurred we check network connection and if fine connect again

    @Override
    public void onError(Session session, OpentokError opentokError) {
        Log.e(LOG_TAG, "Session error: " + opentokError.getMessage());
        active=false;
        onResume();
    }



                            //PublisherListener

    //We have created stream

    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {
        Log.i(LOG_TAG, "Publisher onStreamCreated");
    }

    //Stream destroyed, just log it

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {
        Log.i(LOG_TAG, "Publisher onStreamDestroyed");
    }

    //If an error has occurred we check network connection and if fine connect again

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {
        Log.e(LOG_TAG, "Publisher error: " + opentokError.getMessage());
        active=false;
        onResume();
    }


    //Set action on back device button

    @Override
    public void onBackPressed() {
        AlertDialog ad = new AlertDialog.Builder(this)
                .setMessage("Exit from app ?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        android.os.Process.killProcess(android.os.Process.myPid());
                    }
                })
                .setNegativeButton("No",null)
                .create();
        setColorForAlert(ad);
        ad.show();
    }

    //Set waiting frame

    private void setWaiting(){
            wf = new WaitingFragment();
            fm.beginTransaction()
                    .add(R.id.subscriber_container, wf)
                    .commit();
    }

    //Checking Internet connection

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(getApplicationContext().CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    //Add color for button to AlertDialog

    public void setColorForAlert(AlertDialog ad){
        ad.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button positiveButton = ((AlertDialog)dialog).getButton(DialogInterface.BUTTON_POSITIVE);
                positiveButton.setTextColor(getApplicationContext().getResources().getColor(R.color.colorPrimaryDark));
                positiveButton.invalidate();

                Button negativeButton = ((AlertDialog)dialog).getButton(DialogInterface.BUTTON_NEGATIVE);
                negativeButton.setTextColor(getApplicationContext().getResources().getColor(R.color.colorPrimaryDark));
                negativeButton.invalidate();
            }
        });
    }
}
