package com.shailendra.annotateview;

import java.io.IOException;
import java.util.Iterator;
import java.util.Stack;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class AnnotateImageView extends ZoomImageView {

    private static final String LOG_TAG = AnnotateImageView.class.getName();
    private Stack<CoordinateModel> recentAnotationCordinates;
    private Paint mPaint;
    private GestureDetector mTapDetector;

    private int screenWidth;
    private int screenHeight;

    int halfRectangleWidth;
    int halfRectangleHeight;
    private Context context;

    public AnnotateImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnnotateImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        initCoordinates();
        getScreenDimensions();
        recentAnotationCordinates = new Stack<CoordinateModel>();
        setImageResource(R.drawable.img);
    }

    public AnnotateImageView(Context context) {
        this(context, null, 0);
    }

    private void initCoordinates() {
        mTapDetector = new GestureDetector(getContext(), new TapDetector());
        mPaint = new Paint();
        mPaint.setColor(Color.RED);
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    private void getScreenDimensions() {
        Display display = ((Activity) getContext()).getWindowManager().getDefaultDisplay();
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB_MR2) {
            screenWidth = display.getWidth();
            screenHeight = display.getHeight();
        } else {
            Point size = new Point();
            display.getSize(size);
            screenWidth = size.x;
            screenHeight = size.y;
        }

        // set Rectangle dimensions
        halfRectangleWidth = Math.min(screenWidth, screenHeight) / 50;
        halfRectangleHeight = halfRectangleWidth * 4 / 5;

        // reduce footer height
        screenHeight = (int) (screenHeight - getResources().getDimension(R.dimen.footer_height));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (mTapDetector.onTouchEvent(event)) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void drawView(Canvas canvas) {
        super.drawView(canvas);

        // Draw coordinates.
        Iterator<CoordinateModel> iterator = recentAnotationCordinates.iterator();
        while (iterator.hasNext()) {
            CoordinateModel cordinate = iterator.next();
            canvas.drawRect(cordinate.getX() - halfRectangleWidth, cordinate.getY() - halfRectangleHeight,
                    cordinate.getX() + halfRectangleWidth, cordinate.getY() + halfRectangleHeight, mPaint);
        }
    }

    public void reset() {
        super.reset();
        invalidate();
        if (gestureImageViewTouchListener != null) {
            gestureImageViewTouchListener.handleUp();
        }
    }

    /**
     * User click detector
     */
    private class TapDetector extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            Log.d(LOG_TAG, event.getX() + " " + event.getY());
            float cordinateX = (event.getX() - x) / scaleAdjust;
            float cordinateY = (event.getY() - y) / scaleAdjust;

            final float left = cordinateX - halfRectangleWidth;
            final float top = cordinateY - halfRectangleHeight;
            final float right = cordinateX + halfRectangleWidth;
            final float bottom = cordinateY + halfRectangleHeight;

            // Draw only when coordinate is within image bounds.
            Rect imageBounds = drawable.getBounds();
            if (left >= imageBounds.left && top >= imageBounds.top && right <= imageBounds.right
                    && bottom <= imageBounds.bottom) {
                // Draw point
                CoordinateModel cordinate = new CoordinateModel();
                cordinate.setX(cordinateX);
                cordinate.setY(cordinateY);
                recentAnotationCordinates.push(cordinate);
                invalidate();

            }
            return super.onSingleTapUp(event);
        }
    }

    public void clear() {
        recentAnotationCordinates.clear();
        reset();
    }

    public void undo() {
        if (!recentAnotationCordinates.isEmpty()) {
            recentAnotationCordinates.pop();
            invalidate();
        }
    }

    /**
     * Save/Replace annotated diagram into memory.
     * 
     * @return true if successfully saved, false otherwise
     * @throws IOException
     */
    public void save() throws IOException {
        reset();
        invalidate();
        gestureImageViewTouchListener.handleUp();
        setDrawingCacheEnabled(true);
        buildDrawingCache(true);
        Bitmap bitmap = getDrawingCache();
        if (bitmap != null) {

            Rect imageBounds = drawable.getBounds();

            int bitmapWidth = imageBounds.right - imageBounds.left;
            int bitmapHeight = imageBounds.bottom - imageBounds.top;

            bitmapHeight = bitmapHeight * screenWidth / bitmapWidth;
            if (bitmapHeight > screenHeight) {
                bitmapWidth = bitmapWidth * screenHeight / bitmapHeight;
                bitmapHeight = screenHeight;
            }
            try {
                bitmap = Bitmap.createBitmap(bitmap, 0, (screenHeight - bitmapHeight) / 2, screenWidth, bitmapHeight);
            } catch (Exception exception) {
                Log.e(LOG_TAG, "Exception while resizing bitmap " + exception);
            }
            MediaStorageUtils.saveToStorage(context, bitmap);
        }
        setDrawingCacheEnabled(false);
    }

}
