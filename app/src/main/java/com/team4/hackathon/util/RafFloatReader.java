package com.team4.hackathon.util;

import java.io.RandomAccessFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RafFloatReader {

    public static List<float[]> readFloatsFromRaf(String filePath, int floatsPerArray) throws IOException {
        List<float[]> resultList = new ArrayList<>();

        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            long fileSize = raf.length();
            long floatsCount = fileSize / 4; // float = 4 byte

            int arraysCount = (int)Math.ceil((double)floatsCount / floatsPerArray);

            for (int i = 0; i < arraysCount; i++) {
                float[] floatArray = new float[floatsPerArray];

                for (int j = 0; j < floatsPerArray; j++) {
                    long floatIndex = i * floatsPerArray + j;
                    if (floatIndex < floatsCount) {
                        // RandomAccessFile readFloat() olvas 4 byte-ot floatként
                        raf.seek(floatIndex * 4);
                        floatArray[j] = raf.readFloat();
                    } else {
                        // Ha nincs több adat, nullával vagy más értékkel töltheted fel
                        floatArray[j] = 0f;
                    }
                }

                resultList.add(floatArray);
            }
        }

        return resultList;
    }

}
