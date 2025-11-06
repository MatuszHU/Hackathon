package com.team4.hackathon;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.jtransforms.fft.DoubleFFT_1D;

import java.util.ArrayList;
import java.util.List;

import gtec.java.unicorn.Unicorn;

public class Countdown extends AppCompatActivity {
    private double relDelta;
    private double relAlpha;
    private double relBeta;

    private TextView countdownText;
    private static final long CALIBRATION_DURATION_MS = 10_000;
    private List<float[]> testCollection = new ArrayList<>();
    private int count = 10;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnable;
    private Unicorn _unicorn;
    private Thread _receiver;
    private boolean _receiverRunning = false;
    private Context _context = null;
    private boolean isTesting = true;
    double[] baselineAlpha;
    double[] baselineBeta;
    double[] baselineDelta;
    private Intent h,n,s;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_countdown);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        baselineAlpha = getIntent().getDoubleArrayExtra("alpha");
        baselineBeta = getIntent().getDoubleArrayExtra("beta");
        baselineDelta = getIntent().getDoubleArrayExtra("delta");
        countdownText = findViewById(R.id.countdownText);
        h = new Intent(Countdown.this, happy.class);
        n = new Intent(Countdown.this, neatural.class);
        s = new Intent(Countdown.this, sad.class);
        startCountdown();

        StartReceiver();

        }

        private void startCountdown() {
        runnable = new Runnable() {
        @Override
        public void run() {
            countdownText.setText(String.valueOf(count));
            count--;
            if (count >= 0) {
                handler.postDelayed(this, 1000);
            }
            try {
                StopReceiver();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            if(relAlpha > 1.0){
                startActivity(h);
            }
            else if(relDelta > 1.0){
                startActivity(n);
            }
            else{
                startActivity(s);
            }
        }
        };
        handler.post(runnable);

    }
    private void StartReceiver() {
        _receiverRunning = true;
        _receiver = new Thread(() -> {
            while (_receiverRunning) {
                try {
                    float[] data = _unicorn.GetData();

                    if (isTesting) testCollection.add(data);
                } catch (Exception ex) {
                    runOnUiThread(() -> {
                        //Log.e("LogInActivity", "Acquisition failed. " + ex.getMessage());
                        _unicorn = null;
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
            relDelta = delta / baselineDelta[0];
            relAlpha = alpha / baselineAlpha[0];
            relBeta = beta / (baselineBeta[1]/baselineBeta[3]);
            Log.d("EEG_REL", "CH"+ch+" Δrel="+relDelta+" αrel="+relAlpha+" βrel="+relBeta);
        }
    }





}

