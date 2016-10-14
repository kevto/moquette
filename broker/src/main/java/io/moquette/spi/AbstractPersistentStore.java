package io.moquette.spi;

import java.io.Serializable;


/**
 * Abstract PersistentStore implementation containing the DTO and nothing else.
 *
 * @author Kevin Berendsen
 */
public abstract class AbstractPersistentStore implements IPersistentStore {

    /**
     * This is a DTO used to persist minimal status (clean session and activation status) of
     * a session.
     * */
    public static class PersistentSession implements Serializable {
        /** Clean session state in boolean. */
        public final boolean cleanSession;

        /**
         * Constructs a new instance
         * @param cleanSession whether starts a session clean or not.
         */
        public PersistentSession(boolean cleanSession) {
            this.cleanSession = cleanSession;
        }
    }

}

