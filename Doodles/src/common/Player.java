package common;

import java.io.Serializable;

public class Player implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String name;

    public Player(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}