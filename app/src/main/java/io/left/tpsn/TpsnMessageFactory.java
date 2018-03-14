package io.left.tpsn;

import static android.content.ContentValues.TAG;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Factory to create TpsnMessage objects with simple binary serialization.
 */
public class TpsnMessageFactory extends BaseTpsnMessageFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public BaseTpsnMessage createFromByteArray(byte[] message) {
        TpsnMessage msg = null;
        ByteArrayInputStream bis = new ByteArrayInputStream(message);
        ObjectInput in = null;
        try {
            in = new ObjectInputStream(bis);
            Object obj = in.readObject();
            msg = (TpsnMessage) obj;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                // ignore close exception
            }
        }

        return msg;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] create(TpsnMessageType type) {
        TpsnMessage msgObj = new TpsnMessage(type);
        byte[] msg = toByteArray(msgObj);

        return msg;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] create(TpsnMessageType type, int level) {
        TpsnMessage msgObj = new TpsnMessage(type);
        msgObj.setLevel(level);
        byte[] msg = toByteArray(msgObj);

        return msg;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] create(TpsnMessageType type, int level, long timeStamp1) {
        TpsnMessage msgObj = new TpsnMessage(type);
        msgObj.setLevel(level);
        msgObj.setTimeStamp1(timeStamp1);
        byte[] msg = toByteArray(msgObj);

        return msg;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] create(TpsnMessageType type, int level,
                         long timeStamp1, long timeStamp2, long timeStamp3,
                         String receiverId) {
        TpsnMessage msgObj = new TpsnMessage(type);
        msgObj.setLevel(level);
        msgObj.setTimeStamp1(timeStamp1);
        msgObj.setTimeStamp2(timeStamp2);
        msgObj.setTimeStamp3(timeStamp3);
        msgObj.setReceiverId(receiverId);
        byte[] msg = toByteArray(msgObj);

        return msg;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected byte[] toByteArray(BaseTpsnMessage msg) {

        if (!(msg instanceof TpsnMessage)) {
            Log.e(TAG, "Invalid message type.");
            return null;
        }

        TpsnMessage tpsnMsg = (TpsnMessage) msg;
        byte[] bytes = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(tpsnMsg);
            out.flush();
            bytes = bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
                // ignore close exception
            }
        }

        return bytes;
    }

    /**
     * A simple implementation of the Tpsn Message.
     */
    private static class TpsnMessage extends BaseTpsnMessage implements Serializable {
        private TpsnMessageType type;
        private int level;
        private long timeStamp1;
        private long timeStamp2;
        private long timeStamp3;
        private String receiverId;

        private TpsnMessage(TpsnMessageType type) {
            this.type = type;
        }

        public TpsnMessageType getType() {
            return type;
        }

        public int getLevel() {
            return level;
        }

        public long getTimeStamp1() {
            return timeStamp1;
        }

        public long getTimeStamp2() {
            return timeStamp2;
        }

        public long getTimeStamp3() {
            return timeStamp3;
        }

        public String getReceiverId() {
            return receiverId;
        }

        public void setLevel(int level) {
            this.level = level;
        }

        public void setTimeStamp1(long timeStamp1) {
            this.timeStamp1 = timeStamp1;
        }

        public void setTimeStamp2(long timeStamp2) {
            this.timeStamp2 = timeStamp2;
        }

        public void setTimeStamp3(long timeStamp3) {
            this.timeStamp3 = timeStamp3;
        }

        public void setReceiverId(String receiverId) {
            this.receiverId = receiverId;
        }
    }
}
