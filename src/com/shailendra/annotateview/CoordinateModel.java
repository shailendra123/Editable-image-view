package com.shailendra.annotateview;


/**
 * Coordinate Model.
 * 
 */
public class CoordinateModel {

    private float x;
    private float y;

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return "CoordinateModel [x=" + x + ", y=" + y + "]";
    }

}
