package com.shailendra.annotateview;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;

public class MediaStorageUtils {

    public static File saveToStorage(Context context, Bitmap bitmap) throws IOException {

        String fileName = Environment.getExternalStorageDirectory().toString().concat("/annotated_image.jpeg");
        File file = new File(fileName);
        if (!file.exists()) {
            file.createNewFile();
        }
        FileOutputStream out = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        out.flush();
        out.close();
        return file;
    }
}
