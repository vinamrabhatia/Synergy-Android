package com.example.vinamra.synergy2;

/**
 * Created by vinamra on 12/11/17.
 */

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.support.v4.app.NotificationCompat;

import java.util.ArrayList;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity
{
    private ListView mList;
    private ArrayList<String> arrayList;
    private MyCustomAdapter mAdapter;
    private TCPClient mTcpClient;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(onNotice, new IntentFilter("Msg"));

        arrayList = new ArrayList<String>();
        Log.v("Looking for permissions", "Yes");
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE},
                666);

        Intent intent = new Intent(this, NotificationService.class); startService(intent);

        Intent intent1=new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        startActivity(intent1);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE)
                != PackageManager.PERMISSION_GRANTED)
        {
            Log.d(TAG, "permission denied");
        }
        else
            Log.d(TAG, "permission granted");

        final EditText editText = (EditText) findViewById(R.id.editText);
        Button send = (Button)findViewById(R.id.send_button);

        //relate the listView from java to the one created in xml
        mList = (ListView)findViewById(R.id.list);
        mAdapter = new MyCustomAdapter(this, arrayList);
        mList.setAdapter(mAdapter);

        // connect to the server
        new connectTask().execute("");

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String message = editText.getText().toString();

                //add the text in the arrayList
                arrayList.add("c: " + message);

                //sends the message to the server
                if (mTcpClient != null) {
                    mTcpClient.sendMessage(message);
                }

                //refresh the list
                mAdapter.notifyDataSetChanged();
                editText.setText("");
            }
        });

    }

    private void addNotification(String title, String text) {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_stat_name)
                        .setContentTitle(title)
                        .setContentText(text);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);

        // Add as notification
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, builder.build());
    }

    private BroadcastReceiver onNotice= new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // String pack = intent.getStringExtra("package");
            String title = intent.getStringExtra("title");
            String text = intent.getStringExtra("text");
            String ticker = intent.getStringExtra("ticker");
            String pack = intent.getStringExtra("pack");
            //int id = intent.getIntExtra("icon",0);
            Log.v("Broadcast Received", title + text);

            if (mTcpClient != null) {
                mTcpClient.sendMessage(pack);
                mTcpClient.sendMessage(ticker);
                mTcpClient.sendMessage(title);
                mTcpClient.sendMessage(text);
            }
            Context remotePackageContext = null;
            /*try {
//                remotePackageContext = getApplicationContext().createPackageContext(pack, 0);
//                Drawable icon = remotePackageContext.getResources().getDrawable(id);
//                if(icon !=null) {
//                    ((ImageView) findViewById(R.id.imageView)).setBackground(icon);
//                }
                byte[] byteArray =intent.getByteArrayExtra("icon");
                Bitmap bmp = null;
                if(byteArray !=null) {
                    bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                }
                Model model = new Model();
                model.setName(title +" " +text);
                model.setImage(bmp);

                if(modelList !=null) {
                    modelList.add(model);
                    adapter.notifyDataSetChanged();
                }else {
                    modelList = new ArrayList<Model>();
                    modelList.add(model);
                    adapter = new CustomListAdapter(getApplicationContext(), modelList);
                    list=(ListView)findViewById(R.id.list);
                    list.setAdapter(adapter);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }*/
        }
    };

    public class connectTask extends AsyncTask<String,String,TCPClient> {

        @Override
        protected TCPClient doInBackground(String... message) {

            //we create a TCPClient object and
            mTcpClient = new TCPClient(new TCPClient.OnMessageReceived() {
                @Override
                //here the messageReceived method is implemented
                public void messageReceived(String message) {
                    //this method calls the onProgressUpdate
                    publishProgress(message);
                    addNotification("Synergy", message);
                }
            });
            mTcpClient.run();

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

            //in the arrayList we add the messaged received from server
            arrayList.add(values[0]);
            // notify the adapter that the data set has changed. This means that new message received
            // from server was added to the list
            mAdapter.notifyDataSetChanged();
        }
    }
}