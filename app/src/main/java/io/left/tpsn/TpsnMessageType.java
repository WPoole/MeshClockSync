package io.left.tpsn;

/**
 * Message types needed for TPSN Time Synchronization protocol.
 */
public enum TpsnMessageType {
    LEVEL_DISCOVERY((byte)0),
    TIME_SYNC((byte)1),
    SYNC_PULSE((byte)2),
    ACK((byte)3),
    LEVEL_REQUEST((byte)4);

    private final byte type;

    TpsnMessageType(byte type) {
        this.type = type;
    }

    public byte getValue() {
        return type;
    }
}
