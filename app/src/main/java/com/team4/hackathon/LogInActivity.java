package com.team4.hackathon;

import android.Manifest;
import android.content.Context;
import org.jtransforms.fft.DoubleFFT_1D;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import gtec.java.unicorn.Unicorn;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class LogInActivity extends AppCompatActivity implements View.OnClickListener {

    private List<float[]> calibrationCollection = new ArrayList<>();
    private List<float[]> testCollection = new ArrayList<>();
    private boolean isCalibrating = false, isTesting = false;
    private static final long CALIBRATION_DURATION_MS = 30_000;

    private String _btnConStr = "Connect", _btnDisconStr = "Disconnect";
    private Button _btnConnect, _btnCalibrate, _btnTest;
    private Spinner _spnDevices;
    private Unicorn _unicorn;
    private Thread _receiver;
    private boolean _receiverRunning = false;
    private Context _context = null;

    private RandomAccessFile raf;

    private double[] baselineDelta, baselineAlpha, baselineBeta;

    private static final int PermissionRequestCode = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        File file = new File(getFilesDir(), "baseline.txt");

        try {
            raf = new RandomAccessFile(file,"rw");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        Intent answerNo = new Intent(LogInActivity.this, StayStill.class);

        Button btnNo = findViewById(R.id.btnNo);
        _btnCalibrate = findViewById(R.id.btnCali);

        _btnCalibrate.setOnClickListener(v -> {
            startCalibration();
            try {
                raf.setLength(0);
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {


                            for (int i = 0; i < calibrationCollection.size(); i++) {
                                for (int j = 0; j < calibrationCollection.get(i).length; j++) {
                                    try {
                                        Log.d("calib", calibrationCollection.get(i)[j]+"");
                                        raf.writeBytes(calibrationCollection.get(i)[j] + ",");
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            }
                    }
                }, 30100);
        } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        btnNo.setOnClickListener(v -> {
            answerNo.putExtra("alpha", baselineAlpha);
            answerNo.putExtra("beta", baselineBeta);
            answerNo.putExtra("delta", baselineDelta);
            startActivity(answerNo);

        });

        try {
            _context = this.getApplicationContext();
            _spnDevices = findViewById(R.id.spnDevices);
            _btnConnect = findViewById(R.id.btnConnect);
            _btnCalibrate = findViewById(R.id.btnCalibrate);
            _btnTest = findViewById(R.id.btnTest);

            _btnConnect.setText(_btnConStr);
            _btnConnect.setOnClickListener(this);
            _btnCalibrate.setOnClickListener(this);
            _btnTest.setOnClickListener(this);

        } catch (Exception ex) {
            Toast.makeText(_context, "UI error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("LogInActivity", "UI error: " + ex.getMessage());
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Toast.makeText(this, "Bluetooth not supported.", Toast.LENGTH_SHORT).show();
            Log.e("LogInActivity", "Bluetooth not supported.");
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissions(new String[]{
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_ADVERTISE,
                        Manifest.permission.BLUETOOTH_SCAN}, PermissionRequestCode);
            } else {
                GetAvailableDevices();
            }
        } catch (Exception ex) {
            Toast.makeText(_context, "Permission error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("LogInActivity", "Permission error: " + ex.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PermissionRequestCode && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            GetAvailableDevices();
        }
    }

    private void GetAvailableDevices() {
        try {
            List<String> devices = Unicorn.GetAvailableDevices();
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, devices);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            _spnDevices.setAdapter(adapter);
        } catch (Exception ex) {
            Toast.makeText(_context, "Device error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("LogInActivity", "Device error: " + ex.getMessage());
        }
    }

    private void StartReceiver() {
        _receiverRunning = true;
        _receiver = new Thread(() -> {
            while (_receiverRunning) {
                try {
                    float[] data = _unicorn.GetData();
                    if (isCalibrating) calibrationCollection.add(data);
                    if (isTesting) testCollection.add(data);
                } catch (Exception ex) {
                    runOnUiThread(() -> {
                        Log.e("LogInActivity", "Acquisition failed. " + ex.getMessage());
                        Disconnect();
                    });
                }
            }
        });
        _receiver.setPriority(Thread.MIN_PRIORITY);
        _receiver.setDaemon(false);
        _receiver.start();
    }

    private void StopReceiver() throws Exception {
        _receiverRunning = false;
        _receiver.join(500);
    }

    private void startCalibration() {
        calibrationCollection.clear();
        isCalibrating = true;
        Log.d("LogInActivity", "Calibration started...");
        if (!_receiverRunning) StartReceiver();
        new Handler(getMainLooper()).postDelayed(() -> {
            isCalibrating = false;
            Log.d("LogInActivity", "Calibration finished. Samples: " + calibrationCollection.size());
            storeBaselinePSD(calibrationCollection);
        }, CALIBRATION_DURATION_MS);
    }

    private void startTest() {
        testCollection.clear();
        isTesting = true;
        Log.d("LogInActivity", "Test started...");
        new Handler(getMainLooper()).postDelayed(() -> {
            isTesting = false;
            Log.d("LogInActivity", "Test finished. Samples: " + testCollection.size());
            compareToBaseline(testCollection);
        }, CALIBRATION_DURATION_MS);
    }

    private void storeBaselinePSD(List<float[]> calibData) {
        if (calibData == null || calibData.isEmpty()) {
            Log.d("BASE", "Nincs baseline sample!");
            return;
        }
        int channels = calibData.get(0).length;
        int n = calibData.size();
        double Fs = 256.0;
        double[][] eeg = new double[channels][n];
        for (int ch = 0; ch < channels; ch++)
            for (int t = 0; t < n; t++) eeg[ch][t] = calibData.get(t)[ch];
        for (int ch = 0; ch < channels; ch++)
            for (int t = 0; t < n; t++)
                eeg[ch][t] *= (0.54 - 0.46 * Math.cos(2*Math.PI*t/(n-1)));
        double[] delta = new double[channels], alpha = new double[channels], beta = new double[channels];
        for (int ch = 0; ch < channels; ch++) {
            DoubleFFT_1D fft = new DoubleFFT_1D(n);
            fft.realForward(eeg[ch]);
            int dc=0, ac=0, bc=0;
            for (int k=0; k<n/2; k++) {
                double re = eeg[ch][2*k], im = eeg[ch][2*k+1];
                double psd = (re*re+im*im)/(n*Fs);
                double f = k*Fs/n;
                if (f>=0.5 && f<=4) {delta[ch] += psd; dc++;}
                else if (f>=8 && f<=13) {alpha[ch] += psd; ac++;}
                else if (f>13 && f<=30) {beta[ch] += psd; bc++;}
            }
            if (dc>0) delta[ch] /= dc;
            if (ac>0) alpha[ch] /= ac;
            if (bc>0) beta[ch] /= bc;
            Log.d("BASE", "CH"+ch+" Δ="+delta[ch]+", α="+alpha[ch]+", β="+beta[ch]);
        }
        baselineDelta = delta; baselineAlpha = alpha; baselineBeta = beta;
        Log.d("CHECK", "Baseline calculated: delta[0]=" + baselineDelta[0]);
        Log.d("LogInActivity", "Baseline stored.");
    }

    private void compareToBaseline(List<float[]> testData) {
        if (testData == null || testData.isEmpty() || baselineDelta==null) {
            Log.d("LogInActivity", "No baseline or test data!");
            return;
        }
        int channels = testData.get(0).length, n = testData.size();
        double Fs = 256.0;
        double[][] eeg = new double[channels][n];
        for (int ch = 0; ch < channels; ch++)
            for (int t = 0; t < n; t++) eeg[ch][t] = testData.get(t)[ch];
        for (int ch = 0; ch < channels; ch++)
            for (int t = 0; t < n; t++)
                eeg[ch][t] *= (0.54 - 0.46 * Math.cos(2*Math.PI*t/(n-1)));
        for (int ch = 0; ch < channels; ch++) {
            DoubleFFT_1D fft = new DoubleFFT_1D(n);
            fft.realForward(eeg[ch]);
            double delta=0, alpha=0, beta=0; int dc=0,ac=0,bc=0;
            for (int k=0; k<n/2; k++) {
                double re=eeg[ch][2*k], im=eeg[ch][2*k+1];
                double psd = (re*re+im*im)/(n*Fs);
                double f = k*Fs/n;
                if (f>=0.5&&f<=4){delta+=psd;dc++;}
                else if(f>=8&&f<=13){alpha+=psd;ac++;}
                else if(f>13&&f<=30){beta+=psd;bc++;}
            }
            if(dc>0) delta/=dc;
            if(ac>0) alpha/=ac;
            if(bc>0) beta/=bc;
            double relDelta = delta / baselineDelta[ch];
            double relAlpha = alpha / baselineAlpha[ch];
            double relBeta = beta / baselineBeta[ch];
            Log.d("EEG_REL", "CH"+ch+" Δrel="+relDelta+" αrel="+relAlpha+" βrel="+relBeta);
        }
    }

    private void Connect() {
        _btnConnect.setEnabled(false);
        _spnDevices.setEnabled(false);
        String device = (String)_spnDevices.getSelectedItem();
        try {
            Log.d("LogInActivity", "Connecting to " + device +"...");
            _unicorn = new Unicorn(device);
            _btnConnect.setText(_btnDisconStr);
            _unicorn.StartAcquisition();
            Log.d("LogInActivity", "Connected.\nAcquisition running.");
            StartReceiver();
        } catch (Exception ex) {
            _unicorn = null;
            System.gc();
            System.runFinalization();
            _btnConnect.setText(_btnConStr);
            _spnDevices.setEnabled(true);
            Log.e("LogInActivity", "Connect failed: "+ ex.getMessage());
        }
        _btnConnect.setEnabled(true);
    }

    private void Disconnect() {
        _btnConnect.setEnabled(false);
        try {
            _receiverRunning = false;
            try { StopReceiver(); } catch(Exception e){ Log.e("Disconnect","StopReceiver fail",e);}
            if (_unicorn != null) {
                try { _unicorn.StopAcquisition(); } catch(Exception e){ Log.e("Disconnect","StopAcq fail",e);}
                _unicorn = null;
            }
            Log.d("LogInActivity", "Disconnected");
        } catch (Exception ex) {
            _unicorn = null;
            System.gc(); System.runFinalization();
            _btnConnect.setText(_btnConStr);
            Log.e("LogInActivity", "Disconnect error: "+ ex.getMessage());
        }
        _spnDevices.setEnabled(true);
        _btnConnect.setEnabled(true);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnConnect) {
            if (_btnConnect.getText().equals(_btnConStr)) Connect(); else Disconnect();
        }
        if (v.getId() == R.id.btnCalibrate) startCalibration();
        if (v.getId() == R.id.btnTest) startTest();
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }
}
