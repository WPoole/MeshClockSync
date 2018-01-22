package io.left.timesync;

/**
 * The Clock Synchronization Manager
 */
public interface ClockSyncManager {

    interface EventListener {
        void clockSyncOffsetChanged(long clockOffset);
        void debugMessagereceived(String message);
    }

    /**
     * Starts the Synchronization Algorithm.
     * @return Returns if succeeded to start, otherwise false.
     */
    boolean start();

    /**
     * Starts the Synchronization Algorithm.
     * @return Returns if succeeded to start, otherwise false.
     */
    boolean restart();

    /**
     * Resets the synchronized data.
     */
    void reset();

    /**
     * Returns the calculated clock offset in ms.
     * @return
     */
    long getClockOffset();

    /**
     * Register event listeners.
     * Returns False if already registered, otherwise True.
     * @param listener
     * @return
     */
    boolean registerEventListener(EventListener listener);

    /**
     * Unregister event listeners.
     * Returns True if successfully unregistered, otherwise if object is not registered returns False.
     * @param listener
     * @return
     */
    boolean unregisterEventListener(EventListener listener);
}
