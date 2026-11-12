package common;

import java.io.Serializable;

public class DrawingCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    private int x;
    private int y;
    private String action;

    public DrawingCommand(int x, int y, String action) {
        this.x = x;
        this.y = y;
        this.action = action;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public String getAction() {
        return action;
    }
}