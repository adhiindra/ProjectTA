package com.example.projectta;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.zxing.Result;

public class qrCodeActivity extends AppCompatActivity {


    private CodeScanner mScannerView;
    private String key;
    private Context ctx;
    FirebaseDatabase database = FirebaseDatabase.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scanner_activity);
        ctx = this;
        key = getIntent().getStringExtra("key");
        startScanning();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mScannerView != null) {
            mScannerView.startPreview();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mScannerView != null) {
            mScannerView.releaseResources();
        }
        super.onPause();
    }

    private void createAkun2(String imei2){
        if(imei2!=null){
            DatabaseReference databaseakun = database.getInstance().getReference("Devices/"+key);
            databaseakun.child("Imei2").setValue(imei2);
        }
    }

    private void startScanning() {
        CodeScannerView scannerView = findViewById(R.id.scanner_view);
        mScannerView = new CodeScanner(this, scannerView);
        mScannerView.setDecodeCallback(new DecodeCallback() {
            @Override
            public void onDecoded(@NonNull final Result result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(qrCodeActivity.this, result.getText(), Toast.LENGTH_SHORT).show();
                        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                        builder.setTitle("Scan Result");
                        builder.setMessage(result.getText());
                        builder.setPositiveButton("Simpan", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                createAkun2(result.getText());
                                Toast.makeText(qrCodeActivity.this, "Imei "+ result.getText() + " Berhasil Tersimpan", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        });
                        builder.setNegativeButton("Batal", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        });
                        builder.show();
                    }
                });
            }
        });
        scannerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mScannerView.startPreview();
            }
        });
    }
}
