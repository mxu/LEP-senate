package org.lep.senate.model;

public class Senator {
    private final int id;
    private final String firstName;
    private final String middleName;
    private final String lastName;
    private final String suffix;
    private final String state;

    public Senator(int id, String firstName, String middleName, String lastName,
                   String suffix, String state) {
        this.id = id;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.suffix = suffix;
        this.state = state;
    }

    public int getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getSuffix() {
        return suffix;
    }

    public String getState() {
        return state;
    }

    @Override
    public String toString() {
        return String.format("org.lep.model.Senator [%d %s %s %s %s %s]", id, firstName, middleName, lastName, suffix, state);
    }
}
