public class Congress {
    private final int id;
    private final int num;

    public Congress(int id, int num) {
        this.id = id;
        this.num = num;
    }

    public int getId() { return id; }
    public int getNum() { return num; }

    @Override
    public String toString() {
        return String.format("Congress[id=%d num=%d]", id, num);
    }
}
