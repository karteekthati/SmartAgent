package com.smartagent;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.smartagent.Adapter.SmartAdapter;
import com.smartagent.Bean.Bean1;

import com.smartagent.Bean.RealmObj;
import com.smartagent.Service.SimpleJobIntentService;

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
import java.util.ArrayList;

import id.zelory.compressor.Compressor;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import pub.devrel.easypermissions.EasyPermissions;

import static com.android.volley.DefaultRetryPolicy.DEFAULT_MAX_RETRIES;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    String TAG = "TAG";
    Realm realm;
    SmartAdapter smartAdapter;
    RecyclerView recyclerView;
    ArrayList<Bean1> datumArrayList;
    TextView TvInternetStatus;
    final String[] perms = {Manifest.permission.INTERNET,Manifest.permission.ACCESS_NETWORK_STATE,Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WAKE_LOCK, Manifest.permission.RECEIVE_BOOT_COMPLETED};




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        InitViews();
    }

    private void InitViews() {

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
        datumArrayList = new ArrayList<Bean1>();
        datumArrayList.clear();

        TvInternetStatus = (TextView)findViewById(R.id.TvInternetStatus);
        TvInternetStatus.setOnClickListener(this);

        recyclerView = (RecyclerView)findViewById(R.id.SmartRecyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.removeAllViews();


        if(isNetworkAvailable(getApplicationContext())){
            TvInternetStatus.setText("Internet Available");
            TvInternetStatus.setTextColor(getResources().getColor(R.color.dark_green));
        }
        else{
            TvInternetStatus.setText("Internet is not available");
            TvInternetStatus.setTextColor(getResources().getColor(R.color.colorAccent));
        }

        if (methodRequiresPermission(perms, 111)) {
            CheckDataAvailability();
            checkSync();

        } else {

            Toast.makeText(MainActivity.this, "Please Grant required app permissions!!", Toast.LENGTH_SHORT).show();
        }

    }


    private boolean methodRequiresPermission(String[] perms, int permission) {

        if (EasyPermissions.hasPermissions(MainActivity.this, perms)) {
            // Already have permission, do the thing
            // ...
            return true;
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, "Need these permissions for the smooth working of the APP, Requesting to allow",
                    permission, perms);
        }

        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);

        CheckDataAvailability();

        checkSync();
    }


    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            finish();
        }
        return false;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.TvInternetStatus:
                if(isNetworkAvailable(getApplicationContext())){
                    String DATA_URL = getResources().getString(R.string.data_url);
                    VolleyGetData(1, DATA_URL);
                }
                else{
                    Toast.makeText(this, "Data and internet is not available, so need to switch on internet to download data", Toast.LENGTH_SHORT).show();
                }
                break;
            default: break;
        }
    }

    private void checkSync(){

        try {
            Intent i = new Intent(MainActivity.this, SimpleJobIntentService.class);
            i.putExtra("startwork", 1);
            SimpleJobIntentService.enqueueWork(MainActivity.this, i);
        }
        catch (Exception ry)
        {
            Log.d(TAG, "error"+ry.toString());
        }
    }



    private void CheckDataAvailability() {

        try{
            RealmResults<RealmObj> offlineobj = realm.where(RealmObj.class).findAll();
            //if data available display data
            if((offlineobj.size() > 0)  && (checkImageSize()== true) && (checkVideoSize() ==  true)){
                //data available so add to adapter
                Log.d(TAG, "showing offline data");
                datumArrayList.clear();
                for(int ii= 0; ii < offlineobj.size(); ii++){
                    RealmObj fetch = offlineobj.get(ii);

                    final String id = fetch.getKey("id");
                    final String name = fetch.getKey("name");
                    final String type = fetch.getKey("type");
                    final String size = fetch.getKey("size");
                    final String path = fetch.getKey("path");

                    Bean1 datum = new Bean1(id,name, type, size, path );
                    datumArrayList.add(datum);
                }

                Toast.makeText(MainActivity.this, "Showing offline data", Toast.LENGTH_SHORT).show();

                smartAdapter = new SmartAdapter(datumArrayList, MainActivity.this);
                recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                recyclerView.setAdapter(smartAdapter);




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
                            Toast.makeText(this, "image is missing", Toast.LENGTH_SHORT).show();

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
                            Toast.makeText(this, "video is missing", Toast.LENGTH_SHORT).show();

                            for(int index = 0; index < offlineobj2.size(); index++){
                                RealmObj fetch = offlineobj.get(index);
                                final String path = fetch.getKey("path");
                                final String type = fetch.getKey("type");

                                if(type.equalsIgnoreCase("VIDEO")){
                                    new GetVideo().execute(path);
                                }

                            }
                        }
                        else{
                            Log.d(TAG, "video available");
                        }

                    }
                    else{
                        Toast.makeText(this, "Some data is missing", Toast.LENGTH_SHORT).show();
                        VolleyGetData(1, DATA_URL);
                    }
                }
                else{
                    Toast.makeText(this, "Data and internet is not available, so need to switch on internet to download data", Toast.LENGTH_SHORT).show();
                }
            }


        }
        catch (Exception e){
            Toast.makeText(this, "Error exception :"+e.toString(), Toast.LENGTH_SHORT).show();
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
//
//
//            String root = Environment.getExternalStorageDirectory().getAbsolutePath();
//            File newFile = new File(root + "/SmartAgent/test.jpg");
//            Bitmap bitmap = new Compressor(this).compressToBitmap(newFile);
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
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this, new HurlStack());
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

    class GetVideo extends AsyncTask<String, Void, Boolean> {

        private Exception exception;

        boolean ret = false;

        BufferedInputStream bis = null;
        FileOutputStream fos = null;
        InputStream is = null;


        @Override
        protected Boolean doInBackground(String... strings) {
            Log.d(TAG, "downloading image");
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
            Log.d(TAG, "Video downloaded success: "+ String.valueOf(feed));
            if(feed ==  true){
                Toast.makeText(MainActivity.this, "Video Successfully downloaded", Toast.LENGTH_SHORT).show();
            }
        }

    }



    public void VolleyGetData(final int index, String DATA_URL) {

        Log.e(TAG,DATA_URL);

        final ProgressDialog loading = ProgressDialog.show(MainActivity.this, "Please wait...","Downloading data..",false,false);

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
                            datumArrayList.clear();

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

                            Toast.makeText(MainActivity.this, "Data Successfully downloaded", Toast.LENGTH_SHORT).show();

                            CheckDataAvailability();

//                               smartAdapter = new SmartAdapter(datumArrayList, MainActivity.this);
//                               recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
//                               recyclerView.setAdapter(smartAdapter);
                        }
                        else{
                            Toast.makeText(MainActivity.this, "No dataa", Toast.LENGTH_SHORT).show();
                        }
                    }
                    else{
                        Log.d(TAG, "image response"+ response.toString());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this,"Exception"+e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(loading.isShowing()){
                    loading.dismiss();
                }
                Log.e(TAG," error"+error.getMessage());
                Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
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





















}
