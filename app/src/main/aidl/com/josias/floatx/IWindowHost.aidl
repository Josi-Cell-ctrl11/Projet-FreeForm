package com.josias.floatx;
import android.view.Surface;
import android.view.MotionEvent;

interface IWindowHost {
    int open(in Surface surface, int width, int height, int dpi, String component) = 0;
    void resize(int width, int height, int dpi) = 1;
    boolean touch(in MotionEvent event) = 2;
    void sendText(String text) = 3;
    void back() = 4;
    void close() = 5;
    void destroy() = 16777114;
}
