package io.left.tpsn;

/**
 * BaseTpsnMessage Object is used to send
 * and obtain the Tpsn Algorithm data exchanged between the mesh nodes.
 */
public abstract class BaseTpsnMessage {

    /**
     * Returns the Tpsn Message type.
     *
     * @return The Tpsn message type.
     */
    public abstract TpsnMessageType getType();

    /**
     * Returns the Tpsn Tree level.
     *
     * @return The Tpsn Tree level.
     */
    public abstract int getLevel();

    /**
     * Returns the Tpsn Message Timestamp 1.
     *
     * @return The Tpsn Message Timestamp 1.
     */
    public abstract long getTimeStamp1();

    /**
     * Returns the Tpsn Message Timestamp 2.
     *
     * @return The Tpsn Message Timestamp 2.
     */
    public abstract long getTimeStamp2();

    /**
     * Returns the Tpsn Message Timestamp 3.
     *
     * @return The Tpsn Message Timestamp 3.
     */
    public abstract long getTimeStamp3();

    /**
     * Returns the Id of the Receiver node.
     * @return The Id of the Receiver node.
     */
    public abstract String getReceiverId();

    /**
     * Sets the Tpsn Tree level.
     *
     * @param level     The Tpsn tree level.
     */
    public abstract void setLevel(int level);

    /**
     * Sets the Tpsn Message Timestamp 1.
     *
     * @param timeStamp1   The Tpsn message timestamp1.
     */
    public abstract void setTimeStamp1(long timeStamp1);

    /**
     * Sets the Tpsn Message Timestamp 2.
     *
     * @param timeStamp2   The Tpsn message timestamp2.
     */
    public abstract void setTimeStamp2(long timeStamp2);

    /**
     * Sets the Tpsn Message Timestamp 3.
     *
     * @param timeStamp3   The Tpsn message timestamp3.
     */
    public abstract void setTimeStamp3(long timeStamp3);

    /**
     * Sets the Id of the Receiver node.
     *
     * @param receiverId    The receiver id.
     */
    public abstract void setReceiverId(String receiverId);
}
