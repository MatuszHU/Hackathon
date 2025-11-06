package com.team4.hackathon;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

public class StayStill extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_stay_still);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        double[] baselineAlpha = getIntent().getDoubleArrayExtra("alpha");
        double[] baselineBeta = getIntent().getDoubleArrayExtra("beta");
        double[] baselineDelta = getIntent().getDoubleArrayExtra("delta");

        Intent intent = new Intent(StayStill.this, Countdown.class);
        ImageView btnPlay = findViewById(R.id.ic_play);
        intent.putExtra("alpha1", baselineAlpha);
        intent.putExtra("beta1", baselineBeta);
        intent.putExtra("delta1", baselineDelta);
        btnPlay.setOnClickListener(v -> startActivity(intent));

    }
}