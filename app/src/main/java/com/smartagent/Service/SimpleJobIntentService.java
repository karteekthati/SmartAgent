package com.smartagent.Service;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.JobIntentService;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.smartagent.Bean.RealmObj;
import com.smartagent.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

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
        int duration = 50;
        int ctr = 1;
      if(index == 1){
          while(ctr <= duration){
             Log.d(TAG, "Time elapsed : "+ ctr*10 + " secs");
              try{
                  Thread.sleep(10000);
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

        try{
            Realm realm = Realm.getDefaultInstance();
            RealmResults<RealmObj> offlineobj = realm.where(RealmObj.class).findAll();
            //if data available display data
            if((offlineobj.size() > 0)  && (checkImageSize()== true) && (checkVideoSize() ==  true)){
                //data available
                Log.d(TAG, "available offline data");
            }
            else{
                //else check internet connection and download data
                if(isNetworkAvailable(getApplicationContext())){

                    String DATA_URL = getResources().getString(R.string.data_url);

                    RealmResults<RealmObj> offlineobj2 = realm.where(RealmObj.class).findAll();
                    if(offlineobj2.size() > 0){

                        Log.d(TAG, "Offline data available");

                        //checking image
                        if(checkImageSize() == false){

                            for(int index = 0; index < offlineobj2.size(); index++){
                                RealmObj fetch = offlineobj.get(index);
                                final String path = fetch.getKey("path");
                                final String type = fetch.getKey("type");

                                if(type.equalsIgnoreCase("IMAGE")){
                                    ImageDownload(1, path);
                                }

                            }
                        }
                        else{
                            Log.d(TAG, "image available");
                        }

                        if(checkVideoSize() == false){

                            for(int index = 0; index < offlineobj2.size(); index++){
                                RealmObj fetch = offlineobj.get(index);
                                final String path = fetch.getKey("path");
                                final String type = fetch.getKey("type");

                                if(type.equalsIgnoreCase("VIDEO")){
                                    //  GetVideo(1, path);

                                    new GetVideo().execute(path);
                                }

                            }
                        }
                        else{
                            Log.d(TAG, "video available");
                        }

                    }
                    else{

                        VolleyGetData(1, DATA_URL);
                    }
                }
                else{
                    Log.d(TAG, "no internet");

                }
            }
            realm.close();


        }
        catch (Exception e){
            Log.d(TAG, "exception :"+e.toString());

        }

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



    private boolean checkImageSize() {
        try {

            String root = Environment.getExternalStorageDirectory().getAbsolutePath();
            File newFile = new File(root + "/SmartAgent/test.jpg");
            long length = newFile.length();
            length = length/1024;
            Log.d(TAG, "File Path : " + newFile.getPath() + ", File size : " + length +" KB");

            Log.d(TAG, "image size : "+ String.valueOf(length));
            if(length >= 70){
                return true;
            }

        }catch (Exception e){
            Log.d(TAG, "image size exception : "+ e.toString());
        }
        return false;
    }

    private boolean checkVideoSize() {
        try{
            String root = Environment.getExternalStorageDirectory().getAbsolutePath();
            File newFile = new File(root + "/SmartAgent/video.mp4");
            long length = newFile.length();
            length = length/1024;
            Log.d(TAG, "File Path : " + newFile.getPath() + ", File size : " + length +" KB");

            Log.d(TAG, "video size : "+ String.valueOf(length));
            if(length >= 7000){
                return  true;
            }

        }catch(Exception e){
            Log.d(TAG,"File not found : " + e.getMessage() + e);
        }
        return false;
    }



    private void ImageDownload(int i, String path) {

// Retrieves an image specified by the URL, displays it in the UI.
        ImageRequest request = new ImageRequest(path,
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap bitmap) {
                        SaveImage(bitmap);
                        Log.d(TAG, "processing image save");
                    }
                }, 0, 0, null,
                new Response.ErrorListener() {
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "Error image download:"+ error.toString());
                    }
                });
        int socketTimeout = 100000;//1 min - change to what you want
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        request.setRetryPolicy(policy);
        request.setShouldCache(false);

        //Creating a request queue
        RequestQueue requestQueue = Volley.newRequestQueue(con, new HurlStack());
        requestQueue.getCache().clear();
        //Adding our request to the queue
        requestQueue.add(request);
    }

    private static void SaveImage(Bitmap finalBitmap) {

        File myDir = new File(Environment.getExternalStorageDirectory()
                + "/SmartAgent/");

        if (!myDir.exists()) {
            if (myDir.mkdirs()) {
                Log.d("TAG", "Successfully created the parent dir:" + myDir.getName());
            } else {
                Log.d("TAG", "Failed to create the parent dir:" + myDir.getName());
            }
        }
        String fname =  "test" +".jpg";

        File file = new File (myDir, fname);
        if (file.exists ()) file.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();

            Log.d("TAG", "Image download success");

        } catch (Exception e) {
            e.printStackTrace();
            Log.d("TAG", "image storing error :"+ e.toString());
        }
    }



    public void VolleyGetData(final int index, String DATA_URL) {

        Log.e(TAG,DATA_URL);

        final ProgressDialog loading = ProgressDialog.show(con, "Please wait...","Downloading data..",false,false);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, DATA_URL, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if(loading.isShowing()){
                    loading.dismiss();
                }

                Log.e(TAG,"Response : "+response.toString());

                try {
                    if(index == 1){
                        JSONArray jsonArray = response.getJSONArray("dependencies");
                        if(jsonArray.length() > 0){

                            Realm realm = Realm.getDefaultInstance();


                            realm.executeTransaction(new Realm.Transaction() {
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

//
//                                   Bean1 datum = new Bean1(id,name, type, size, path );
//                                   datumArrayList.add(datum);

                                realm.executeTransaction(new Realm.Transaction() {
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


                            CheckDataAvailability();

//                               smartAdapter = new SmartAdapter(datumArrayList, con);
//                               recyclerView.setLayoutManager(new LinearLayoutManager(con));
//                               recyclerView.setAdapter(smartAdapter);
                        }
                        else{
                            Log.d(TAG, "no data");

                        }


                        realm.close();
                    }
                    else{
                        Log.d(TAG, "image response"+ response.toString());
                    }

                } catch (Exception e) {
                    e.printStackTrace();

                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(loading.isShowing()){
                    loading.dismiss();
                }
                Log.e(TAG," error"+error.getMessage());

            }
        });

        int socketTimeout = 100000;//1 min - change to what you want
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        jsonObjectRequest.setRetryPolicy(policy);
        jsonObjectRequest.setShouldCache(false);

        //Creating a request queue
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.getCache().clear();
        //Adding our request to the queue
        requestQueue.add(jsonObjectRequest);

    }


    class GetVideo extends AsyncTask<String, Void, Boolean> {

        private Exception exception;

        boolean ret = false;

        BufferedInputStream bis = null;
        FileOutputStream fos = null;
        InputStream is = null;


        @Override
        protected Boolean doInBackground(String... strings) {
            File myDir = new File(Environment.getExternalStorageDirectory()
                    + "/SmartAgent/");

            if (!myDir.exists()) {
                if (myDir.mkdirs()) {
                    Log.d(TAG, "Successfully created the parent dir:" + myDir.getName());
                } else {
                    Log.d(TAG, "Failed to create the parent dir:" + myDir.getName());
                }
            }

            String fname =  "video" +".mp4";
            File file = new File (myDir, fname);
            if (file.exists ()) file.delete ();


            try {

                URL url = new URL(strings[0]);

                URLConnection connection = url.openConnection();

                is = connection.getInputStream();
                bis = new BufferedInputStream(is);

                ByteArrayOutputStream baf = new ByteArrayOutputStream();
                //We create an array of bytes
                byte[] data = new byte[50];
                int current = 0;

                while((current = bis.read(data,0,data.length)) != -1){
                    baf.write(data,0,current);
                }

                fos = new FileOutputStream(file);
                fos.write(baf.toByteArray());
                fos.close();
                ret = true;
            }
            catch(Exception e) {
                Log.e("TAG", "Error while downloading and saving file !", e);
            }
            finally {
                try {
                    if ( fos != null ) fos.close();
                } catch( IOException e ) {}
                try {
                    if ( bis != null ) bis.close();
                } catch( IOException e ) {}
                try {
                    if ( is != null ) is.close();
                } catch( IOException e ) {
                    Log.d(TAG, "exception :"+ e.toString());
                }
            }

            return ret;
        }

        protected void onPostExecute(Boolean feed) {
            // TODO: check this.exception
            // TODO: do something with the feed

            if(feed ==  true){
                Log.d(TAG, "sync video downloaded success: "+ String.valueOf(feed));
            }
        }

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
        }, 3000);
    }

}