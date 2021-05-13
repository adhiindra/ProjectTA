package com.example.projectta;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    String imei = "";
    String btAddress = "";
    String key = "";

    AlertDialog.Builder dialog;
    LayoutInflater inflater;
    View dialogView;

    // GUI Components
    private SwipeRefreshLayout swipeRefreshLayout;
    private TimePickerDialog timePickerDialog;
    private TextView mBluetoothStatus;
    private TextView mReadBuffer;
    private BluetoothAdapter mBTAdapter;
    private ArrayAdapter<String> mBTArrayAdapter;
    private ListView mDevicesListView;
    private TextView mDevicesTvImei;
    private EditText mDevicesTeImei;
    private ImageView ignition;
    private ImageView guest;
    private ImageView timer;
    private MaterialButton connect;
    private ImageView dUser;
    private ImageView cekTegangan;
    private MaterialButton timerJam;
    private TextView waktu;
    private TextView volt;
    private ImageView imgAki;
    private TextView stsAki;
    private MaterialButton finger1;
    private MaterialButton finger2;
    private MaterialButton finger3;
    private MaterialButton finger4;
    private MaterialButton addDevices;
    private MaterialButton cekImei;
    private Context ctx;
    private ArrayList<Integer> fingerprintButtons = new ArrayList<>();


    private boolean addbutton = false;
    private boolean isIgnitionOff = true;
    private boolean isTimerOff = true;
    private boolean isGuestOff = true;
    private boolean isNotConnected = true;
    private boolean finger1IsNull = true;
    private boolean finger2IsNull = true;
    private boolean finger3IsNull = true;
    private boolean finger4IsNull = true;
    private boolean fingerIsDeleted = false;
    private boolean fingerCanceled = false;

    private Handler mHandler; // Our main handler that will receive callback notifications
    private ConnectedThread mConnectedThread; // bluetooth background worker thread to send and receive data
    private BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    Handler handler = new Handler();

    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier


    // #defines for identifying shared types between calling functions
    private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status


    private void createAkun(){
        DatabaseReference databaseakun = database.getInstance().getReference("Devices/").push();
        databaseakun.child("Imei").setValue(imei);
        databaseakun.child("btAddress").setValue(btAddress);
        databaseakun.child("finger1").setValue(0);
        databaseakun.child("finger2").setValue(0);
        databaseakun.child("finger3").setValue(0);
        databaseakun.child("finger4").setValue(0);
    }

    private void createAkun2(String imei2){
        if(imei2!=null){
            DatabaseReference databaseakun = database.getInstance().getReference("Devices/"+key);
            databaseakun.child("Imei2").setValue(imei2);
        }
    }

    private void checkDevices(String Text){
        DatabaseReference databaseakun = database.getInstance().getReference("Devices/");
        Query query = databaseakun.orderByChild("btAddress").equalTo(btAddress);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // dataSnapshot is the "issue" node with all children with id 0

                    for (DataSnapshot user : dataSnapshot.getChildren()) {
                        // do something with the individual "issues"
                        DataSnapshot devices = dataSnapshot.child(user.getKey());
                        String dbImei = devices.child("Imei").getValue(String.class);
                        String dBtAddress = devices.child("btAddress").getValue(String.class);
                        String dbImei2 = devices.child("Imei2").getValue(String.class);
                        Integer finger1 = devices.child("finger1").getValue(Integer.class);
                        Integer finger2 = devices.child("finger2").getValue(Integer.class);
                        Integer finger3 = devices.child("finger3").getValue(Integer.class);
                        Integer finger4 = devices.child("finger4").getValue(Integer.class);


                        if (dbImei.equals(imei) || dbImei2.equals(imei)) {
                            key = devices.getKey();
//                            Toast.makeText(MainActivity.this,key , Toast.LENGTH_LONG).show();
                            isNotConnected = false;
                            mConnectedThread.write("10");
                            Log.i("BoolanCon",String.valueOf(isNotConnected));
                            Toast.makeText(MainActivity.this, Text, Toast.LENGTH_LONG).show();
                            connect.setText("DISCONNECT");
                            mReadBuffer.setText("-");
                            setVisible();
                            fingerprintButtons.add(finger1);
                            fingerprintButtons.add(finger2);
                            fingerprintButtons.add(finger3);
                            fingerprintButtons.add(finger4);
                            cekFingerButton();

                        } else {
                            mReadBuffer.setText("Perangkat ini Tidak Cocok!");
                            connect.setText("Disconnect");
                            connect.setEnabled(true);
                            isNotConnected = false;
                            Toast.makeText(MainActivity.this, "Perangkat ini Tidak Cocok!", Toast.LENGTH_LONG).show();
                        }
                    }
                } else {
                    if (btAddress == null){
                        Toast.makeText(MainActivity.this, "Silahkan Koneksikan Ke Bluetooth!", Toast.LENGTH_LONG).show();
                    }
                    else {
                        connect.setText("CREATING...");
                        connect.setEnabled(false);
                        createAkun();
                        checkDevices("Berhasil Membuat Akun");
//                        Toast.makeText(MainActivity.this, "Daftar Terlebih Dahulu!", Toast.LENGTH_LONG).show();
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }

        });
    }

    private void switchMode(String text){
        Log.i("Text",text);
        if("Mesin Hidup".equals(text)){
            isIgnitionOff = false;
            setVisible();
            mReadBuffer.setText(text);
            ignition.setImageResource(R.drawable.engine_1);
            Log.i("BoolanIg",String.valueOf(isIgnitionOff));
        }
        else if("Mesin Mati".equals(text)){
            isIgnitionOff = true;
            setVisible();
            mReadBuffer.setText(text);
            ignition.setImageResource(R.drawable.engineoff);
            Log.i("BoolanIg",String.valueOf(isIgnitionOff));
        }
        else if("Timer On".equals(text)){
            isTimerOff = false;
            mReadBuffer.setText(text);
            setVisible();
            timer.setImageResource(R.drawable.clock1);
            Log.i("BoolanIg",String.valueOf(isTimerOff));
        }
        else if("Timer Off".equals(text)){
            isTimerOff = true;
            setVisible();
            timer.setImageResource(R.drawable.clockoff);
            ignition.setImageResource(R.drawable.engineoff);
            isIgnitionOff = true;
            mReadBuffer.setText(text);
        }
        else if("Lock Mode".equals(text)){
            isGuestOff = true;
            isTimerOff = true;
            setVisible();
            timer.setImageResource(R.drawable.clockoff);
            guest.setImageResource(R.drawable.lock);
            mReadBuffer.setText(text);
        }
        else if("Guest Mode".equals(text)){
            isGuestOff = false;
            setVisible();
            guest.setImageResource(R.drawable.unlock);
            mReadBuffer.setText(text);
        }
        else if(text.startsWith("volt")){
            String tvolt = text.replace("volt ","");
            volt.setText(tvolt);
            float vVolt = Float.parseFloat(tvolt);
            if(vVolt < 12.0 && vVolt >= 11.0){
                imgAki.setImageResource(R.drawable.carbattery2);
                stsAki.setText("CEK AKI!");
            }
            else if(vVolt < 11.0){
                imgAki.setImageResource(R.drawable.carbattery3);
                stsAki.setText("GANTI AKI!");
            }
            else if(vVolt >=12.0 ){
                imgAki.setImageResource(R.drawable.carbattery1);
                stsAki.setText("BAIK");
            }
        }
        else if (text.startsWith("finger:")){
            String fText = text.replace("finger:","");
            if (fText.equals("Scan Sekali Lagi!")){
                mReadBuffer.setText(fText);
            }else if (fText.equals("Sukses!")){
                mReadBuffer.setText(fText);
                addbutton = true;
            }else if (fText.equals("Silahkan Scan Jari Anda!")){
                mReadBuffer.setText(fText);
            }else if (fText.equals("Deleted!")) {
                mReadBuffer.setText(fText);
                fingerIsDeleted = true;
            }
        }
        else if(text.startsWith("start:")){
            Log.i("statusLogin",text);
            String tText = text.replace("start:","");
            String[] split = tText.split(";");
            for(int i = 0; i < split.length; i++){
                if(i==1){
                    if(split[i].equals("0")){
                        switchMode("Lock Mode");
                    }else if(split[i].equals("1")) {
                        switchMode("Guest Mode");
                    }
                }else if(i==3){
                    if (split[i].equals("0")){
                        switchMode("Mesin Mati");
                    }else if (split[i].equals("1")){
                        switchMode("Mesin Hidup");
                    }
                }else if (i==2){
                    if (split[i].equals("0")){
                        switchMode("Timer Off");
                    }else if (split[i].equals("1")){
                        switchMode("Timer On");
                    }
                }else if(i==4){
                    waktu.setText(split[i]);
                }
                else {
                    switchMode(split[i]);
                }
            }
            swipeRefreshLayout.setRefreshing(false);
        }
        else if ("Disconnect".equals(text)){
            mConnectedThread.cancel();
        }

    }

    private void cekFingerButton(){
        int x = fingerprintButtons.size();
        if (x == 0){
            finger1.setText("+");
            finger1IsNull = true;
            finger2.setText("+");
            finger2IsNull = true;
            finger3.setText("+");
            finger3IsNull = true;
            finger4.setText("+");
            finger4IsNull = true;
        }else {
            for (int i = 0; i < x ;i++){
                if (fingerprintButtons.get(i)!=0){
                    switch(i){
                        case 0:
                            finger1.setText("ID 1");
                            finger1IsNull = false;
                            break;
                        case 1:
                            finger2.setText("ID 2");
                            finger2IsNull = false;
                            break;
                        case 2:
                            finger3.setText("ID 3");
                            finger3IsNull = false;
                            break;
                        case 3:
                            finger4.setText("ID 4");
                            finger4IsNull = false;
                    }
                }
            }
        }
    }

    private void setVisible(){

        ignition.setEnabled(true);
        timer.setEnabled(true);
        timerJam.setEnabled(true);
        guest.setEnabled(true);
        dUser.setEnabled(true);
        cekTegangan.setEnabled(true);
        connect.setEnabled(true);
        finger1.setEnabled(true);
        finger2.setEnabled(true);
        finger3.setEnabled(true);
        finger4.setEnabled(true);
        addDevices.setEnabled(true);
        swipeRefreshLayout.setEnabled(true);

    }

    private void setInVisible(){

        ignition.setEnabled(false);
        timer.setEnabled(false);
        timerJam.setEnabled(false);
        guest.setEnabled(false);
        dUser.setEnabled(false);
        cekTegangan.setEnabled(false);
        finger1.setEnabled(false);
        finger2.setEnabled(false);
        finger3.setEnabled(false);
        finger4.setEnabled(false);
        addDevices.setEnabled(false);

    }

    private void refreshListener(){
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mConnectedThread.write("10");
            }
        });
    }

    private void cekLocation(){
        int REQUEST_ACCESS_COARSE_LOCATION = 1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {  // Only ask for these permissions on runtime when running Android 6.0 or higher
            switch (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_FINE_LOCATION)) {
                case PackageManager.PERMISSION_DENIED:
                    ((TextView) new AlertDialog.Builder(ctx)
                            .setTitle("Runtime Permissions up ahead")
                            .setMessage(Html.fromHtml("<p>To find nearby bluetooth devices please click \"Allow\" on the runtime permissions popup.</p>" +
                                    "<p>For more info see <a href=\"http://developer.android.com/about/versions/marshmallow/android-6.0-changes.html#behavior-hardware-id\">here</a>.</p>"))
                            .setNeutralButton("Okay", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                        ActivityCompat.requestPermissions(MainActivity.this,
                                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                                REQUEST_ACCESS_COARSE_LOCATION);
                                    }
                                }
                            })
                            .show()
                            .findViewById(android.R.id.message))
                            .setMovementMethod(LinkMovementMethod.getInstance());       // Make the link clickable. Needs to be called after show(), in order to generate hyperlinks
                    break;
                case PackageManager.PERMISSION_GRANTED:
                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new);
        FirebaseDatabase database = FirebaseDatabase.getInstance();

        getImei();
        mBluetoothStatus = (TextView)findViewById(R.id.tv_stsBT);
        mReadBuffer = (TextView) findViewById(R.id.tv_sts);
        ignition = findViewById(R.id.bt_enggine);
        timer = findViewById(R.id.bt_timer);
        guest = findViewById(R.id.bt_mode);
        connect = findViewById(R.id.bt_koneksi);
        dUser = findViewById(R.id.bt_delAkun);
        cekTegangan = findViewById(R.id.img_aki);
        timerJam = findViewById(R.id.bt_setWaktu);
        waktu = findViewById(R.id.tv_waktu);
        volt = findViewById(R.id.tv_voltAki);
        imgAki = findViewById(R.id.img_aki);
        stsAki = findViewById(R.id.tv_stsAki);
        finger1 = findViewById(R.id.bt_finger1);
        finger2 = findViewById(R.id.bt_finger2);
        finger3 = findViewById(R.id.bt_finger3);
        finger4 = findViewById(R.id.bt_finger4);
        addDevices = findViewById(R.id.button_imei);
        cekImei = findViewById(R.id.button_cekimei);
        swipeRefreshLayout = findViewById(R.id.pullToRefresh);
        swipeRefreshLayout.setEnabled(false);
        ctx = this;

        cekLocation();
        setInVisible();
        refreshListener();

        mBTArrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio

        mHandler = new Handler(){
            public void handleMessage(android.os.Message msg){
                if(msg.what == MESSAGE_READ){
                    String readMessage = null;
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    readMessage = new String(readBuf, 0, msg.arg1);
                    switchMode(readMessage);
                    Log.i("BufferMsg", readMessage);
                }

                if(msg.what == CONNECTING_STATUS){
                    if(msg.arg1 == 1){
                        mBluetoothStatus.setText((String)(msg.obj));
                    }
                    else if(msg.arg1 == 2){
                        mBluetoothStatus.setText("DISCONNECTED");
                    }
                    else
                        mBluetoothStatus.setText("FAILED");
                }
            }
        };

        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isNotConnected){
                    Log.i("BoolanCon",String.valueOf(isNotConnected));
                    dialogFormBt();
                }else {
                    setInVisible();
                    ignition.setImageResource(R.drawable.enginepending);
                    guest.setImageResource(R.drawable.lockpending);
                    timer.setImageResource(R.drawable.clockpending);
                    imgAki.setImageResource(R.drawable.carbatterypending);
                    connect.setText("CONNECT");
                    mConnectedThread.cancel();
                    mBluetoothStatus.setText("DISCONNECTED");
                    volt.setText("-");
                    mReadBuffer.setText("-");
                    btAddress = null;
                    isNotConnected = true;
                    fingerprintButtons.clear();
                    cekFingerButton();
                }

            }
        });

        cekImei.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogFormCekImei();
            }
        });

        addDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogFormImei();
            }
        });

        finger1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                DatabaseReference user = database.getReference("Devices/"+key);
                if (finger1IsNull){
                    mConnectedThread.write("enfinger"+1);
                    finger1.setText("....");
                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            if (addbutton){
                                user.child("finger"+1).setValue(1);
                                finger1.setText("ID 1");
                                addbutton = false;
                                finger1IsNull = false;
                                return;
                            }

                            handler.postDelayed(this,100);
                        }
                    };
                    handler.postDelayed(r,100);
                }else {
                    builder.setCancelable(true);
                    builder.setTitle("WARNING!!");
                    builder.setMessage("Apakah Anda Yakin Untuk Menghapus Fingerprint?");
                    builder.setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mConnectedThread.write("delfinger"+1);
                                    Runnable r = new Runnable() {
                                        @Override
                                        public void run() {
                                            if (fingerIsDeleted){
                                                user.child("finger"+1).setValue(0);
                                                finger1.setText("+");
                                                fingerIsDeleted = false;
                                                finger1IsNull = true;
                                                return;
                                            }

                                            handler.postDelayed(this,100);
                                        }
                                    };
                                    handler.postDelayed(r,100);
                                }
                            });
                    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });

                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            }
        });

        finger2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                DatabaseReference user = database.getReference("Devices/"+key);
                if (finger2IsNull){
                    mConnectedThread.write("enfinger"+2);
                    finger2.setText("....");
                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            if (addbutton){
                                user.child("finger"+2).setValue(2);
                                finger2.setText("ID 2");
                                addbutton = false;
                                finger2IsNull = false;
                                return;
                            }

                            handler.postDelayed(this,100);
                        }
                    };
                    handler.postDelayed(r,100);
                }else {
                    builder.setCancelable(true);
                    builder.setTitle("WARNING!!");
                    builder.setMessage("Apakah Anda Yakin Untuk Menghapus Fingerprint?");
                    builder.setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mConnectedThread.write("delfinger"+2);
                                    Runnable r = new Runnable() {
                                        @Override
                                        public void run() {
                                            if (fingerIsDeleted){
                                                user.child("finger"+2).setValue(0);
                                                finger2.setText("+");
                                                fingerIsDeleted = false;
                                                finger2IsNull = true;
                                                return;
                                            }

                                            handler.postDelayed(this,100);
                                        }
                                    };
                                    handler.postDelayed(r,100);
                                }
                            });
                    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });

                    AlertDialog dialog = builder.create();
                    dialog.show();
                }

            }
        });

        finger3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                DatabaseReference user = database.getReference("Devices/"+key);
                if (finger3IsNull){
                    mConnectedThread.write("enfinger"+3);
                    finger3.setText("....");
                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            if (addbutton){
                                user.child("finger"+3).setValue(3);
                                finger3.setText("ID 3");
                                addbutton = false;
                                finger3IsNull = false;
                                return;
                            }

                            handler.postDelayed(this,100);
                        }
                    };
                    handler.postDelayed(r,100);
                }else {
                    builder.setCancelable(true);
                    builder.setTitle("WARNING!!");
                    builder.setMessage("Apakah Anda Yakin Untuk Menghapus Fingerprint?");
                    builder.setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mConnectedThread.write("delfinger"+3);
                                    Runnable r = new Runnable() {
                                        @Override
                                        public void run() {
                                            if (fingerIsDeleted){
                                                user.child("finger"+3).setValue(0);
                                                finger3.setText("+");
                                                fingerIsDeleted = false;
                                                finger3IsNull = true;
                                                return;
                                            }

                                            handler.postDelayed(this,100);
                                        }
                                    };
                                    handler.postDelayed(r,100);
                                }
                            });
                    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });

                    AlertDialog dialog = builder.create();
                    dialog.show();
                }

            }
        });

        finger4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                DatabaseReference user = database.getReference("Devices/"+key);
                if (finger4IsNull){
                    mConnectedThread.write("enfinger"+4);
                    finger4.setText("....");
                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            if (addbutton){
                                user.child("finger"+4).setValue(4);
                                finger4.setText("ID 4");
                                addbutton = false;
                                finger4IsNull = false;
                                return;
                            }

                            handler.postDelayed(this,100);
                        }
                    };
                    handler.postDelayed(r,100);
                }else {
                    builder.setCancelable(true);
                    builder.setTitle("WARNING!!");
                    builder.setMessage("Apakah Anda Yakin Untuk Menghapus Fingerprint?");
                    builder.setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mConnectedThread.write("delfinger"+4);
                                    Runnable r = new Runnable() {
                                        @Override
                                        public void run() {
                                            if (fingerIsDeleted){
                                                user.child("finger"+4).setValue(0);
                                                finger4.setText("+");
                                                fingerIsDeleted = false;
                                                finger4IsNull = true;
                                                return;
                                            }

                                            handler.postDelayed(this,100);
                                        }
                                    };
                                    handler.postDelayed(r,100);
                                }
                            });
                    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });

                    AlertDialog dialog = builder.create();
                    dialog.show();
                }

            }
        });

        timerJam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTimeDialog();
            }
        });

        cekTegangan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mConnectedThread.write("6");
            }
        });

        dUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                builder.setCancelable(true);
                builder.setTitle("WARNING!!");
                builder.setMessage("Apakah Anda Yakin Untuk Menghapus Akun?");
                builder.setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mConnectedThread.write("11");
                                ignition.setImageResource(R.drawable.enginepending);
                                timer.setImageResource(R.drawable.clockpending);
                                guest.setImageResource(R.drawable.lockpending);
                                imgAki.setImageResource(R.drawable.carbatterypending);
                                btAddress = null;
                                swipeRefreshLayout.setEnabled(false);
                                fingerprintButtons.clear();
                                cekFingerButton();
                                DatabaseReference myRef = database.getReference("Devices/"+key);
                                myRef.removeValue();
                                checkDevices("-");
                                volt.setText("-");
                                waktu.setText("-");
                                mReadBuffer.setText("Akun Dihapus");
                                connect.setText("CONNECT");
                                setInVisible();
                                isNotConnected = true;
                                mHandler.obtainMessage(CONNECTING_STATUS, 2, -1)
                                        .sendToTarget();
                            }
                        });
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();

            }
        });

        ignition.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if(mConnectedThread != null){
                    //First check to make sure thread created
                    ignition.setImageResource(R.drawable.enginepending);
                    setInVisible();
                    if (isIgnitionOff){
                        mConnectedThread.write("0");
//                        isIgnitionOff = false;
                        Log.i("Boolan",String.valueOf(isIgnitionOff));
                    }
                    else {
                        mConnectedThread.write("1");
//                        isIgnitionOff = true;
                        Log.i("Boolan",String.valueOf(isIgnitionOff));
                    }
                }
            }
        });

        timer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mConnectedThread != null){
                    timer.setImageResource(R.drawable.clockpending);
                    setInVisible();
                    if (isTimerOff){
                        mConnectedThread.write("2");
                    }
                    else {
                        mConnectedThread.write("3");

                    }
                }
            }
        });

        guest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mConnectedThread != null){
                    guest.setImageResource(R.drawable.lockpending);
                    setInVisible();
                    if (isGuestOff){
                        mConnectedThread.write("5");
                        timer.setEnabled(false);
                    }
                    else {
                        mConnectedThread.write("4");
                        timer.setEnabled(true);
                    }
                }

            }
        });

    }

    private void showTimeDialog() {

        /**
         * Calendar untuk mendapatkan waktu saat ini
         */
        Calendar calendar = Calendar.getInstance();

        /**
         * Initialize TimePicker Dialog
         */
        timePickerDialog = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                /**
                 * Method ini dipanggil saat kita selesai memilih waktu di DatePicker
                 */

                String waktuskr;
                waktuskr = hourOfDay+":"+minute;
                mConnectedThread.write("hrs"+waktuskr);
                waktu.setText(waktuskr);
            }
        },
                /**
                 * Tampilkan jam saat ini ketika TimePicker pertama kali dibuka
                 */
                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE),

                /**
                 * Cek apakah format waktu menggunakan 24-hour format
                 */
                DateFormat.is24HourFormat(this));

        timePickerDialog.show();
    }

    private void bluetoothOn(){
        if (!mBTAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            mBluetoothStatus.setText("Bluetooth enabled");
            Toast.makeText(getApplicationContext(),"Bluetooth turned on",Toast.LENGTH_SHORT).show();

        }
        else{
            Toast.makeText(getApplicationContext(),"Bluetooth is already on", Toast.LENGTH_SHORT).show();
        }
    }

    private void dialogFormCekImei(){
        dialog = new AlertDialog.Builder(MainActivity.this);
        inflater = getLayoutInflater();
        dialogView = inflater.inflate(R.layout.cekimei_form,null);
        dialog.setView(dialogView);
        dialog.setCancelable(true);
        dialog.setTitle("Cek Imei Perangkat");
        mDevicesTvImei = (TextView) dialogView.findViewById(R.id.tv_imei);
        mDevicesTvImei.setText(imei);

        dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        dialog.show();
    }

    private void dialogFormImei(){
        dialog = new AlertDialog.Builder(MainActivity.this);
        inflater = getLayoutInflater();
        dialogView = inflater.inflate(R.layout.imei_form,null);
        dialog.setView(dialogView);
        dialog.setCancelable(true);
        dialog.setTitle("Tambah Perangkat");
        mDevicesTeImei = (EditText) dialogView.findViewById(R.id.te_imei);
        dialog.setPositiveButton("Add Devices", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                createAkun2(mDevicesTeImei.getText().toString());
            }
        });

        dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        dialog.show();

    }

    private void dialogFormBt(){

        dialog = new AlertDialog.Builder(MainActivity.this);
        inflater = getLayoutInflater();
        dialogView = inflater.inflate(R.layout.bt_form, null);
        dialog.setView(dialogView);
        dialog.setCancelable(true);
        dialog.setTitle("List Bluetooth");
        mDevicesListView    = (ListView) dialogView.findViewById(R.id.devicesListView1);
        mDevicesListView.setAdapter(mBTArrayAdapter); // assign model to view
        mDevicesListView.setOnItemClickListener(mDeviceClickListener);
        discover();

        dialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data) {
        // Check which request we're responding to
        super.onActivityResult(requestCode, resultCode, Data);
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.
                mBluetoothStatus.setText("Enabled");
            } else
                mBluetoothStatus.setText("Disabled");
        }
    }

    private void discover(){

        if(mBTAdapter.isEnabled()) {
            mBTArrayAdapter.clear(); // clear items
            mBTAdapter.startDiscovery();
            Toast.makeText(getApplicationContext(), "Discovery started", Toast.LENGTH_SHORT).show();
            registerReceiver(blReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        }
        else{
            bluetoothOn();
            Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
        }
    }

    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the name to the list
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                mBTArrayAdapter.notifyDataSetChanged();
            }
        }
    };

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

            if(!mBTAdapter.isEnabled()) {
                Toast.makeText(getBaseContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
                return;
            }

//            mBluetoothStatus.setText("Connecting...");
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            final String address = info.substring(info.length() - 17);
            final String name = info.substring(0,info.length() - 17);

            // Spawn a new thread to avoid blocking the GUI one
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //Your code
                    boolean fail = false;

                    BluetoothDevice device = mBTAdapter.getRemoteDevice(address);

                    try {
                        mBTSocket = createBluetoothSocket(device);
                    } catch (IOException e) {
                        fail = true;
                        Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                    }
                    // Establish the Bluetooth socket connection.
                    try {
                        mBTSocket.connect();
                    } catch (IOException e) {
                        try {
                            fail = true;
                            mBTSocket.close();
                            mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                                    .sendToTarget();
                        } catch (IOException e2) {
                            //insert code to deal with this
                            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                    if(fail == false) {
                        mConnectedThread = new MainActivity.ConnectedThread(mBTSocket);
                        mConnectedThread.start();
                        btAddress = info.substring(info.length() - 17);
                        connect.setText("LOGIN...");
                        connect.setEnabled(false);
                        checkDevices(name);
                        mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, name)
                                .sendToTarget();
                    }
                }
            });
        }
    };

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connection with BT device using UUID
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.available();
                    if(bytes != 0) {
                        SystemClock.sleep(100); //pause and wait for rest of data. Adjust this depending on your sending speed.
                        bytes = mmInStream.available(); // how many bytes are ready to be read?
                        bytes = mmInStream.read(buffer, 0, bytes); // record how many bytes we actually read
                        mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget(); // Send the obtained bytes to the UI activity
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private void getImei() {
        String deviceId;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            deviceId = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        } else {
            final TelephonyManager mTelephony = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (this.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {

                }
            }
            assert mTelephony != null;
            if (mTelephony.getDeviceId() != null)
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                {
                    deviceId = mTelephony.getImei();
                }else {
                    deviceId = mTelephony.getDeviceId();
                }
            } else {
                deviceId = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
            }
        }

        imei = deviceId;

    }
}