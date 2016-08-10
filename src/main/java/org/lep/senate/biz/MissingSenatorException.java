package org.lep.senate.biz;

public class MissingSenatorException extends Exception {
    public MissingSenatorException(String msg) {
        super(msg);
    }
}
