package org.lep.senate.model;

public class Congress {
    private final int id;

    public Congress(int id) {
        this.id = id;
    }

    public int getId() { return id; }

    @Override
    public String toString() {
        return String.format("org.lep.model.Congress[id=%d]", id);
    }
}
