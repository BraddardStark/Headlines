package bradleyjarvis.headlines;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import org.apache.http.client.methods.HttpPostHC4;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    double lat;
    double longi;
    String name;
    String countryCode;
    String imageUrl;
    String test;
    Gson gson = new Gson();
    ArrayList<String> news = new ArrayList<String>();
    ArrayList<String> detail = new ArrayList<String>();
    ArrayList<String> urls = new ArrayList<String>();
    public Location mLastLocation;
    public GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();



        mGoogleApiClient.connect();
        Log.d("TEST", "CONNECT");

    }

    public void onWindowFocusChanged(boolean hasFocus) {
        final ExpandableListView list = (ExpandableListView)findViewById(R.id.newsList);
        list.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                Log.d("TEST", "CHILD CLICKED" + groupPosition + childPosition);
                Log.d("TEST", "" + urls.get(groupPosition));
                String url = urls.get(groupPosition);
                Uri webpage = Uri.parse(url);
                Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
                startActivity(intent);
                return false;
            }
        });
        list.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            int previousGroup = -1;
            @Override
            public void onGroupExpand(int groupPosition) {
                if(groupPosition != previousGroup)
                    list.collapseGroup(previousGroup);
                previousGroup = groupPosition;
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
    public void onConnected(Bundle bundle) {
        Log.d("TEST", "CONNECTED");
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        //if location can be found, then execute with the values
        //if not, add mock values
        if(mLastLocation != null){
            lat = mLastLocation.getLatitude();
            longi = mLastLocation.getLongitude();
            new AsyncTaskParseJson().execute();
        }else {
            lat = 53.23;
            longi = -0.54;
            new AsyncTaskParseJson().execute();
        }

    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("TEST", "FAILED: " + connectionResult.toString());
    }

    // added asynctask class methods below -  you can make this class as a separate class file
    public class AsyncTaskParseJson extends AsyncTask<String, String, String> {

        // set the url of the web service to call
        String yourServiceUrl = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" + lat + "," + longi + "&key=AIzaSyDCwbywHeHkdn_CRqJ9xraBvz_r2ZPKPIM";

        @Override
        // this method is used for......................
        protected void onPreExecute() {}

        @Override
        // this method is used for...................
        protected String doInBackground(String... arg0)  {
            try {
                // create new instance of the httpConnect class
                HTTPConnect jParser = new HTTPConnect();

                // get json string from service url
                String json = jParser.getJSONFromUrl(yourServiceUrl);
                Log.d("TEST", yourServiceUrl);
                // parse returned json string into json array
                JSONObject jsonObject = new JSONObject(json);

                JSONArray jsonArray = jsonObject.getJSONArray("results");

                // loop through json array and add each tweet to item in arrayList
                JSONObject json_message = jsonArray.getJSONObject(0);

                JSONArray jsonArray1 = json_message.getJSONArray("address_components");

                JSONObject jsonObject1 = jsonArray1.getJSONObject(2);

                JSONObject jsonObject2 = jsonArray1.getJSONObject(6);

                name = jsonObject1.getString("long_name");
                countryCode = jsonObject2.getString("short_name");
                Log.d("TEST","" + countryCode);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        // below method will run when service HTTP request is complete, will then bind tweet text in arrayList to ListView
        protected void onPostExecute(String strFromDoInBg) {
            // bind the values of the ArrayList to the ListView to display the tweets
            TextView tv = (TextView)findViewById(R.id.textView);
            tv.setText(name);
            new AsyncGetImage().execute();
            new AsyncGetNews().execute();
        }
    }

    // added asynctask class methods below -  you can make this class as a separate class file
    public class AsyncGetImage extends AsyncTask<String, String, String> {

        // set the url of the web service to call
        String yourServiceUrl = "https://api.cognitive.microsoft.com/bing/v5.0/images/search?q="+name+ "+" +countryCode+"&count=1&safeSearch=Moderate";


        @Override
        // this method is used for......................
        protected void onPreExecute() {

        }

        @Override
        // this method is used for...................
        protected String doInBackground(String... arg0)  {
            try {
                // create new instance of the httpConnect class
                HTTPConnect jParser = new HTTPConnect();
                // get json string from service url
                String json = jParser.getJSONFromUrl(yourServiceUrl);

                // parse returned json string into json array
                JSONObject jsonObject = new JSONObject(json);
                JSONArray jsonArray = jsonObject.getJSONArray("value");
                JSONObject jsonObject1 = jsonArray.getJSONObject(0);
                imageUrl = jsonObject1.getString("contentUrl");
                Log.d("TEST", imageUrl);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        // below method will run when service HTTP request is complete, will then bind tweet text in arrayList to ListView
        protected void onPostExecute(String strFromDoInBg) {
            // bind the values of the ArrayList to the ListView to display the tweets
            ImageView iv = (ImageView)findViewById(R.id.landscape);
            int imageWidth = iv.getWidth();
            int imageHeight = iv.getHeight();
            Picasso
                    .with(getApplicationContext())
                    .load(imageUrl)
                    .resize(imageWidth,imageHeight)
                    .centerCrop()
                    .into(iv);
        }
    }
    // added asynctask class methods below -  you can make this class as a separate class file
    public class AsyncGetNews extends AsyncTask<String, String, String> {

        // set the url of the web service to call
        String yourServiceUrl = "https://api.cognitive.microsoft.com/bing/v5.0/news/search?q=Lincoln+loc:GB&count=10&safeSearch=Moderate";


        @Override
        // this method is used for......................
        protected void onPreExecute() {
        }

        @Override
        // this method is used for...................
        protected String doInBackground(String... arg0) {
            Log.d("TEST", "" + yourServiceUrl);
            try {
                Log.d("TEST", "TEST WORKED");
                // create new instance of the httpConnect class
                HTTPConnect jParser = new HTTPConnect();
                // get json string from service url

                String json = jParser.getJSONFromUrl(yourServiceUrl);

                // parse returned json string into json array
                JSONObject jsonObject = new JSONObject(json);

                Log.d("TEST", "" + json);
                JSONArray jsonArray = jsonObject.getJSONArray("value");

                for (int i = 0; i < jsonArray.length(); i++){
                    JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                    String headline = jsonObject1.getString("name");
                    Log.d("TEST", "" + headline);
                    String description = jsonObject1.getString("description");
                    String url = jsonObject1.getString("url");

                    news.add(headline);
                    detail.add(description);
                    urls.add(url);
                }


            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        // below method will run when service HTTP request is complete, will then bind tweet text in arrayList to ListView
        protected void onPostExecute(String strFromDoInBg) {
            ExpandableListView list = (ExpandableListView)findViewById(R.id.newsList);
//            ArrayAdapter<String> newsArrayAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_expandable_list_item_1, news);
//            list.setAdapter(newsArrayAdapter);

            HashMap<String, List<String>> allChildItems = returnGroupedChildItems();

            ExpandableListViewAdapter expandableListViewAdapter = new ExpandableListViewAdapter(getApplicationContext(), news, allChildItems);

            list.setAdapter(expandableListViewAdapter);

            saveEverything();

        }

        private HashMap<String, List<String>> returnGroupedChildItems(){

            HashMap<String, List<String>> childContent = new HashMap<String, List<String>>();

            for (int i = 0; i < detail.size(); i++) {
                childContent.put(news.get(i), detail.subList(i,i+1));
            }

            return childContent;

        }

    }

    public void saveEverything(){
//        String FILENAME = "stored_data.txt";
//        String string = "hello world!";
//
//        try {
//            FileOutputStream fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
//            fos.write(name.getBytes());
//            fos.write(countryCode.getBytes());
//            fos.close();
//            Log.d("TEST", "SAVED");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        try {
//            FileInputStream fis = openFileInput(FILENAME);
//            byte[] input = new byte[fis.available()];
//            while (fis.read(input) != -1) {}
//            test += new String(input);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        Log.d("TEST", "FILE SAYS " + test);

        try {
            SharedPreferences userInfo = getSharedPreferences("userData", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = userInfo.edit();
            // put the key values of the EditText widgets name and email into the shared preferences file
            editor.putString("name", name);
            editor.putString("countryCode", countryCode);
            editor.commit();
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        SharedPreferences userInfo = getSharedPreferences("userData", Context.MODE_PRIVATE);

        // set the name and email textView widget to the values from Shared Preferenced file 'userData'
        Log.d("TEST", "" + (userInfo.getString("user", "")));
        Log.d("TEST", "" + (userInfo.getString("email", "")));


    }
}
