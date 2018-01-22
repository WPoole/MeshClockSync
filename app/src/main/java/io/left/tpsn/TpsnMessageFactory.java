package io.left.tpsn;

import android.util.Log;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import static android.content.ContentValues.TAG;

/**
 * Factory to create TpsnMessage objects with simple binary serialization
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
    public byte[] create(TpsnMessageType type, int level, long timeStamp_1) {
        TpsnMessage msgObj = new TpsnMessage(type);
        msgObj.setLevel(level);
        msgObj.setTimeStamp_1(timeStamp_1);
        byte[] msg = toByteArray(msgObj);

        return msg;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] create(TpsnMessageType type, int level, long timeStamp_1, long timeStamp_2, long timeStamp_3, String receiverId) {
        TpsnMessage msgObj = new TpsnMessage(type);
        msgObj.setLevel(level);
        msgObj.setTimeStamp_1(timeStamp_1);
        msgObj.setTimeStamp_2(timeStamp_2);
        msgObj.setTimeStamp_3(timeStamp_3);
        msgObj.setReceiverId(receiverId);
        byte[] msg = toByteArray(msgObj);

        return msg;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected byte[] toByteArray(BaseTpsnMessage msg) {

        TpsnMessage tpsnMsg = null;
        try{
            tpsnMsg = (TpsnMessage) msg;
        }
        catch (Exception ex){
            Log.e(TAG, "Failed to cast Base message to the actual TpsnMessage.", ex);
            return null;
        }

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
        private long timeStamp_1;
        private long timeStamp_2;
        private long timeStamp_3;
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

        public long getTimeStamp_1() {
            return timeStamp_1;
        }

        public long getTimeStamp_2() {
            return timeStamp_2;
        }

        public long getTimeStamp_3() {
            return timeStamp_3;
        }

        public String getReceiverId() {
            return receiverId;
        }

        public void setLevel(int level) {
            this.level = level;
        }

        public void setTimeStamp_1(long timeStamp_1) {
            this.timeStamp_1 = timeStamp_1;
        }

        public void setTimeStamp_2(long timeStamp_2) {
            this.timeStamp_2 = timeStamp_2;
        }

        public void setTimeStamp_3(long timeStamp_3) {
            this.timeStamp_3 = timeStamp_3;
        }

        public void setReceiverId(String receiverId) {
            this.receiverId = receiverId;
        }
    }
}
