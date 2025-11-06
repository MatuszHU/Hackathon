package com.team4.hackathon.util;

import android.os.Handler;
import android.widget.TextView;
import gtec.java.unicorn.Unicorn;

import java.util.List;

public class DataReceiver {
    private Thread receiverThread;
    private boolean running;
    private Unicorn unicorn;
    private TextView tvState;
    private boolean isCalibrating, isTesting;
    private List<float[]> calibrationCollection, testCollection;

    public DataReceiver(Unicorn unicorn, TextView tvState, List<float[]> calibrationCollection, List<float[]> testCollection) {
        this.unicorn = unicorn;
        this.tvState = tvState;
        this.calibrationCollection = calibrationCollection;
        this.testCollection = testCollection;
    }

    public void startReceiving() {
        running = true;
        receiverThread = new Thread(() -> {
            while (running) {
                try {
                    float[] data = unicorn.GetData();
                    if (isCalibrating) calibrationCollection.add(data);
                    if (isTesting) testCollection.add(data);
                } catch (Exception ex) {
                    tvState.post(() -> tvState.append("\nAcquisition failed. " + ex.getMessage()));
                    stopReceiving();
                }
            }
        });
        receiverThread.start();
    }

    public void stopReceiving() {
        running = false;
        try {
            receiverThread.join(500);
        } catch (InterruptedException ignored) {}
    }

    public void setCalibrating(boolean state) {
        this.isCalibrating = state;
    }

    public void setTesting(boolean state) {
        this.isTesting = state;
    }
}
