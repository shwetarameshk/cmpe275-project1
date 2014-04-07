package poke.server;

import eye.Comm;

/**
 * Created by poojasrinivas on 4/1/14.
 */
public interface Listner {
    public abstract String getListenerID();

    /**
     * process management messages
     *
     * @param msg
     */
    public abstract void onMessage(Comm.Request msg);

    /**
     * called when the connection fails or is not readable.
     */
    public abstract void connectionClosed();

    /**
     * called when the connection to the node is ready. This may occur on the
     * initial connection or when re-connection after a failure.
     */
    public abstract void connectionReady();
}
