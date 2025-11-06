package com.team4.hackathon;

import org.jtransforms.fft.DoubleFFT_1D;
import java.util.List;

public class Calibrate {
    public static double[][] calculateBaselineBands(List<float[]> calibData, double Fs) {
        if (calibData == null || calibData.isEmpty()) return null;
        int channels = calibData.get(0).length, n = calibData.size();
        double[][] eeg = new double[channels][n];
        for (int ch = 0; ch < channels; ch++)
            for (int t = 0; t < n; t++)
                eeg[ch][t] = calibData.get(t)[ch];
        for (int ch = 0; ch < channels; ch++)
            for (int t = 0; t < n; t++)
                eeg[ch][t] *= (0.54 - 0.46 * Math.cos(2 * Math.PI * t / (n - 1)));
        double[][] bands = new double[channels][3]; // Δ, α, β
        for (int ch = 0; ch < channels; ch++) {
            DoubleFFT_1D fft = new DoubleFFT_1D(n);
            fft.realForward(eeg[ch]);
            int dc = 0, ac = 0, bc = 0;
            for (int k = 0; k < n / 2; k++) {
                double re = eeg[ch][2 * k], im = eeg[ch][2 * k + 1];
                double psd = (re * re + im * im) / (n * Fs);
                double f = k * Fs / n;
                if (f >= 0.5 && f <= 4) { bands[ch][0] += psd; dc++; }
                else if (f >= 8 && f <= 13) { bands[ch][1] += psd; ac++; }
                else if (f > 13 && f <= 30) { bands[ch][2] += psd; bc++; }
            }
            if (dc > 0) bands[ch][0] /= dc;
            if (ac > 0) bands[ch][1] /= ac;
            if (bc > 0) bands[ch][2] /= bc;
        }
        return bands;
    }
}
