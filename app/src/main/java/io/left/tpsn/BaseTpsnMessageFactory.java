package io.left.tpsn;

/**
 * Use this Class to create the TpsnMessage Objects.
 */
public abstract class BaseTpsnMessageFactory {

    /**
     * Creates the Tpsn Message from it's binary representation.
     * @param message The binary message.
     * @return The deserialized Tpsn Message Object.
     */
    public abstract BaseTpsnMessage createFromByteArray(byte[] message) ;

    /**
     * Creates the Tpsn Message.
     * @param type The Tpsn Message type.
     * @return The Tpsn Message Object.
     */
    public abstract byte[] create(TpsnMessageType type);

    /**
     * Creates the Tpsn Message.
     * @param type The Tpsn Message type.
     * @param level The Tpsn Tree Level.
     * @return The Tpsn Message Object.
     */
    public abstract byte[] create(TpsnMessageType type, int level);

    /**
     * Creates the Tpsn Message.
     * @param type The Tpsn Message type.
     * @param level The Tpsn Tree Level.
     * @param timeStamp_1 The Tpsn Timestamp 1.
     * @return The Tpsn Message Object.
     */
    public abstract byte[] create(TpsnMessageType type, int level, long timeStamp_1);

    /**
     * Creates the Tpsn Message.
     * @param type The Tpsn Message type.
     * @param level The Tpsn Tree Level.
     * @param timeStamp_1 The Tpsn Timestamp 1.
     * @param timeStamp_2 The Tpsn Timestamp 2.
     * @param timeStamp_3 The Tpsn Timestamp 3.
     * @param receiverId The Id of receiver node.
     * @return The Tpsn Message Object.
     */
    public abstract byte[] create(TpsnMessageType type, int level, long timeStamp_1, long timeStamp_2, long timeStamp_3, String receiverId);


    /**
     * Encodes the Tpsn Message Object to it's binary representation.
     * @param msg The Tpsn Message Object
     * @return Encoded binary representation.
     */
    protected abstract byte[] toByteArray(BaseTpsnMessage msg);
}
