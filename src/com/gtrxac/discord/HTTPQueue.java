package com.gtrxac.discord;

import java.io.*;
import java.util.*;
import javax.microedition.io.*;

public class HTTPQueue implements Runnable {
    // Time before a connection will be closed for timeout.
    private static final int TIMEOUT_MS = 60000;

    // Max amount of possible HTTP connections
    // Nokia S40 has a limit of 8 HTTP connections, if they run out, you get "No response entries available"
//#ifdef MAX_HTTP_SLOTS_32
    private static final int MAX_SLOTS = 32;
//#else
//#ifdef MAX_HTTP_SLOTS_16
    private static final int MAX_SLOTS = 16;
//#else
    private static final int MAX_SLOTS = 8;
//#endif
//#endif

    /** Guards the queue and the per-thread mapping. Never held while sleeping or closing. */
    private static final Object LOCK = new Object();

    private long timestamp;
    private Thread owner;
    private volatile boolean canceled;
    public volatile InputStream is;
    public volatile OutputStream os;
    public volatile HttpConnection hc;

    private static final Vector queueItems = new Vector(MAX_SLOTS);
    private static final Hashtable activeByThread = new Hashtable();
    private static Thread monitor;

    private HTTPQueue() {
        timestamp = System.currentTimeMillis();
    }

    public static HTTPQueue newQueueItem() {
        // wait for available slot
        while (true) {
            synchronized (LOCK) {
                if (queueItems.size() < MAX_SLOTS) {
                    HTTPQueue result = new HTTPQueue();
                    result.owner = Thread.currentThread();
                    queueItems.addElement(result);
                    activeByThread.put(result.owner, result);
                    ensureMonitor();
                    return result;
                }
                ensureMonitor();
            }
            // System.out.println("waiting, slots filled " + queueItems.size());
            Util.sleep(100);
        }
    }

    /** Starts the queue monitor only when there is something to monitor. */
    private static void ensureMonitor() {
        if (monitor == null) {
            monitor = new Thread(new HTTPQueue());
            monitor.start();
        }
    }

    public void setConnection(HttpConnection connection) {
        hc = connection;
        if (canceled) close(hc, null, null);
    }

    public void setInputStream(InputStream stream) {
        is = stream;
        if (canceled) close(null, stream, null);
    }

    public void setOutputStream(OutputStream stream) {
        os = stream;
        if (canceled) close(null, null, stream);
    }

    public void run() {
        while (true) {
            Vector expired = null;

            synchronized (LOCK) {
                if (queueItems.isEmpty()) {
                    monitor = null;
                    return;
                }

                long now = System.currentTimeMillis();
                for (int i = 0; i < queueItems.size(); ) {
                    HTTPQueue item = (HTTPQueue) queueItems.elementAt(i);

                    if (now - item.timestamp >= TIMEOUT_MS) {
                        queueItems.removeElementAt(i);
                        if (item.owner != null && activeByThread.get(item.owner) == item) {
                            activeByThread.remove(item.owner);
                        }
                        if (expired == null) expired = new Vector();
                        expired.addElement(item);
                    }
                    else i++;
                }
            }

            if (expired != null) {
                for (int i = 0; i < expired.size(); i++) {
                    ((HTTPQueue) expired.elementAt(i)).close();
                }
            }

            // System.out.println("Checked connections, currently " + queueItems.size());
            Util.sleep(5000);
        }
    }

    /**
     * Closes the active request started by the given thread, if any. Closing
     * the connection unblocks a pending read/write on that thread.
     * @return true if a queued request was found and canceled
     */
    public static boolean cancelFor(Thread thread) {
        if (thread == null) return false;

        HTTPQueue item;
        synchronized (LOCK) {
            item = (HTTPQueue) activeByThread.remove(thread);
            if (item == null) return false;
            queueItems.removeElement(item);
        }

        item.canceled = true;
        item.close();
        return true;
    }

    private void close() {
        close(hc, is, os);
    }

    private void close(HttpConnection connection, InputStream input, OutputStream output) {
        // System.out.println(this.toString() + " closed");
        try { if (input != null) input.close(); } catch (Exception e) {}
        try { if (output != null) output.close(); } catch (Exception e) {}
        try { if (connection != null) connection.close(); } catch (Exception e) {}
    }

    public void finished() {
        close();
        synchronized (LOCK) {
            queueItems.removeElement(this);
            if (owner != null && activeByThread.get(owner) == this) {
                activeByThread.remove(owner);
            }
        }
    }
}
