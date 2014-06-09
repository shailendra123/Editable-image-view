package com.shailendra.annotateview;

import java.io.IOException;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

public class AnnotateDiagramActivity extends Activity {

    private static final String LOG_TAG = AnnotateDiagramActivity.class.getSimpleName();

    // Views
    private AnnotateImageView annotateImageView;
    private Button btnClear, btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.annotate_diagram);

        initUI();
    }

    private void initUI() {

        btnClear = (Button) findViewById(R.id.btn_clear);
        btnClear.setOnClickListener(mViewClickListener);

        btnSave = (Button) findViewById(R.id.btn_save);
        btnSave.setOnClickListener(mViewClickListener);

        annotateImageView = (AnnotateImageView) findViewById(R.id.image_editor_view);
        final int orientation =
                annotateImageView.getImageWidth() > annotateImageView.getImageHeight()
                        ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

        setRequestedOrientation(orientation);
    }

    private final OnClickListener mViewClickListener = new OnClickListener() {

        @Override
        public void onClick(View view) {
            switch (view.getId()) {

            case R.id.btn_clear:
                annotateImageView.clear();
                break;

            case R.id.btn_save:
                try {
                    annotateImageView.save();
                } catch (IOException iOException) {
                    Log.e(LOG_TAG, "Exception while saving file" + " : " + iOException);
                }
                break;

            default:
                break;
            }

        }
    };
}
