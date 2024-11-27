package com.example.network;

public class InputEvent {
    public static final byte KEY = 0;
    public static final byte MOUSE_BUTTON = 1;
    public static final byte MOUSE_MOVE = 2;
    
    public final byte type;
    public final int code;
    public final boolean state;
    public final double x;
    public final double y;
    
    // Constructor for key and mouse button events
    public InputEvent(byte type, int code, boolean state) {
        this.type = type;
        this.code = code;
        this.state = state;
        this.x = 0;
        this.y = 0;
    }
    
    // Constructor for mouse movement events
    public InputEvent(double dx, double dy) {
        this.type = MOUSE_MOVE;
        this.code = 0;
        this.state = false;
        this.x = dx;
        this.y = dy;
    }
} 