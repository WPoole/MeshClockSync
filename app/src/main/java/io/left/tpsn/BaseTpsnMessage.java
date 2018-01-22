package io.left.tpsn;

/**
 * BaseTpsnMessage Object is used to send and obtain the Tpsn Algorithm data exchanged between the mesh nodes.
 */
public abstract class BaseTpsnMessage {

    /**
     * Returns the Tpsn Message type.
     * @return
     */
    public abstract TpsnMessageType getType();

    /**
     * Returns the Tpsn Tree level.
     * @return
     */
    public abstract int getLevel();

    /**
     * Returns the Tpsn Message Timestamp 1.
     * @return
     */
    public abstract long getTimeStamp_1();

    /**
     * Returns the Tpsn Message Timestamp 2.
     * @return
     */
    public abstract long getTimeStamp_2();

    /**
     * Returns the Tpsn Message Timestamp 3.
     * @return
     */
    public abstract long getTimeStamp_3();

    /**
     * Returns the Id of the Receiver node.
     * @return
     */
    public abstract String getReceiverId();

    /**
     * Sets the Tpsn Tree level.
     * @param level
     */
    public abstract void setLevel(int level);

    /**
     * Sets the Tpsn Message Timestamp 1.
     * @param timeStamp_1
     */
    public abstract void setTimeStamp_1(long timeStamp_1);

    /**
     * Sets the Tpsn Message Timestamp 2.
     * @param timeStamp_2
     */
    public abstract void setTimeStamp_2(long timeStamp_2);

    /**
     * Sets the Tpsn Message Timestamp 3.
     * @param timeStamp_3
     */
    public abstract void setTimeStamp_3(long timeStamp_3);

    /**
     * Sets the Id of the Receiver node.
     * @param receiverId
     */
    public abstract void setReceiverId(String receiverId);
}
