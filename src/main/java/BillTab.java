public enum BillTab {
    HEADER("header"),
    ACTIONS("all-actions"),
    AMENDMENTS("amendments"),
    COMMITTEES("committees"),
    COSPONSORS("cosponsors");

    private final String fileName;

    BillTab(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() { return fileName; }
}
