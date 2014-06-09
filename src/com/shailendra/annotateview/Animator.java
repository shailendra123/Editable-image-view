package com.shailendra.annotateview;

public class Animator extends Thread {

    private final ZoomImageView view;
    private Animation animation;
    private boolean running = false;
    private boolean active = false;
    private long lastTime = -1L;
    
    //Synchronize object
    private final Object syncObject = new Object();

    public Animator(ZoomImageView view, String threadName) {
        super(threadName);
        this.view = view;
    }

    @Override
    public void run() {

        running = true;

        while (running) {

            while (active && animation != null) {
                long time = System.currentTimeMillis();
                active = animation.update(view, time - lastTime);
                view.redraw();
                lastTime = time;

                while (active) {
                    try {
                        if (view.waitForDraw(32)) { // 30Htz
                            break;
                        }
                    } catch (InterruptedException ignore) {
                        active = false;
                    }
                }
            }

            synchronized (syncObject) {
                if (running) {
                    try {
                        syncObject.wait();
                    } catch (InterruptedException ignore) {
                    }
                }
            }
        }
    }

    public void finish() {
        synchronized (syncObject) {
            running = false;
            active = false;
            syncObject.notifyAll();
        }
    }

    public void play(Animation transformer) {
        if (active) {
            cancel();
        }
        this.animation = transformer;

        activate();
    }

    public void activate() {
        synchronized (syncObject) {
            lastTime = System.currentTimeMillis();
            active = true;
            syncObject.notifyAll();
        }
    }

    public void cancel() {
        active = false;
    }
}
