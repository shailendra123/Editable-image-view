
package com.shailendra.annotateview;

import android.graphics.PointF;
import android.view.MotionEvent;

public class MathUtils {
	
	public static double distance(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return Math.sqrt(x * x + y * y);
	}
	
	public static double distance(PointF point1, PointF point2) {
		float x = point1.x - point2.x;
		float y = point1.y - point2.y;
		return Math.sqrt(x * x + y * y);
	}
	
	public static double distance(float x1, float y1, float x2, float y2) {
		float x = x1 - x2;
		float y = y1 - y2;
		return Math.sqrt(x * x + y * y);
	}

	public static void midpoint(MotionEvent event, PointF point) {
		float x1 = event.getX(0);
		float y1 = event.getY(0);
		float x2 = event.getX(1);
		float y2 = event.getY(1);
		midpoint(x1, y1, x2, y2, point);
	}

	public static void midpoint(float x1, float y1, float x2, float y2, PointF point) {
		point.x = (x1 + x2) / 2.0f;
		point.y = (y1 + y2) / 2.0f;
	}
	
	/**
	 * Rotates p1 around p2 by angle degrees.
	 * @param point1
	 * @param point2
	 * @param angle
	 */
	public void rotate(PointF point1, PointF point2, float angle) {
		float px = point1.x;
		float py = point1.y;
		float ox = point2.x;
		float oy = point2.y;
		point1.x = (float) (Math.cos(angle) * (px-ox) - Math.sin(angle) * (py-oy) + ox);
		point1.y = (float) (Math.sin(angle) * (px-ox) + Math.cos(angle) * (py-oy) + oy);
	}
	
	public static float angle(PointF p1, PointF p2) {
		return angle(p1.x, p1.y, p2.x, p2.y);
	}	
	
	public static float angle(float x1, float y1, float x2, float y2) {
		return (float) Math.atan2(y2 - y1, x2 - x1);
	}
}
