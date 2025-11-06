package com.team4.hackathon.util;

import android.util.Log;
import org.jtransforms.fft.DoubleFFT_1D;
import android.widget.TextView;

import java.util.List;

public class EEGProcessor {
    private double[] baselineDelta, baselineAlpha, baselineBeta;
    private TextView tvState;

    public EEGProcessor(TextView tvState) {
        this.tvState = tvState;
    }

    public void storeBaselinePSD(List<float[]> calibData) {
        if (calibData == null || calibData.isEmpty()) {
            Log.d("BASE", "Nincs baseline sample!");
            tvState.append("\nNo samples at calibration.");
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
        tvState.append("\nBaseline stored.");
    }

    public void compareToBaseline(List<float[]> testData) {
        if (testData == null || testData.isEmpty() || baselineDelta==null) {
            tvState.append("\nNo baseline or test data!");
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
            double delta=0, alpha=0, beta=0; int dc=0, ac=0, bc=0;
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
            tvState.append("\nCH"+ch+" Δrel="+String.format("%.2f",relDelta)+", αrel="+String.format("%.2f",relAlpha)+", βrel="+String.format("%.2f",relBeta));
        }
    }
}
