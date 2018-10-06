package com.smartagent;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.smartagent.Adapter.SmartAdapter;
import com.smartagent.Bean.Bean1;

import com.smartagent.Bean.RealmObj;
import com.smartagent.Service.SimpleJobIntentService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

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
                    VolleyGetData();
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
            if(offlineobj.size() > 0){
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
                    VolleyGetData();
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

    private void VolleyGetData() {
        String DATA_URL = getResources().getString(R.string.data_url);
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

                               Bean1 datum = new Bean1(id,name, type, size, path );
                               datumArrayList.add(datum);

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

                                    Toast.makeText(MainActivity.this, "Dataa Successfully downloaded", Toast.LENGTH_SHORT).show();

                                    smartAdapter = new SmartAdapter(datumArrayList, MainActivity.this);
                                    recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                                    recyclerView.setAdapter(smartAdapter);
                       }
                       else{
                           Toast.makeText(MainActivity.this, "No dataa", Toast.LENGTH_SHORT).show();
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
            RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
            requestQueue.getCache().clear();
            //Adding our request to the queue
            requestQueue.add(jsonObjectRequest);

    }


}
