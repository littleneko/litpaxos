package org.littleneko.sm;

/**
 * Created by little on 2017-06-14.
 */
public abstract class StateMachine {
    private int SMID;

    public int getSMID() {
        return SMID;
    }

    public void setSMID(int SMID) {
        this.SMID = SMID;
    }

    public abstract void execute(String value);
}
