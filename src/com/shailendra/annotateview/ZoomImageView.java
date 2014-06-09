package com.shailendra.annotateview;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

/**
 * ImageView with Zoom/Pinch functionality.
 */
public class ZoomImageView extends ImageView {

    public static final String GLOBAL_NS = "http://schemas.android.com/apk/res/android";
    public static final String LOCAL_NS = "http://schemas.annotateview.com/android";
    private static final String LOG_TAG = ZoomImageView.class.getName();
    // private static final float SCALE = 1.0f;

    private final Semaphore drawLock = new Semaphore(0);
    private Animator animator;

    protected Drawable drawable;

    protected float x = 0;
    protected float y = 0;

    protected boolean layout = false;

    protected float scaleAdjust = 1.0f;
    private float startingScale = -1.0f;

    private float maxScale = 5.0f;
    private float minScale = 0.75f;
    private float fitScaleHorizontal = 1.0f;
    private float fitScaleVertical = 1.0f;
    private float rotation = 0.0f;

    protected float centerX;
    protected float centerY;

    private Float startX;
    private Float startY;

    protected int hWidth;
    protected int hHeight;

    protected int resId = -1;
    private boolean recycleReady = false;
    private boolean strict = false;

    protected int bitmapWidth = -1;
    protected int bitmapHeight;

    private int alpha = 255;
    private ColorFilter colorFilter;

    private int deviceOrientation = -1;
    private int imageOrientation;

    private GestureImageViewListener gestureImageViewListener;
    protected GestureImageViewTouchListener gestureImageViewTouchListener;
    private OnClickListener onClickListener;
    protected int originalDrawableWidth;
    protected int originalDrawableHeight;

    public ZoomImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZoomImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        String scaleType = attrs.getAttributeValue(GLOBAL_NS, "scaleType");

        if (isNullOrEmpty(scaleType)) {
            setScaleType(ScaleType.CENTER_INSIDE);
        }
        String strStartX = attrs.getAttributeValue(LOCAL_NS, "start-x");
        String strStartY = attrs.getAttributeValue(LOCAL_NS, "start-y");
        if (!isNullOrEmpty(strStartX)) {
            startX = Float.parseFloat(strStartX);
        }
        if (!isNullOrEmpty(strStartY)) {
            startY = Float.parseFloat(strStartY);
        }

        setStartingScale(attrs.getAttributeFloatValue(LOCAL_NS, "start-scale", startingScale));
        setMinScale(attrs.getAttributeFloatValue(LOCAL_NS, "min-scale", minScale));
        setMaxScale(attrs.getAttributeFloatValue(LOCAL_NS, "max-scale", maxScale));
        setStrict(attrs.getAttributeBooleanValue(LOCAL_NS, "strict", strict));
        setRecycle(attrs.getAttributeBooleanValue(LOCAL_NS, "recycle", recycleReady));

        initImage();
    }

    public ZoomImageView(Context context) {
        super(context);
        setScaleType(ScaleType.CENTER_INSIDE);
        initImage();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        if (drawable != null) {
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                bitmapHeight = MeasureSpec.getSize(heightMeasureSpec);

                if (getLayoutParams().width == LayoutParams.WRAP_CONTENT) {
                    float ratio = (float) getImageWidth() / (float) getImageHeight();
                    bitmapWidth = Math.round((float) bitmapHeight * ratio);
                } else {
                    bitmapWidth = MeasureSpec.getSize(widthMeasureSpec);
                }
            } else {
                bitmapWidth = MeasureSpec.getSize(widthMeasureSpec);
                if (getLayoutParams().height == LayoutParams.WRAP_CONTENT) {
                    float ratio = (float) getImageHeight() / (float) getImageWidth();
                    bitmapHeight = Math.round((float) bitmapWidth * ratio);
                } else {
                    bitmapHeight = MeasureSpec.getSize(heightMeasureSpec);
                }
            }
        } else {
            bitmapHeight = MeasureSpec.getSize(heightMeasureSpec);
            bitmapWidth = MeasureSpec.getSize(widthMeasureSpec);
        }

        setMeasuredDimension(bitmapWidth, bitmapHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed || !layout) {
            setupCanvas(bitmapWidth, bitmapHeight, getResources().getConfiguration().orientation);
        }
    }

    protected void setupCanvas(int measuredWidth, int measuredHeight, int orientation) {

        if (deviceOrientation != orientation) {
            layout = false;
            deviceOrientation = orientation;
        }

        if (drawable != null && !layout) {
            int imageWidth = getImageWidth();
            int imageHeight = getImageHeight();

            hWidth = Math.round(((float) imageWidth / 2.0f));
            hHeight = Math.round(((float) imageHeight / 2.0f));

            int measuredWidthWithoutPadding = measuredWidth - (getPaddingLeft() + getPaddingRight());
            int measuredHeightWithutPadding = measuredHeight - (getPaddingTop() + getPaddingBottom());

            computeCropScale(imageWidth, imageHeight, measuredWidthWithoutPadding, measuredHeightWithutPadding);

            if (startingScale <= 0.0f) {
                computeStartingScale(imageWidth, imageHeight, measuredWidthWithoutPadding, measuredHeightWithutPadding);
            }

            scaleAdjust = startingScale;

            this.centerX = (float) measuredWidthWithoutPadding / 2.0f;
            this.centerY = (float) measuredHeightWithutPadding / 2.0f;

            if (startX == null) {
                x = centerX;
            } else {
                x = startX;
            }

            if (startY == null) {
                y = centerY;
            } else {
                y = startY;
            }

            gestureImageViewTouchListener = new GestureImageViewTouchListener(this, measuredWidth, measuredHeight);

            if (isLandscape()) {
                gestureImageViewTouchListener.setMinScale(minScale * fitScaleHorizontal);
            } else {
                gestureImageViewTouchListener.setMinScale(minScale * fitScaleVertical);
            }

            gestureImageViewTouchListener.setMaxScale(maxScale * startingScale);

            gestureImageViewTouchListener.setFitScaleHorizontal(fitScaleHorizontal);
            gestureImageViewTouchListener.setFitScaleVertical(fitScaleVertical);
            gestureImageViewTouchListener.setOnClickListener(onClickListener);

            setImageBounds();
            gestureImageViewTouchListener.handleUp();

            layout = true;
        }
    }

    protected void setImageBounds() {
        drawable.setBounds(-hWidth, -hHeight, hWidth, hHeight);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureImageViewTouchListener.onTouch(this, event);
    }

    protected void computeCropScale(int imageWidth, int imageHeight, int measuredWidth, int measuredHeight) {
        fitScaleHorizontal = (float) measuredWidth / (float) imageWidth;
        fitScaleVertical = (float) measuredHeight / (float) imageHeight;
    }

    protected void computeStartingScale(int imageWidth, int imageHeight, int measuredWidth, int measuredHeight) {
        switch (getScaleType()) {
        case CENTER:
            // Center the image in the view, but perform no scaling.
            startingScale = 1.0f;
            break;

        case CENTER_CROP:
            // Scale the image uniformly (maintain the image's aspect ratio) so that both dimensions
            // (width and height) of the image will be equal to or larger than the corresponding dimension of the view
            // (minus padding).
            startingScale =
                    Math.max((float) measuredHeight / (float) imageHeight, (float) measuredWidth / (float) imageWidth);
            break;

        case CENTER_INSIDE:

            // Scale the image uniformly (maintain the image's aspect ratio) so that both dimensions
            // (width and height) of the image will be equal to or less than the corresponding dimension of the view
            // (minus padding).
            float wRatio = (float) imageWidth / (float) measuredWidth;
            float hRatio = (float) imageHeight / (float) measuredHeight;

            if (wRatio > hRatio) {
                startingScale = fitScaleHorizontal;
            } else {
                startingScale = fitScaleVertical;
            }

            break;
        default:
            break;
        }
    }

    protected boolean isRecycled() {
        boolean isRecycled = false;
        if (drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            if (bitmap != null) {
                isRecycled = bitmap.isRecycled();
            }
        }
        return isRecycled;
    }

    public void recycle() {
        if (recycleReady && drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (layout) {
            if (drawable != null && !isRecycled()) {
                canvas.save();

                drawView(canvas);

                canvas.restore();
            }

            if (drawLock.availablePermits() <= 0) {
                drawLock.release();
            }
        }
    }

    protected void drawView(Canvas canvas) {

        canvas.translate(x, y);

        if (rotation != 0.0f) {
            canvas.rotate(rotation);
        }

        if (scaleAdjust != 1.0f) {
            canvas.scale(scaleAdjust, scaleAdjust);
        }

        drawable.draw(canvas);

    }

    /**
     * Waits for a draw
     * 
     * @param max
     *            time to wait for draw (ms)
     * @throws InterruptedException
     */
    public boolean waitForDraw(long timeout) throws InterruptedException {
        return drawLock.tryAcquire(timeout, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void onAttachedToWindow() {
        animator = new Animator(this, "GestureImageViewAnimator");
        animator.start();

        if (resId >= 0 && drawable == null) {
            setImageResource(resId);
        }

        super.onAttachedToWindow();
    }

    public void animationStart(Animation animation) {
        if (animator != null) {
            animator.play(animation);
        }
    }

    public void animationStop() {
        if (animator != null) {
            animator.cancel();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) {
            animator.finish();
        }
        if (recycleReady && drawable != null && !isRecycled()) {
            recycle();
            drawable = null;
        }
        super.onDetachedFromWindow();
    }

    private final void initImage() {
        if (this.drawable != null) {
            this.drawable.setAlpha(alpha);
            this.drawable.setFilterBitmap(true);
            if (colorFilter != null) {
                this.drawable.setColorFilter(colorFilter);
            }
        }

        if (!layout) {
            requestLayout();
            redraw();
        }
    }

    public void setImageBitmap(Bitmap image) {
        this.drawable = new BitmapDrawable(getResources(), image);
        initImage();
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        this.drawable = drawable;
        originalDrawableWidth = getImageWidth();
        originalDrawableHeight = getImageHeight();
        initImage();
    }

    public void setImageResource(int id) {
        if (this.drawable != null) {
            this.recycle();
        }
        if (id >= 0) {
            this.resId = id;
            setImageDrawable(new BitmapDrawable(getResources(), BitmapFactory.decodeResource(getResources(), id)));
        }
    }

    public int getScaledWidth() {
        return Math.round(getImageWidth() * getScale());
    }

    public int getScaledHeight() {
        return Math.round(getImageHeight() * getScale());
    }

    public int getImageWidth() {
        int imageWidth = 0;
        if (drawable != null) {
            imageWidth = drawable.getIntrinsicWidth();
        }
        return imageWidth;
    }

    public int getImageHeight() {
        int imageHeight = 0;
        if (drawable != null) {
            imageHeight = drawable.getIntrinsicHeight();
        }
        return imageHeight;
    }

    public void moveBy(float x, float y) {
        this.x += x;
        this.y += y;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public final void redraw() {
        postInvalidate();
    }

    public final void setMinScale(float min) {
        this.minScale = min;
        if (gestureImageViewTouchListener != null) {
            gestureImageViewTouchListener.setMinScale(min * fitScaleHorizontal);
        }
    }

    public final void setMaxScale(float max) {
        this.maxScale = max;
        if (gestureImageViewTouchListener != null) {
            gestureImageViewTouchListener.setMaxScale(max * startingScale);
        }
    }

    public void setScale(float scale) {
        scaleAdjust = scale;
    }

    public float getScale() {
        return scaleAdjust;
    }

    public float getImageX() {
        return x;
    }

    public float getImageY() {
        return y;
    }

    public boolean isStrict() {
        return strict;
    }

    public final void setStrict(boolean strict) {
        this.strict = strict;
    }

    public boolean isRecycle() {
        return recycleReady;
    }

    public final void setRecycle(boolean recycle) {
        this.recycleReady = recycle;
    }

    public void reset() {
        x = centerX;
        y = centerY;
        scaleAdjust = 1.0f;
        if (gestureImageViewTouchListener != null) {
            gestureImageViewTouchListener.reset();
        }
        redraw();
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    public void setGestureImageViewListener(GestureImageViewListener pinchImageViewListener) {
        this.gestureImageViewListener = pinchImageViewListener;
    }

    public GestureImageViewListener getGestureImageViewListener() {
        return gestureImageViewListener;
    }

    @Override
    public Drawable getDrawable() {
        return drawable;
    }

    @Override
    public void setAlpha(int alpha) {
        this.alpha = alpha;
        if (drawable != null) {
            drawable.setAlpha(alpha);
        }
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        this.colorFilter = cf;
        if (drawable != null) {
            drawable.setColorFilter(cf);
        }
    }

    @Override
    public void setImageURI(Uri mUri) {
        if ("content".equals(mUri.getScheme())) {
            String[] orientationColumn = { MediaStore.Images.Media.ORIENTATION };

            Cursor cursor = getContext().getContentResolver().query(mUri, orientationColumn, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                imageOrientation = cursor.getInt(cursor.getColumnIndex(orientationColumn[0]));
            }

            InputStream in = null;

            try {
                in = getContext().getContentResolver().openInputStream(mUri);
                Bitmap bmp = BitmapFactory.decodeStream(in);

                if (imageOrientation != 0) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(imageOrientation);
                    Bitmap rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
                    bmp.recycle();
                    setImageDrawable(new BitmapDrawable(getResources(), rotated));
                } else {
                    setImageDrawable(new BitmapDrawable(getResources(), bmp));
                }
            } catch (FileNotFoundException fileNotFoundException) {
                Log.e(LOG_TAG, "error while reading image uri " + fileNotFoundException);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ioException) {
                        Log.e(LOG_TAG, "error whille closing stream " + ioException);
                    }
                }

                if (cursor != null) {
                    cursor.close();
                }
            }

        } else {
            setImageDrawable(Drawable.createFromPath(mUri.toString()));
        }

        if (drawable == null) {
            Log.e("GestureImageView", "resolveUri failed on bad bitmap uri: " + mUri);
            // Don't try again.
        }
    }

    @Override
    public Matrix getImageMatrix() {
        if (strict) {
            throw new UnsupportedOperationException("NOT_SUPPORTED");
        }
        return super.getImageMatrix();
    }

    public final void setScaleType(ScaleType scaleType) {
        if (scaleType == ScaleType.CENTER || scaleType == ScaleType.CENTER_CROP || scaleType == ScaleType.CENTER_INSIDE) {

            super.setScaleType(scaleType);
        } else if (strict) {
            throw new UnsupportedOperationException("NOT_SUPPORTED");
        }
    }

    @Override
    public void invalidateDrawable(Drawable drawable) {
        if (strict) {
            throw new UnsupportedOperationException("NOT_SUPPORTED");
        }
        super.invalidateDrawable(drawable);
    }

    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        if (strict) {
            throw new UnsupportedOperationException("NOT_SUPPORTED");
        }
        return super.onCreateDrawableState(extraSpace);
    }

    @Override
    public void setAdjustViewBounds(boolean adjustViewBounds) {
        if (strict) {
            throw new UnsupportedOperationException("NOT_SUPPORTED");
        }
        super.setAdjustViewBounds(adjustViewBounds);
    }

    @Override
    public void setImageLevel(int level) {
        if (strict) {
            throw new UnsupportedOperationException("NOT_SUPPORTED");
        }
        super.setImageLevel(level);
    }

    public final void setImageMatrix(Matrix matrix) {
        if (strict) {
            throw new UnsupportedOperationException("NOT_SUPPORTED");
        }
    }

    public final void setImageState(int[] state, boolean merge) {
        if (strict) {
            throw new UnsupportedOperationException("NOT_SUPPORTED");
        }
    }

    public final void setSelected(boolean selected) {
        if (strict) {
            throw new UnsupportedOperationException("NOT_SUPPORTED");
        }
        super.setSelected(selected);
    }

    public final float getCenterX() {
        return centerX;
    }

    public float getCenterY() {
        return centerY;
    }

    public boolean isLandscape() {
        return getImageWidth() >= getImageHeight();
    }

    public boolean isPortrait() {
        return getImageWidth() <= getImageHeight();
    }

    public final void setStartingScale(float startingScale) {
        this.startingScale = startingScale;
    }

    public final void setStartingPosition(float x, float y) {
        this.startX = x;
        this.startY = y;
    }

    @Override
    public void setOnClickListener(OnClickListener clickListener) {
        this.onClickListener = clickListener;

        if (gestureImageViewTouchListener != null) {
            gestureImageViewTouchListener.setOnClickListener(clickListener);
        }
    }

    /**
     * Returns true if the image dimensions are aligned with the orientation of the device.
     * 
     * @return
     */
    public boolean isOrientationAligned() {
        boolean orientationAligned = true;
        if (deviceOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            orientationAligned = isLandscape();
        } else if (deviceOrientation == Configuration.ORIENTATION_PORTRAIT) {
            orientationAligned = isPortrait();
        }
        return orientationAligned;
    }

    public int getDeviceOrientation() {
        return deviceOrientation;
    }

    private static boolean isNullOrEmpty(String str) {
        return (str == null || str.trim().length() == 0);
    }
}
