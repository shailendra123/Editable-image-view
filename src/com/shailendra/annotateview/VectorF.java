package com.shailendra.annotateview;

import android.graphics.PointF;
import android.view.MotionEvent;

public class VectorF {

    private float angle;
    private double length;

    private final PointF start = new PointF();
    private final PointF end = new PointF();

    public void calculateEndPoint() {
        end.x = (float) (Math.cos(angle) * length + start.x);
        end.y = (float) (Math.sin(angle) * length + start.y);
    }

    public void setStart(PointF p) {
        this.start.x = p.x;
        this.start.y = p.y;
    }

    public void setEnd(PointF p) {
        this.end.x = p.x;
        this.end.y = p.y;
    }

    public void set(MotionEvent event) {
        this.start.x = event.getX(0);
        this.start.y = event.getY(0);
        this.end.x = event.getX(1);
        this.end.y = event.getY(1);
    }

    public double calculateLength() {
        length = MathUtils.distance(start, end);
        return length;
    }

    public float calculateAngle() {
        angle = MathUtils.angle(start, end);
        return angle;
    }

    public float getAngle() {
        return angle;
    }

    public void setAngle(float angle) {
        this.angle = angle;
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public PointF getStart() {
        return start;
    }

    public PointF getEnd() {
        return end;
    }

}
