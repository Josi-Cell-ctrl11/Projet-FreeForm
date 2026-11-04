package com.josias.floatx;
import android.view.Surface;
import android.view.MotionEvent;

interface IWindowHost {
    int open(in Surface surface, int width, int height, int dpi, String component);
    void resize(int width, int height, int dpi);
    oneway void touch(in MotionEvent event);
    void sendText(String text);
    void back();
    void close();
    void destroy() = 16777114;
}
