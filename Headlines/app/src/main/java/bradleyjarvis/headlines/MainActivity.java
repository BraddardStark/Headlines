package bradleyjarvis.headlines;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    //Create Global Variables that will be used
    double lat;
    double longi;
    String name;
    String countryCode;
    String temperature;
    Gson gson = new Gson();
    ArrayList<String> news = new ArrayList<String>();
    ArrayList<String> detail = new ArrayList<String>();
    ArrayList<String> urls = new ArrayList<String>();
    public Location mLastLocation;
    public GoogleApiClient mGoogleApiClient;
    SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //If internet connection exists build the api client
        if (isConnectedToInternet()) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();

            mGoogleApiClient.connect();
        } else {
            //Else load from storage
            loadFromStorage();
        }
    }

    //Check if there is an internet connection
    public boolean isConnectedToInternet() throws SecurityException {
        ConnectivityManager connectivity = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null)
                for (int i = 0; i < info.length; i++)
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
        }
        return false;
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        final ExpandableListView list = (ExpandableListView) findViewById(R.id.newsList);
        //Create a listener that detects when a child has been clicked in the expandable list
        list.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                //Get the url of the clicked child
                String url = urls.get(groupPosition);
                Uri webpage = Uri.parse(url);
                //Load the url in an external browser application
                Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
                startActivity(intent);
                return false;
            }
        });
        //Create a listener the detects when a parent has been expanded in the expandable list
        list.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            //Define previous group
            int previousGroup = -1;

            @Override
            public void onGroupExpand(int groupPosition) {
                //Check if the previous position is the same as the new position
                if (groupPosition != previousGroup)
                    //Collapse the previous group
                    list.collapseGroup(previousGroup);
                //Set the previous group to be the same as the current group
                previousGroup = groupPosition;
            }
        });
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        //Create a swipeRefresh listener
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            //If refreshed, refresh content
            public void onRefresh() {
                refreshContent();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    //Google api connected
    public void onConnected(Bundle bundle) {
        //Get users location
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if(mLastLocation != null){
            //Store location in variables
            lat = mLastLocation.getLatitude();
            longi = mLastLocation.getLongitude();
            //Call async API calls
            new AsyncGetLocation().execute();
            new AsyncGetWeather().execute();
        }else {
            //If location isn't available use predefined values
            lat = 52.2;
            longi = 0.1;
            //Call async API calls
            new AsyncGetLocation().execute();
            new AsyncGetWeather().execute();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        //If connection suspended connect to api again
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("TEST", "FAILED: " + connectionResult.toString());
    }

    //Aysnc task to get name of location
    public class AsyncGetLocation extends AsyncTask<String, String, String> {
        //Create the API url
        String yourServiceUrl = "http://api.opencagedata.com/geocode/v1/json?q="+lat+"+"+longi+"&key=5401c1821d04ae00be38d4c35b07bf0c";
        @Override
        protected void onPreExecute() {}

        @Override
        protected String doInBackground(String... arg0)  {
            try {
                Log.d("TEST", "" + lat + longi);
                //Create a new instance of HTTPConnect
                HTTPConnect jParser = new HTTPConnect();
                //Get json from API url
                String json = jParser.getJSONFromUrl(yourServiceUrl);
                //Get json object from returned json
                JSONObject jsonObject = new JSONObject(json);
                //Get json array inside json object
                JSONArray jsonArray = jsonObject.getJSONArray("results");
                //Get json object inside json array
                JSONObject jsonObject1 = jsonArray.getJSONObject(0);
                //Get json object inside json object
                JSONObject jsonObject2 = jsonObject1.getJSONObject("components");

                if(jsonObject2.has("city")){
                    //Set name to name of city
                    name = jsonObject2.getString("city");
                }
                else if (jsonObject2.has("town")){
                    //Set name to name of town
                    name = jsonObject2.getString("town");
                }
                else if (jsonObject2.has("village")){
                    //Set name to name of village
                    name = jsonObject2.getString("village");
                }
                else if (jsonObject2.has("county")){
                    //Set name to name of county
                    name = jsonObject2.getString("county");
                }
                //Get country code
                countryCode = jsonObject2.getString("country_code");

                Log.d("TEST", "" + name);

            } catch (JSONException e) {
                e.printStackTrace();
                //If unable to return json load from storage
                loadFromStorage();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String strFromDoInBg) {
            //Find textview
            TextView tv = (TextView)findViewById(R.id.locationText);
            //Set textview to display name
            tv.setText(name);
            //Find imageview
            ImageView iv = (ImageView)findViewById(R.id.landscape);
            //Get imageview width and height
            int imageWidth = iv.getWidth();
            int imageHeight = iv.getHeight();
            //Use Picasso to load in image from API
            Picasso
                    .with(getApplicationContext())
                    //API call
                    .load("https://maps.googleapis.com/maps/api/staticmap?center="+lat+","+longi+"&zoom=14&size=540x320&key=AIzaSyDwUH_2gq6CxS-fjl31EIGwQs5zpFlD8Q8")
                    //Resize image to match imageview
                    .resize(imageWidth,imageHeight)
                    //Crop the image to fill the imageview
                    .centerCrop()
                    //Insert into imageview
                    .into(iv);
            //Aysnc API call to get news
            new AsyncGetNews().execute();
        }
    }

    public class AsyncGetNews extends AsyncTask<String, String, String> {

        //Create the API url
        String yourServiceUrl = "https://api.cognitive.microsoft.com/bing/v5.0/news/search?q="+name+"+loc:"+countryCode+"&count=10&safeSearch=Moderate";

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected String doInBackground(String... arg0) {
            try {
                //Create a new instance of HTTPConnect
                HTTPConnect jParser = new HTTPConnect();
                //Get json from API url
                String json = jParser.getJSONFromUrl(yourServiceUrl);
                //Get json object from returned json
                JSONObject jsonObject = new JSONObject(json);
                //Get json array from json object
                JSONArray jsonArray = jsonObject.getJSONArray("value");
                //Clear the previous lists
                news.clear();
                detail.clear();
                urls.clear();
                //Loop through json array
                for (int i = 0; i < jsonArray.length(); i++){
                    //Get json object from json array at current location of i
                    JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                    //Get headline from json object
                    String headline = jsonObject1.getString("name");
                    //Get description from json object
                    String description = jsonObject1.getString("description");
                    //Get url from json object
                    String url = jsonObject1.getString("url");

                    //Add headline to list
                    news.add(headline);
                    //Add description to list
                    detail.add(description);
                    //Add url to list
                    urls.add(url);
                }


            } catch (JSONException e) {
                e.printStackTrace();
                //If unable to return json load from storage
                loadFromStorage();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String strFromDoInBg) {
            //Create expandablelist
            createExpandableList();
            //Save everything
            saveEverything();
        }
    }

    public class AsyncGetWeather extends AsyncTask<String, String, String> {

        //Create the API url
        String yourServiceUrl = "https://api.darksky.net/forecast/ea8eb0b0229015d05bc55c0e076123a2/"+lat+","+longi+"";

        @Override
        // this method is used for......................
        protected void onPreExecute() {
        }

        @Override
        // this method is used for...................
        protected String doInBackground(String... arg0) {
            try {
                //Create new instance of HTTPConnect
                HTTPConnect jParser = new HTTPConnect();

                //Get json from API url
                String json = jParser.getJSONFromUrl(yourServiceUrl);
                Log.d("TEST", yourServiceUrl);
                //Get json object from json
                JSONObject jsonObject = new JSONObject(json);
                //Get json object from json object
                JSONObject jsonObject1 = jsonObject.getJSONObject("currently");
                //Get temperature from json object
                temperature = jsonObject1.getString("temperature");

            } catch (JSONException e) {
                e.printStackTrace();
                //If unable to return json load from storage
                loadFromStorage();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String strFromDoInBg) {
            //Find textview
            TextView tv = (TextView)findViewById(R.id.temperatureText);
            //Convert temperature from fahrenheit to celsius
            //Parse temperature into a double
            double result = Double.parseDouble(temperature);
            Log.d("TEST", ""+ result);
            result = ((result - 32)/1.8);
            //Round double to nearest whole number and store in integer
            int temp = (int) Math.round(result);
            Log.d("TEST", ""+ temp);
            //Write integer temp back to temperature as string
            temperature = Integer.toString(temp);
            Log.d("TEST", ""+temperature);
            //Set textview to display temperature
            tv.setText(temperature + "°C");
        }
    }

    //Method to save to storage
    public void saveEverything(){
        try {
            //Get shared preferences saved data
            SharedPreferences savedData = getSharedPreferences("savedData", Context.MODE_PRIVATE);
            //Edit saved data
            SharedPreferences.Editor editor = savedData.edit();
            //Add name to saved data
            editor.putString("name", name);
            //Add country code to saved data
            editor.putString("countryCode", countryCode);
            //Add temperature to saved data
            editor.putString("temperature", temperature);
            //Convert news list back to json and add to saved data
            editor.putString("news", "" + gson.toJson(news));
            //Convert detail list back to json and add to saved data
            editor.putString("detail", "" + gson.toJson(detail));
            //Convert urls list back to json and add to saved data
            editor.putString("urls", "" + gson.toJson(urls));
            editor.commit();
            Log.d("TEST", "SAVED");
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    //Method to create expandablelist
    public void createExpandableList(){
        //Find expandablelist
        ExpandableListView list = (ExpandableListView)findViewById(R.id.newsList);
        //Get child items from hashmap
        HashMap<String, List<String>> allChildItems = returnGroupedChildItems();
        //Create new expandable list adapter with news headlines and child items
        ExpandableListViewAdapter expandableListViewAdapter = new ExpandableListViewAdapter(getApplicationContext(), news, allChildItems);
        //Set the adapter
        list.setAdapter(expandableListViewAdapter);
    }

    private HashMap<String, List<String>> returnGroupedChildItems(){
        //Create a hashmap for child items
        HashMap<String, List<String>> childContent = new HashMap<String, List<String>>();
        //Loop through detail list
        for (int i = 0; i < detail.size(); i++) {
            //Add child content from details to match headline
            childContent.put(news.get(i), detail.subList(i,i+1));
        }
        return childContent;
    }

    public void loadFromStorage(){
        //Get saved data from shared preferences
        SharedPreferences savedData = getSharedPreferences("savedData", Context.MODE_PRIVATE);
        Log.d("TEST", "LOADED");
        //If saved data contains some data
        if(savedData.contains("name")) {
            //Load name from saved data
            name = savedData.getString("name", "");
            //Append name to show offline
            name = name + " - Offline";
            //Load temperature from saved data
            temperature = savedData.getString("temperature", "");
            //Load news from saved data and convert from json to list
            news = gson.fromJson(savedData.getString("news", ""), news.getClass());
            //Load detail from saved data and convert from json to list
            detail = gson.fromJson(savedData.getString("detail", ""), detail.getClass());
            //Load urls from saved data and convert from json to list
            urls = gson.fromJson(savedData.getString("urls", ""), urls.getClass());
            //Find textview
            TextView tv = (TextView) findViewById(R.id.locationText);
            //Set textview to display name
            tv.setText(name);
            //Find textview
            TextView tv1 = (TextView) findViewById(R.id.temperatureText);
            //Set textview to display temperature
            tv1.setText(temperature + "°C");
            //Create expandable list using loaded data
            createExpandableList();
        }
        else{
            //If no saved data found display message stating internet connection is required
            Toast.makeText(getApplicationContext(),"Internet Connection Required", Toast.LENGTH_LONG).show();
            //Create a new dialog
            AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
            dialog.setMessage("Internet Connection Required");
            //Take user to settings on click
            dialog.setPositiveButton("Go to settings", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //Intent to open up wireless settings
                    Intent intent = new Intent(Settings.ACTION_SETTINGS);
                    startActivity(intent);
                }
            });
            //Get rid of dialog on click
            dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            //Show the dialog
            dialog.show();

        }
    }

    //Method to refresh content
    private void refreshContent(){
        //If internet connection exists build the api client
        if(isConnectedToInternet()){
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();

            mGoogleApiClient.connect();
        }
        else{
            //Else load from storage
            loadFromStorage();
        }
        //Stop displaying refresh animation
        mSwipeRefreshLayout.setRefreshing(false);
    }
}
