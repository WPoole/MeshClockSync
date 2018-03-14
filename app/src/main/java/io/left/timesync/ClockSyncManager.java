package io.left.timesync;

/**
 * The Clock Synchronization Manager.
 */
public interface ClockSyncManager {

    interface EventListener {
        void clockSyncOffsetChanged(long clockOffset);

        void debugMessagereceived(String message);
    }

    /**
     * Starts the Synchronization Algorithm.
     *
     * @return  Returns if succeeded to start, otherwise false.
     */
    boolean start();

    /**
     * Starts the Synchronization Algorithm.
     *
     * @return  True if succeeded to start, otherwise False.
     */
    boolean restart();

    /**
     * Resets the synchronized data.
     */
    void reset();

    /**
     * Returns the calculated clock offset in ms.
     *
     * @return  The clock offset.
     */
    long getClockOffset();

    /**
     * Register event listeners.
     *
     * @param listener  The event listener.
     * @return          False if already registered, otherwise True.
     */
    boolean registerEventListener(EventListener listener);

    /**
     * Unregister event listeners.
     *
     * @param listener  The event listener.
     * @return          True if successfully unregistered,
     *                  False if the listener has been registered.
     */
    boolean unregisterEventListener(EventListener listener);
}
