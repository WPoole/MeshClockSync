package io.left.timesync;

import static io.left.rightmesh.mesh.MeshManager.PEER_CHANGED;
import static io.left.rightmesh.mesh.MeshManager.REMOVED;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import io.left.rightmesh.android.AndroidMeshManager;
import io.left.rightmesh.android.MeshService;
import io.left.rightmesh.id.MeshId;
import io.left.rightmesh.mesh.MeshManager;
import io.left.rightmesh.mesh.MeshStateListener;
import io.left.rightmesh.util.MeshUtility;
import io.left.rightmesh.util.RightMeshException;
import io.left.tpsn.TpsnMessageFactory;
import io.left.tpsn.TpsnSyncManager;
import io.reactivex.functions.Consumer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;


//android:gravity="bottom"

public class MainActivity extends Activity
        implements MeshStateListener, ClockSyncManager.EventListener {

    // Port to bind app to.
    private static final int APP_PORT = 1010;

    private static final String PATTERN = "CLOCK_SYNC";

    private static final String TAG = TpsnSyncManager.class.getCanonicalName();

    // MeshManager instance - interface to the mesh network.
    private AndroidMeshManager mMeshManager = null;

    // Set to keep track of peers connected to the mesh.
    private HashSet<MeshId> mUsers = new HashSet<>();

    private ClockSyncManager mClockSyncManager;
    private Timer mTimer = new Timer(true);
    private TimerTask mTimerClockTask;
    private SimpleDateFormat mSdf = new SimpleDateFormat("hh:mm:ss:SSS");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView textView = findViewById(R.id.txtLog);
        textView.setMovementMethod(new ScrollingMovementMethod());

        textView = findViewById(R.id.txtStatus);
        textView.setMovementMethod(new ScrollingMovementMethod());

        final TimeZone timeZone = TimeZone.getTimeZone("PST");
        this.mSdf.setTimeZone(timeZone);

        mMeshManager = AndroidMeshManager.getInstance(MainActivity.this,
                MainActivity.this, PATTERN);
        mClockSyncManager = TpsnSyncManager.getInstance(mMeshManager, APP_PORT,
                new TpsnMessageFactory());
        mClockSyncManager.registerEventListener(MainActivity.this);
    }

    public void clear(View v) {
        TextView log = findViewById(R.id.txtLog);
        log.setText("");
    }

    @Override
    public void clockSyncOffsetChanged(long clockOffset) {
        startClockTick();
    }

    @Override
    public void debugMessagereceived(String message) {
        print(message);
    }

    /**
     * Called by the {@link MeshService} when the mesh state changes. Initializes mesh connection
     * on first call.
     *
     * @param meshId our own user id on first detecting
     * @param state state which indicates SUCCESS or an error code
     */
    @Override
    public void meshStateChanged(MeshId meshId, int state) {

        if (state == MeshStateListener.SUCCESS) {
            try {
                // Binds this app to MESH_PORT.
                // This app will now receive all events generated on that port.
                mMeshManager.bind(APP_PORT);

                mMeshManager.on(PEER_CHANGED, new Consumer() {
                    @Override
                    public void accept(Object o) throws Exception {
                        handlePeerChanged((MeshManager.RightMeshEvent) o);
                    }
                });

                // Enable buttons now that mesh is connected.
                Button btnConfigure = (Button) findViewById(R.id.btnConfigure);
                Button btnSend = (Button) findViewById(R.id.btnReset);
                btnConfigure.setEnabled(true);
                btnSend.setEnabled(true);
            } catch (RightMeshException e) {
                String status = "Error initializing the library" + e.toString();
                Toast.makeText(getApplicationContext(), status, Toast.LENGTH_SHORT).show();
                TextView txtStatus = (TextView) findViewById(R.id.txtStatus);
                txtStatus.setText(status);
                return;
            }
        }

        // Update display on successful calls (i.e. not FAILURE or DISABLED).
        if (state == MeshStateListener.SUCCESS || state == MeshStateListener.RESUME) {
            updateStatus();
        }
    }

    /**
     * Handles peer update events from the mesh - maintains a list of peers and updates the display.
     *
     * @param e event object from mesh
     */
    private void handlePeerChanged(MeshManager.RightMeshEvent e) {
        // Update peer list.
        MeshManager.PeerChangedEvent event = (MeshManager.PeerChangedEvent) e;

        //print(event.toString());

        if (event.state != REMOVED && !mUsers.contains(event.peerUuid)) {
            mUsers.add(event.peerUuid);
        } else if (event.state == REMOVED) {
            mUsers.remove(event.peerUuid);
        }

        // Update display.
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateStatus();
            }
        });
    }


    /**
     * Update the {@link TextView} with a list of all peers.
     */
    private void updateStatus() {
        String status = mMeshManager.getUuid().toString().substring(0, 7) + "xxx" + "\npeers:\n";
        for (MeshId user : mUsers) {
            status += user.toString().substring(0, 7) + "xxx" + "\n";
        }
        TextView txtStatus = findViewById(R.id.txtStatus);
        txtStatus.setText(status);
    }

    /**
     * IsRoot checkbox click event handler.
     * @param v The View.
     */
    public void isRootCbxClicked(View v) {
        CheckBox cbx = findViewById(R.id.cbxIsRoot);
        ((TpsnSyncManager) mClockSyncManager).isRoot(cbx.isChecked());
    }

    /**
     * Start button click event handler.
     * @param v The View.
     */
    public void startBtnClicked(View v) {
        CheckBox cbx = findViewById(R.id.cbxIsRoot);
        cbx.setEnabled(false);
        Button btnStart = findViewById(R.id.btnStartSync);
        btnStart.setEnabled(false);
        mClockSyncManager.start();
    }

    /**
     * Reset button click event handler.
     * @param v The View.
     */
    public void resetBtnClicked(View v) {
        CheckBox cbx = findViewById(R.id.cbxIsRoot);
        Button btnStart = findViewById(R.id.btnStartSync);
        cbx.setEnabled(true);
        cbx.setChecked(false);
        btnStart.setEnabled(true);
        mClockSyncManager.reset();
    }

    /**
     * Open mesh settings screen.
     *
     * @param v calling view
     */
    public void configure(View v) {
        try {
            mMeshManager.showSettingsActivity();
        } catch (RightMeshException ex) {
            MeshUtility.Log(this.getClass().getCanonicalName(), "Service not connected");
        }
    }

    /**
     * Called when activity is on screen.
     */
    @Override
    protected void onResume() {
        try {
            super.onResume();
            if (mMeshManager != null) {
                mMeshManager.resume();
            }
        } catch (MeshService.ServiceDisconnectedException e) {
            e.printStackTrace();
        }

        startClockTick();
    }

    /**
     * Called when the app is being closed (not just navigated away from). Shuts down
     * the {@link AndroidMeshManager} instance.
     */
    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        try {
            super.onDestroy();
            if (mMeshManager != null) {
                mMeshManager.stop();
            }
        } catch (MeshService.ServiceDisconnectedException e) {
            e.printStackTrace();
        }

        mClockSyncManager.unregisterEventListener(MainActivity.this);
    }

    private void startClockTick() {
        if (mTimerClockTask != null) {
            mTimerClockTask.cancel();
        }

        mTimerClockTask = new TimerTask() {
            @Override
            public void run() {
                updateClock();
            }
        };

        long clockOffset = mClockSyncManager.getClockOffset();
        //Log.d(TAG, "--> clockOffset: "+clockOffset);
        long droppedMillis = 1000 * (System.currentTimeMillis() / 1000);
        droppedMillis += 3 * 1000;
        droppedMillis -= (clockOffset % 1000);
        mTimer.scheduleAtFixedRate(mTimerClockTask,  new Date(droppedMillis), 1000);
    }

    /**
     * Updates the GUI Clock.
     */
    private void updateClock() {
        String timeStr = mSdf.format(new Date(System.currentTimeMillis()
                + mClockSyncManager.getClockOffset()));

        if (timeStr == null) {
            return;
        }

        final String fTimeStr = timeStr;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView txt = findViewById(R.id.txtTime);
                txt.setText(fTimeStr);
            }
        });
    }

    private void print(String text) {
        Log.d(TAG, text);
        final String ftext = "[" + mSdf.format(System.currentTimeMillis()) + "] " + text + "\n";
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView txt = findViewById(R.id.txtLog);
                txt.append(ftext);
            }
        });
    }
}