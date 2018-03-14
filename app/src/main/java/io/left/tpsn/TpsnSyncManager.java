package io.left.tpsn;

import static io.left.rightmesh.mesh.MeshManager.DATA_RECEIVED;

import android.util.Log;

import io.left.rightmesh.id.MeshID;
import io.left.rightmesh.mesh.MeshManager;
import io.left.rightmesh.proto.MeshDnsProtos;
import io.left.rightmesh.util.RightMeshException;
import io.left.timesync.ClockSyncManager;
import io.reactivex.functions.Consumer;

import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArraySet;


/**
 * Implementation of the TPSN Time Synchronization algorithm.
 * The short explanation of the algorithm can be found in the following link -
 * 3.0 Timing-sync Protocol for Sensor Networks:
 * https://www.cse.wustl.edu/~jain/cse574-06/ftp/time_sync/index.html
 * It's a pure implementation. It can be adapted to our network and be more efficient.
 * For example the ACK messages probably could be sent directly to the connected children,
 * instead of broadcasting them.
 */
public final class TpsnSyncManager implements ClockSyncManager {

    /**
     * Time period (ms) the root waits between starting the tree construction
     * and start of the synchronization.
     */
    private static final int TREE_CONSTRUCTION_TIME = 5 * 1000;

    /**
     * The time for starting the synchronization will be a random value (ms)
     * between 0 and this bound.
     */
    private static final int RANDOM_INTERVAL_BOUND = 5 * 1000;

    /**
     * Waiting timeout ms,
     * after which the sent message will be considered lost.
     */
    private static final long TIMEOUT = 60 * 1000;

    /**
     * Number of retransmits,
     * after this number of retransmits with no answer the parentId node will be considered DEAD.
     */
    private static final short RETRANSMITS = 3;


    private static String TAG = TpsnSyncManager.class.getCanonicalName();

    private Random random = new Random();
    private int appPort;
    private MeshManager meshManager;
    private BaseTpsnMessageFactory messagesFactory;
    private Timer timer = new Timer(true);
    private TimerTask timerElapsedTask;
    private TimerTask syncDelayedTask;
    private short retransmitsCount = 0;
    private boolean levelDiscovery = true;
    private MeshID parentId = null;
    private MeshID ownId = null;
    private int treeLevel = Integer.MAX_VALUE;
    private boolean root = false;
    private boolean clockSynchronized = false;
    private long clockOffset = 0;

    private CopyOnWriteArraySet<EventListener> eventListeners = new CopyOnWriteArraySet<>();


    private static volatile TpsnSyncManager instance = null;

    /**
     * Gets the TpsnSync manager.
     *
     * @param meshManager       The mesh manager object.
     * @param appPort           The application port number.
     * @param messagesFactory   The messages factory.
     * @return                  The TpsnSync manager object.
     */
    public static TpsnSyncManager getInstance(MeshManager meshManager, int appPort,
                                              BaseTpsnMessageFactory messagesFactory) {
        if (instance == null) {
            synchronized (TpsnSyncManager.class) {
                if (instance == null) {
                    instance = new TpsnSyncManager(meshManager, appPort, messagesFactory);
                }
            }
        }

        return instance;
    }

    private TpsnSyncManager(MeshManager meshManager, int appPort,
                            BaseTpsnMessageFactory messagesFactory) {
        this.appPort = appPort;
        this.meshManager = meshManager;
        this.messagesFactory = messagesFactory;

        meshManager.on(DATA_RECEIVED, new Consumer() {
            @Override
            public void accept(Object o) throws Exception {
                handleDataReceived((MeshManager.RightMeshEvent) o);
            }
        });
    }

    /**
     * Sets the Tpsn Algorithm isRoot status for the current node.
     * @param isRoot    The is root status.
     */
    public void isRoot(boolean isRoot) {
        this.root = isRoot;
    }

    /**
     * Starts the synchronization algorithm.
     * @return Returns 'false' if no connection to Mesh Service, otherwise 'true'.
     */
    @Override
    public boolean start() {
        ownId = meshManager.getUuid();
        if (ownId == null) {
            sendMessageEvent("No active connection to the Mesh Service.");
            return false;
        }

        if (root) {
            treeLevel = 0;
            clockSynchronized = true;
        } else {
            treeLevel = Integer.MAX_VALUE;
        }

        sendMessageEvent("Starting TPSN Clock Synchronization Algorithm.....");
        sync();

        return true;
    }

    /**
     * Restarts the synchronization algorithm.
     * Internally calls the resetBtnClicked() method and then the start() method.
     * @return Returns 'false' if no connection to Mesh Service, otherwise 'true'.
     */
    @Override
    public boolean restart() {
        reset();
        return start();
    }

    /**
     * Resets the internal synchronization data.
     */
    @Override
    public void reset() {
        parentId = null;
        ownId = null;
        levelDiscovery = true;
        retransmitsCount = 0;
        clockOffset = 0;
        treeLevel = Integer.MAX_VALUE;
        root = false;
        clockSynchronized = false;

        //Clean all canceled Tasks
        timer.purge();

        sendOffsetChangedEvent();
        sendMessageEvent("The internal synchronization data was resetBtnClicked.");
    }

    /**
     * Returns the clock drift.
     * @return
     */
    @Override
    public long getClockOffset() {
        return clockOffset;
    }


    /**
     * Handles Data received from the Mesh Network.
     * @param e The right mesh event.
     */
    private void handleDataReceived(MeshManager.RightMeshEvent e) {

        //store the time-stamp if it's a Sync-Pulse or Ack packet
        long localTimeStamp = getCurrentTimeMillis();

        sendMessageEvent("Received data from: " + e.peerUuid);

        //This node haven't started the sync process yet.
        if (ownId == null) {
            return;
        }

        final MeshManager.DataReceivedEvent event = (MeshManager.DataReceivedEvent) e;

        //decode the TPSN packet
        BaseTpsnMessage recvMsg = messagesFactory.createFromByteArray(event.data);

        if (recvMsg == null) {
            sendMessageEvent("FAiled to create the TpsnMessage from: " + event.peerUuid);
            return;
        }

        switch (recvMsg.getType()) {
            //Level-Discovery message from the parentId node
            case LEVEL_DISCOVERY:
                sendMessageEvent("Received LEVEL_DISCOVERY message with level "
                        + recvMsg.getLevel() + ", from parent " + event.peerUuid);
                if (recvMsg.getLevel() < this.treeLevel) {
                    this.treeLevel = recvMsg.getLevel() + 1;
                    parentId = event.peerUuid;
                    byte[] packet = messagesFactory.create(TpsnMessageType.LEVEL_DISCOVERY,
                            this.treeLevel);
                    sendMessageEvent("Sending LEVEL_DISCOVERY message with level "
                            + this.treeLevel + " to children.");
                    sendToChildren(packet);
                }
                break;

            //Level-Request message from the new connected child node
            case LEVEL_REQUEST:
                sendMessageEvent("Received LEVEL_REQUEST message from newly connected child node: "
                        + event.peerUuid);
                if (treeLevel == Integer.MAX_VALUE) {
                    sendMessageEvent("No reply. As own level not set yet.");
                    break;
                }

                byte[] levelDiscoveryPacket
                        = messagesFactory.create(TpsnMessageType.LEVEL_DISCOVERY, treeLevel);
                try {
                    sendMessageEvent("Sending LEVEL_DISCOVERY message with level " + this.treeLevel
                            + " to child: " + event.peerUuid);
                    meshManager.sendDataReliable(event.peerUuid, appPort, levelDiscoveryPacket);
                } catch (RightMeshException e1) {
                    sendMessageEvent("Failed to sendDataReliable: peerUuid:" + event.peerUuid
                            + " appPort:" + appPort + ". See log for details.");
                    Log.e(TAG, "Failed to sendDataReliable: peerUuid:" + event.peerUuid
                            + " appPort:" + appPort, e1);
                }

                //If Clock already synchronized, send imitated Ack packet from parentId to this node
                // in order the newly connected node will start sync phase
                if (clockSynchronized) {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e1) {
                        sendMessageEvent("Thread.sleep failed. See log for details.");
                        Log.e(TAG, "Thread.sleep failed.", e1);
                    }

                    byte[] dummyAckPacket = messagesFactory.create(TpsnMessageType.ACK,
                            treeLevel - 1, 0,0,0,
                            ownId.toString());
                    try {
                        sendMessageEvent("If already Synchronized, "
                                + "send imitated Ack packet from parentId to a new child: "
                                + event.peerUuid);
                        meshManager.sendDataReliable(event.peerUuid, appPort, dummyAckPacket);
                    } catch (RightMeshException e1) {
                        sendMessageEvent("Failed to sendDataReliable: peerUuid:" + event.peerUuid
                                + " appPort:" + appPort + ("See log for details"));
                        Log.e(TAG, "Failed to sendDataReliable: peerUuid:" + event.peerUuid
                                + " appPort:" + appPort, e1);
                    }
                } else {
                    sendMessageEvent("clockSynchronized = false");
                }
                break;

            //Time-Sync message from root node, nodes with level 1 should start the sync process.
            case TIME_SYNC:
                sendMessageEvent("Received Time-Sync message from ROOT node,"
                        + " nodes with level 1 should start the sync process. from: "
                        + event.peerUuid);
                if (treeLevel == 1) {
                    sendMessageEvent("Starting randomly delayed Sync Phase.");
                    invokeDelayedSync();
                }
                break;

            //Sync-Pulse message from the child node,
            //the Ack-Message should be sent back as broadcast
            case SYNC_PULSE:
                sendMessageEvent("Received Sync-Pulse message from the child node: "
                        + event.peerUuid);
                sendMessageEvent("Broadcasting ACK message.");
                byte[] ackPacket = messagesFactory.create(TpsnMessageType.ACK, treeLevel,
                        recvMsg.getTimeStamp1(), localTimeStamp, getCurrentTimeMillis(),
                        event.peerUuid.toString());
                //TODO: Update the algorithm for our network, probably we don't need to cast data
                castData(ackPacket);
                break;

            //Ack message from parentId node, a reply for Sync-Pulse message
            case ACK:
                if (ownId == null) {
                    sendMessageEvent("ownId is null, probably the Sync data was resetBtnClicked.");
                } else if (clockSynchronized) {
                    sendMessageEvent("Received ACK message. Already synchronized.");
                } else if (recvMsg.getReceiverId().equals(ownId.toString())) {
                    sendMessageEvent("Received ACK message that was addressed to me.");
                    stopTimer();
                    sendMessageEvent("Calculating the clock offset...");
                    calculateTheOffset(recvMsg, localTimeStamp);
                    clockSynchronized = true;
                    sendOffsetChangedEvent();
                } else if (parentId != null
                        && recvMsg.getReceiverId().equals(parentId.toString())) {
                    sendMessageEvent("Received ACK message that was addressed to my parent.");
                    sendMessageEvent("Starting randomly delayed Sync Phase.");
                    invokeDelayedSync();
                }
                break;

            default:
                Log.e(TAG, "default case");
                break;
        }
    }

    private void invokeDelayedSync() {
        syncDelayedTask = new TimerTask() {
            @Override
            public void run() {
                sync();
            }
        };

        long rand = random.nextInt(RANDOM_INTERVAL_BOUND);

        //The Sync Phase will be started after waiting Random time.
        timer.schedule(syncDelayedTask, rand);
    }

    /**
     * Calculates the Clock's Offset.
     * @param msg           The Ack Tpsn Message.
     * @param timeStamp4   Current local Timestamp (T4).
     */
    private void calculateTheOffset(BaseTpsnMessage msg, long timeStamp4) {
        //Offset = ((T2 - T1) - (T4 - T3)) / 2
        clockOffset = ((msg.getTimeStamp2() - msg.getTimeStamp1())
                - (timeStamp4 - msg.getTimeStamp3())) / 2;
        sendMessageEvent("--> Clock Offset: " + clockOffset);
    }

    /**
     * Stops the timeout timer.
     */
    private void stopTimer() {
        if (timerElapsedTask != null) {
            timerElapsedTask.cancel();
        }
    }

    /**
     * The child node will retransmit Sync-Pulse message to its parentId node
     * after waiting for Ack message timeout.
     */
    private void syncPulseTimeout() {
        if (++retransmitsCount == RETRANSMITS) {
            reset();
        } else {
            sync();
        }
    }


    /**
     * The main Tpsn algorithm function.
     */
    private void sync() {
        //if node is root, starts the TPSN algorithm
        if (treeLevel == 0) {

            //Start the Level Discovery phase
            if (levelDiscovery) {
                byte[] msg = messagesFactory.create(TpsnMessageType.LEVEL_DISCOVERY, treeLevel);
                sendMessageEvent("Sending LEVEL_DISCOVERY to children.");
                sendToChildren(msg);
                levelDiscovery = false;

                timerElapsedTask = new TimerTask() {
                    @Override
                    public void run() {
                        sync();
                    }
                };

                //The Sync Phase will be started after waiting Tree Construction time.
                timer.schedule(timerElapsedTask, TREE_CONSTRUCTION_TIME);
            } else { //Start the Sync Phase
                byte[] msg = messagesFactory.create(TpsnMessageType.TIME_SYNC);
                sendMessageEvent("Sending TIME_SYNC to children.");
                sendToChildren(msg);
            }
        } else { //if not root
            //not a root and doesn't have a parentId
            if (treeLevel == Integer.MAX_VALUE) {
                byte[] msg = messagesFactory.create(TpsnMessageType.LEVEL_REQUEST);
                sendMessageEvent("Sending LEVEL_REQUEST to parent.");
                sendToParent(msg);
            } else { //has parentId, request sync
                timerElapsedTask = new TimerTask() {
                    @Override
                    public void run() {
                        syncPulseTimeout();
                    }
                };

                timer.schedule(timerElapsedTask, TIMEOUT);
                byte[] msg = messagesFactory.create(TpsnMessageType.SYNC_PULSE, treeLevel,
                        getCurrentTimeMillis());
                sendMessageEvent("Sending SYNC_PULSE to parent.");
                sendToParent(msg);
            }
        }
    }

    private void castData(byte[] message) {

        //Get peers that listening to the specific port
        Set<MeshID> peers = null;
        try {
            peers = meshManager.getPeers(appPort);
        } catch (RightMeshException e) {
            sendMessageEvent("Failed to get Peers. See log for details.");
            Log.e(TAG, "Failed to get Peers.", e);
        }

        if (peers == null) {
            return;
        }

        for (MeshID peerMeshId : peers) {
            if (peerMeshId.equals(ownId) || peerMeshId.equals(parentId)) {
                continue;
            }

            try {
                sendMessageEvent("Sending to: " + peerMeshId);
                meshManager.sendDataReliable(peerMeshId, appPort, message);
            } catch (RightMeshException e1) {
                sendMessageEvent("Failed to sendDataReliable: peerUuid:" + peerMeshId + " appPort:"
                        + appPort + ". See log for details.");
                Log.e(TAG, "Failed to sendDataReliable: peerUuid:" + peerMeshId + " appPort:"
                        + appPort, e1);
            }
        }
    }

    private void sendToChildren(byte[] message) {

        //Get peers that listening to the specific port
        Set<MeshID> peers = null;
        try {
            peers = meshManager.getPeers(appPort);
        } catch (RightMeshException e) {
            sendMessageEvent("Failed to get Peers. See log for details.");
            Log.e(TAG, "Failed to get Peers.", e);
        }

        if (peers == null) {
            return;
        }

        try {
            //If a Client, there are no children.
            HashMap<String, MeshDnsProtos.MeshRequest.Role> role
                    = meshManager.getRole(meshManager.getUuid());
            if (role.containsValue(MeshDnsProtos.MeshRequest.Role.CLIENT)) {
                return;
            }
        } catch (RightMeshException e) {
            e.printStackTrace();
        }

        //find out direct children and send the level discovery message
        for (MeshID peerMeshId : peers) {
            if (peerMeshId.equals(ownId) || peerMeshId.equals(parentId)) {
                continue;
            }

            MeshID nextHopPeer = null;
            try {
                nextHopPeer = meshManager.getNextHopPeer(peerMeshId);
            } catch (RightMeshException e) {
                sendMessageEvent("Failed to getNextHopPeer for node: " + peerMeshId
                        + ". See log for details.");
                Log.e(TAG, "Failed to getNextHopPeer for node: " + peerMeshId, e);
                continue;
            }

            //Direct child
            if (nextHopPeer.equals(peerMeshId)) {
                try {
                    sendMessageEvent("Sending to children: " + peerMeshId);
                    meshManager.sendDataReliable(peerMeshId, appPort, message);
                } catch (RightMeshException e) {
                    sendMessageEvent("Failed to send data to node: " + peerMeshId
                            + ". See log for details.");
                    Log.e(TAG, "Failed to send data to node: " + peerMeshId, e);
                }
            }
        }
    }


    private void sendToParent(byte[] message) {

        if (parentId != null) {
            try {
                meshManager.sendDataReliable(parentId, appPort, message);
            } catch (RightMeshException e) {
                sendMessageEvent("Failed to sendDataReliable: parentId:" + parentId + "appPort:"
                        + appPort + ". See log for details.");
                Log.e(TAG, "Failed to sendDataReliable: parentId:" + parentId + " appPort:"
                        + appPort, e);
            }
            return;
        }

        //Get peers that listening to the a specific port
        Set<MeshID> peers = null;
        try {
            peers = meshManager.getPeers(appPort);
        } catch (RightMeshException e) {
            sendMessageEvent("Failed to get Peers. See log for details.");
            Log.e(TAG, "Failed to get Peers.", e);
        }

        if (peers == null) {
            return;
        }

        //find out a direct parentId
        for (MeshID peerMeshId : peers) {
            if (peerMeshId.equals(ownId)) {
                continue;
            }

            try {
                HashMap<String, MeshDnsProtos.MeshRequest.Role> role
                        = meshManager.getRole(peerMeshId);

                //TODO: There are maybe different Masters on different interfaces,
                //to which one we want to send the message.
                if ((role.containsValue(MeshDnsProtos.MeshRequest.Role.MASTER)
                        || role.containsValue(MeshDnsProtos.MeshRequest.Role.ROUTER))
                        && meshManager.getNextHopPeer(peerMeshId).equals(peerMeshId)) {
                    try {
                        sendMessageEvent("Sending to parent: " + peerMeshId);
                        meshManager.sendDataReliable(peerMeshId, appPort, message);
                    } catch (RightMeshException e) {
                        sendMessageEvent("Failed to sendDataReliable: peerMeshId:" + peerMeshId
                                + " appPort:" + appPort + ". See log for details.");
                        Log.e(TAG, "Failed to sendDataReliable: peerMeshId:" + peerMeshId
                                + " appPort:" + appPort, e);
                    }
                }
            } catch (RightMeshException e) {
                sendMessageEvent("Failed to Role for node: " + peerMeshId + "."
                        + " See log for details.");
                Log.e(TAG, "Failed to Role for node: " + peerMeshId, e);
            }
        }
    }

    private void sendMessageEvent(String message) {
        for (EventListener listener : eventListeners) {
            try {
                listener.debugMessagereceived(message);
                // CHECKSTYLE IGNORE IllegalCatchCheck
            } catch (Exception ex) {
                // CHECKSTYLE END IGNORE IllegalCatchCheck
                sendMessageEvent("Failed to invoke debugMessageReceived Event. "
                        + "See log for details.");
                Log.e(TAG, ex.getMessage(), ex);
                unregisterEventListener(listener);
            }
        }
    }

    private void sendOffsetChangedEvent() {
        for (EventListener listener : eventListeners) {
            try {
                listener.clockSyncOffsetChanged(clockOffset);
                // CHECKSTYLE IGNORE IllegalCatchCheck
            } catch (Exception ex) {
                // CHECKSTYLE END IGNORE IllegalCatchCheck
                sendMessageEvent("Failed to invoke clockSyncOffsetChanged event. "
                        + "See log for details.");
                Log.e(TAG, ex.getMessage(), ex);
                unregisterEventListener(listener);
            }
        }
    }

    @Override
    public boolean registerEventListener(ClockSyncManager.EventListener listener) {
        return eventListeners.add(listener);
    }

    @Override
    public boolean unregisterEventListener(ClockSyncManager.EventListener listener) {
        return eventListeners.remove(listener);
    }

    private long getCurrentTimeMillis() {
        return (System.currentTimeMillis() + clockOffset);
    }
}
