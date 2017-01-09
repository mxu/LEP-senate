package org.lep.senate.model;

public class Bill {
    private final int id;
    private final int sponsorId;
    private final int congressId;
    private final int num;
    private final String title;
    private final int AIC;
    private final int ABC;
    private final int BILL;
    private final int PASS;
    private final int LAW;
    private final int amended;

    public Bill(int id,
                int sponsorId,
                int congressId,
                int num,
                String title,
                int AIC,
                int ABC,
                int BILL,
                int PASS,
                int LAW,
                int amended) {
        this.id = id;
        this.sponsorId = sponsorId;
        this.congressId = congressId;
        this.num = num;
        this.title = title;
        this.AIC = AIC;
        this.ABC = ABC;
        this.BILL = BILL;
        this.PASS = PASS;
        this.LAW = LAW;
        this.amended = amended;
    }

    public int getId() {
        return id;
    }

    public int getSponsorId() {
        return sponsorId;
    }

    public int getCongressId() {
        return congressId;
    }

    public int getNum() {
        return num;
    }

    public String getTitle() {
        return title;
    }

    public int getAIC() {
        return AIC;
    }

    public int getABC() {
        return ABC;
    }

    public int getBILL() {
        return BILL;
    }

    public int getPASS() {
        return PASS;
    }

    public int getLAW() {
        return LAW;
    }

    public int getAmended() {
        return amended;
    }

    @Override
    public String toString() {
        return String.format("org.lep.model.Bill [id=%d congressId=%d num=%d sponsorId=%d AIC=%d ABC=%d BILL=%d PASS=%d LAW=%d amended=%d title=%s]\n",
                id,
                congressId,
                num,
                sponsorId,
                AIC,
                ABC,
                BILL,
                PASS,
                LAW,
                amended,
                title);
    }

}
