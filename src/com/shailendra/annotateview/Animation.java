package com.shailendra.annotateview;

public interface Animation {

    /**
     * Transforms the view.
     * 
     * @param view
     * @param diffTime
     * @return true if this animation should remain active. False otherwise.
     */
    public boolean update(ZoomImageView view, long time);

}
