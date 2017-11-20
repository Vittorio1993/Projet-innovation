package com.example.zhanghuakai.innovation;

import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.RemoteException;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity implements BeaconConsumer, RangeNotifier, MonitorNotifier {

    private Button mFirebaseBtn;
    private Button mCheckBtn;
    private Button mSubmitBtn;
    private EditText mNameField;
    private TextView mNameView;
    private TextView mTextMessage;
    private TextView mClassroomView;
    private TextView mClassView;
    private TextView mClasslistView;
    private String nameStudent;
    private String classroomStudent;
    private String classStudent;
    private String codeStudent ;

    private Beacon beaconsinfo=new Beacon(){};

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
        //recognize by their id
        mDatabase = FirebaseDatabase.getInstance().getReference("Present");


        mDatabaseview = FirebaseDatabase.getInstance().getReference();

        final HashMap<String, String> datamap = new HashMap<>();

        final ArrayList<String> listclass=new ArrayList<>();
        Date day=new Date();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        final String date=df.format(day).toString();
        SimpleDateFormat tf = new SimpleDateFormat("HH:mm:ss");
        final String time=tf.format(day).toString();




        mFirebaseBtn = (Button) findViewById(R.id.firebase_btn);
        mNameField = (EditText) findViewById(R.id.name_field);
        mNameView = (TextView) findViewById(R.id.name_view);
        mTextMessage = (TextView) findViewById(R.id.message_view);
        mClassroomView = (TextView) findViewById(R.id.classroom_view);
        mClassView = (TextView) findViewById(R.id.class_view);
        mClasslistView=(TextView)findViewById(R.id.classlist_view);

        mCheckBtn=(Button) findViewById((R.id.check_btn));
        mSubmitBtn=(Button) findViewById(R.id.submit_btn);

        //beacon
        beaconManager = BeaconManager.getInstanceForApplication(this);
        initBeaconManager();
        beaconManager.bind(this);
        checkBluetooth();
        checkLocalisation();

        mFirebaseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //1. create child in root object
                //2. assign some values to the child



                codeStudent = mNameField.getText().toString().trim();
                getIntent().putExtra("codeS",codeStudent);


                //afficher le nom d'etudiant
               mDatabaseview.child("Student").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        int m=0;
                        int n=0;
                        for(DataSnapshot code:dataSnapshot.getChildren()){
                            m=m+1;
                            n=n+1;
                            if (code.getKey().toString().equals(codeStudent)){
                                nameStudent=code.child("Name").getValue().toString();
                                mNameView.setText("Welcome "+nameStudent+" ! ");
                                n=n-1;
                                mCheckBtn.setEnabled(true);
                                mSubmitBtn.setEnabled(true);
                                mCheckBtn.setVisibility(View.VISIBLE);
                            }

                        }
                        if(m==n){
                            mNameView.setText("This code doesn't exist");
                            nameStudent="";
                            mCheckBtn.setEnabled(false);
                            mSubmitBtn.setEnabled(false);
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

              //afficher tous les classes d'etudiant
                mDatabaseview.child("Student").child(getIntent().getExtras().get("codeS").toString()).child("class").addValueEventListener(new ValueEventListener() {

                    String str="";
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for(DataSnapshot code:dataSnapshot.getChildren()){
                            if(code.child("date").getValue().toString().equals(date)){
                                str=str+"Class : "+code.child("name").getValue().toString()+"      Class room :"+code.child("classroom").getValue().toString()+"      Time : "+code.child("time").getValue().toString()+"\n";
                            }
                        }
                        mClasslistView.setText(str);

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });





                //afficher le classroom d'etudiant
                mDatabaseview.child("Classroom").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for(DataSnapshot code:dataSnapshot.getChildren()){
                            if(code.getKey().toString().equals(beaconsinfo.toString())){
                                classroomStudent=code.child("name").getValue().toString();
                                mClassroomView.setText("This classroom is "+classroomStudent);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }
        });

        mCheckBtn.setOnClickListener(new View.OnClickListener() {
         @Override
             public void onClick(View view) {
             //afficher le cours
             mDatabaseview.child("Student").child(getIntent().getExtras().get("codeS").toString()).child("class").addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot code:dataSnapshot.getChildren()){

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Calendar timeplan = Calendar.getInstance();
                    try {
                        timeplan.setTime(sdf.parse(code.child("time").getValue().toString()));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    Calendar systime = Calendar.getInstance();
                    try {
                        timeplan.setTime(sdf.parse(time));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    Date timeplanstar=timeplan.getTime();
                    timeplan.add(Calendar.HOUR_OF_DAY,3);
                    Date timeplanfini=timeplan.getTime();
                    Date timereal=systime.getTime();
                    HashMap<String,String> map=new HashMap<>();
                    map.put("state","yes");

                    if (code.child("classroom").getValue().toString().equals(classroomStudent)&&timeplanstar.before(timereal)&&timereal.before(timeplanfini)){
                        classStudent=code.child("name").getValue().toString();
                       // String codeclass=code.getKey().toString();

                        /*mDatabaseview.setValue(map);
                        mDatabaseview.child("Student").child(codeStudent).child("class").child(codeclass).setValue(map);*/

                        break;
                    }else{
                        classStudent="";
                    }

                }
                if (classStudent.equals("")){
                    mClassView.setText("You don't have class in this calssroom in this moment");
                    mSubmitBtn.setEnabled(false);
                }else{
                    mClassView.setText("Your class is "+classStudent);
                    mSubmitBtn.setEnabled(true);
                    mSubmitBtn.setVisibility(View.VISIBLE);
                }


            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

             });
    }
});

        mSubmitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar systime = Calendar.getInstance();
                Date dday=systime.getTime();
                SimpleDateFormat ttf = new SimpleDateFormat("HH:mm:ss");
                final String itime=ttf.format(dday).toString();
        datamap.put("Code", codeStudent);
        datamap.put("Name", nameStudent);
        datamap.put("beaconinfo",beaconsinfo.toString());
        datamap.put("Date",date);
        datamap.put("Time",itime);
        datamap.put("Class",classStudent);
        datamap.put("Classroom",classroomStudent);
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
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons,Region region) {
        String str ="";
        Integer i=1;
        Iterator iterList = beacons.iterator();


        if (beacons.size() > 0) {
            Beacon Beaconmin = (Beacon) iterList.next();
            while (iterList.hasNext()){
                Beacon firstBeacon = (Beacon) iterList.next();
                if(firstBeacon.getDistance()<Beaconmin.getDistance()){
                    Beaconmin=firstBeacon;
                }
            }
            str=str+"The beacon " +i+ Beaconmin.toString() + " is about " + Beaconmin.getDistance() + " meters away.";
            setText(mTextMessage,str);
            beaconsinfo=Beaconmin;
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



