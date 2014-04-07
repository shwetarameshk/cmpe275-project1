package poke.server;


import eye.Comm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.server.Listner;


public class ServerListener implements Listner {

    protected static Logger logger = LoggerFactory.getLogger("management-ServerListener");

    private String id;

        public ServerListener(String id) {
            this.id = id;
        }


        @Override
        public String getListenerID() {
            return this.id;
        }

        @Override
        public void onMessage(Comm.Request msg) {
            logger.info("SERVER-- Received Message!");
        }

        @Override
        public void connectionClosed() {
            logger.info("Pooja - Inside connection closed");
        }

        @Override
        public void connectionReady() {
            // do nothing at the moment
        }
}
