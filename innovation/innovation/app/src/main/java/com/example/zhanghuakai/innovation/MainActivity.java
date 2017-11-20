package com.example.zhanghuakai.innovation;

import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.RemoteException;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.Collection;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements BeaconConsumer, RangeNotifier, MonitorNotifier {

    private Button mFirebaseBtn;
    private EditText mNameField;
    private EditText mEmailField;
    private TextView mNameView;
    private TextView mTextMessage;

    private DatabaseReference mDatabase;
    private DatabaseReference mDatabaseview;

    //Value of beacon
    private BeaconManager beaconManager;
    protected static final String TAG = "AnonymousAuth";
    private static  final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION=1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //beacon
        beaconManager = BeaconManager.getInstanceForApplication(this);
        initBeaconManager();
        beaconManager.bind(this);
        checkBluetooth();
        checkLocalisation();


        //recognize by their id
        mFirebaseBtn = (Button) findViewById(R.id.firebase_btn);
        mNameField = (EditText) findViewById(R.id.name_field);
        mEmailField = (EditText) findViewById(R.id.email_filed);
        mNameView = (TextView) findViewById(R.id.name_view);
        mTextMessage = (TextView) findViewById(R.id.message_view);


        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabaseview = FirebaseDatabase.getInstance().getReference().child("Name");


        mDatabaseview.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String name = dataSnapshot.getValue().toString().trim();
                mNameView.setText("Name : " + name);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        mFirebaseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //1. create child in root object
                //2. assign some values to the child

                String name = mNameField.getText().toString().trim();
                String email = mEmailField.getText().toString().trim();
                HashMap<String, String> datamap = new HashMap<>();

                datamap.put("Name", name);
                datamap.put("Email", email);

                mDatabase.push().setValue(datamap);

            }
        });


    }

    //method of beacon

    @Override
    public void onBeaconServiceConnect() {
        mTextMessage.setText("start onBeaconServiceConnect");
        beaconManager.addRangeNotifier(this);
        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
            Log.e(TAG, "START RANGING");
            mTextMessage.setText("Start Monitoring");
        } catch (RemoteException e) {
            Log.e(TAG,"ERROR ");
            e.printStackTrace();
        }
    }

    private void initBeaconManager() {
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BeaconParser.ALTBEACON_LAYOUT));
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:0-3=4c000215,i:4-19,i:20-21,i:22-23,p:24-24"));
    }

    @TargetApi(17)
    private void checkBluetooth() {
        try {
            if (!BeaconManager.getInstanceForApplication(this).checkAvailability()) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Bluetooth not enabled");
                builder.setMessage("Please enable bluetooth in settings and restart this application.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish();
                        System.exit(0);
                    }
                });
                builder.show();
            }
        } catch (RuntimeException e) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Bluetooth LE not available");
            builder.setMessage("Sorry, this device does not support Bluetooth LE.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    finish();
                    System.exit(0);
                }
            });
            builder.show();
        }
    }

    private void checkLocalisation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons in the background.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @TargetApi(23)
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons,
                                        Region region) {
        if (beacons.size() > 0) {
            Beacon firstBeacon = beacons.iterator().next();
            setText(mTextMessage, "The first beacon " + firstBeacon.toString() + " is about " + firstBeacon.getDistance() + " meters away.");
                    // Write a message to the database
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference myRef = database.getReference("testBeacon");
            myRef.push().setValue(beacons);
        }
    }

    @Override
    public void didEnterRegion(Region region) {
        Log.e(TAG, "Enter a beacon");
        setText(mTextMessage,"Enter a beacon ");
    }
    @Override
    public void didExitRegion(Region region) {
        Log.e(TAG, "I no longer see an beacon");
        setText(mTextMessage,"I no longer see an beacon");
    }
    @Override
    public void didDetermineStateForRegion(int i, Region region) {
    }
    private void setText(final TextView text,final String value){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text.setText(value);
            }
        });
    }



}



