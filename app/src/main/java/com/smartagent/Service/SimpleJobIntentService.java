package com.smartagent.Service;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.JobIntentService;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.smartagent.Bean.RealmObj;
import com.smartagent.MainActivity;
import com.smartagent.R;

import org.json.JSONArray;
import org.json.JSONObject;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

import static com.android.volley.DefaultRetryPolicy.DEFAULT_MAX_RETRIES;

public class SimpleJobIntentService extends JobIntentService {
    /**
     * Unique job ID for this service.
     */
    static final int JOB_ID = 1000;
    private static final  String TAG = "TAG";
    Realm realm;
    Context con;

    /**
     * Convenience method for enqueuing work in to this service.
     */
    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, SimpleJobIntentService.class, JOB_ID, work);

    }

    @Override
    public void onCreate(){
        super.onCreate();
        long unixTime = System.currentTimeMillis() / 1000L;
       // Toast.makeText(this, "task Execution Started", Toast.LENGTH_SHORT).show();
       Log.d(TAG, "Job service started : "+ String.valueOf(unixTime));
        con = SimpleJobIntentService.this;

        Realm.init(this);

        try{
            realm = Realm.getDefaultInstance();

        }catch (Exception e){

            // Get a Realm instance for this thread
            RealmConfiguration config = new RealmConfiguration.Builder()
                    .deleteRealmIfMigrationNeeded()
                    .build();
            realm = Realm.getInstance(config);

        }
    }

    @Override
    protected void onHandleWork(Intent intent) {
        // We have received work to do.  The system or framework is already
        // holding a wake lock for us at this point, so we can just go.
        Log.i(TAG, "onHandleWork, Thread name: " + Thread.currentThread().getName());
        int index = intent.getIntExtra("startwork", 0);
        //counter for 30 sec to check data
        int duration = 30;
        int ctr = 1;
      if(index == 1){
          while(ctr <= duration){
              Log.d(TAG, "Time elapsed : "+ ctr + "secs");
              try{
                  Thread.sleep(1000);
              }
              catch (Exception e){
                  Log.d(TAG, "exception sync at count : "+e.toString());
              }
              ctr++;
          }
          CheckDataAvailability();
      }

    }

    private void CheckDataAvailability() {

        Realm backgroundRealm = Realm.getDefaultInstance();

//        try{
            RealmResults<RealmObj> offlineobj = backgroundRealm.where(RealmObj.class).findAll();
            //if data available display data
            if(offlineobj.size() > 0){
                //data available so add to adapter
                Log.d(TAG, "sync offline data available");
            }
            else{
                //else check internet connection and download data
                if(isNetworkAvailable(getApplicationContext())){
                    VolleyGetData();
                }
                else{
                    Log.d(TAG ," Error in sync"+ "no internet");

                }
            }

//        }
//        catch (Exception e){
//            Log.d(TAG ," Exception"+ e.toString());
//
//        }

    }


    public static boolean isNetworkAvailable(Context ctx) {
        ConnectivityManager conMgr = (ConnectivityManager) ctx
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = conMgr.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting())
            return true;
        else
            return false;
    }

    private void VolleyGetData() {
        String DATA_URL = getResources().getString(R.string.data_url);
        Log.e(TAG,DATA_URL);


        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, DATA_URL, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                Log.e(TAG,"Response in sync : "+response.toString());

                try {
                    JSONArray jsonArray = response.getJSONArray("dependencies");
                    if(jsonArray.length() > 0){

                        Realm backgroundRealm = Realm.getDefaultInstance();

                        backgroundRealm.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                RealmResults<RealmObj> results = realm.where(RealmObj.class).findAll();
                                if (results.size() > 0) {
                                    results.deleteAllFromRealm();
                                }
                            }
                        });

                        for(int i = 0; i < jsonArray.length(); i++){
                            JSONObject innerjson = jsonArray.getJSONObject(i);
                            final String id = innerjson.getString("id");
                            final String name = innerjson.getString("name");
                            final String type = innerjson.getString("type");
                            final String size = innerjson.getString("sizeInBytes");
                            final String path = innerjson.getString("cdn_path");

                            backgroundRealm.executeTransaction(new Realm.Transaction() {
                                @Override
                                public void execute(Realm realm) {

                                    RealmObj realmObj = realm.createObject(RealmObj.class);

                                    realmObj.setKey("id", String.valueOf(id));
                                    realmObj.setKey("name", String.valueOf(name));
                                    realmObj.setKey("type",String.valueOf(type) );
                                    realmObj.setKey("size", String.valueOf(size));
                                    realmObj.setKey("path", String.valueOf(path));
                                }
                            });
                        }
                    }
                    else{
                        Log.d(TAG, "error occured sync");
                    }

                } catch (Exception e) {
                    Log.d(TAG, "catch in sync : "+ e.toString());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG," error in sync"+error.getMessage());
            }
        });
        int socketTimeout = 100000;//1 min - change to what you want
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        jsonObjectRequest.setRetryPolicy(policy);
        jsonObjectRequest.setShouldCache(false);

        //Creating a request queue
        RequestQueue requestQueue = Volley.newRequestQueue(con);
        requestQueue.getCache().clear();
        //Adding our request to the queue
        requestQueue.add(jsonObjectRequest);

    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        long unixTime = System.currentTimeMillis() / 1000L;
        Log.d(TAG, "Task COmpleted : "+String.valueOf(unixTime));
         WaitandBroadCast();
    }

    private void WaitandBroadCast() {
        //handler to restart the job after destroy in 30 sec

        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(con, SimpleJobIntentService.class);
                i.putExtra("startwork", 1);
                SimpleJobIntentService.enqueueWork(con, i);
            }
        }, 30000);
    }

}