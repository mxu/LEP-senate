public class Congress {
    private final int id;

    public Congress(int id) {
        this.id = id;
    }

    public int getId() { return id; }

    @Override
    public String toString() {
        return String.format("Congress[id=%d]", id);
    }
}
