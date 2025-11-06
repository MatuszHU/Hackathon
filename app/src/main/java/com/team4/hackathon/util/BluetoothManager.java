package com.team4.hackathon.util;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.List;

import gtec.java.unicorn.Unicorn;

public class BluetoothManager {
        private Context context;
        private Spinner spnDevices;
        private Unicorn unicorn;

        public BluetoothManager(Context context, Spinner spnDevices) {
            this.context = context;
            this.spnDevices = spnDevices;
        }

        public void populateDevices() {
            try {
                List<String> devices = Unicorn.GetAvailableDevices();
                ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, devices);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spnDevices.setAdapter(adapter);
            } catch (Exception ex) {
                Toast.makeText(context, "Device error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        public Unicorn connect(String device) throws Exception {
            unicorn = new Unicorn(device);
            unicorn.StartAcquisition();
            return unicorn;
        }

        public void disconnect() {
            if (unicorn != null) {
                try {
                    unicorn.StopAcquisition();
                } catch (Exception e) {
                    // Log or handle error
                }
                unicorn = null;
            }
        }
    }

