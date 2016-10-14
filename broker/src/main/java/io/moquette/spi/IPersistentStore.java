package io.moquette.spi;


/**
 * PersistentStore interface. Initializes a persistent store and capable of closing one.
 *
 * @author Kevin Berendsen
 */
public interface IPersistentStore {

    /**
     * Get the messages store.
     * @return implementation of IMessagesStore.
     */
    public IMessagesStore getMessagesStore();

    /**
     * Get the sessions store.
     * @return implementation of ISessionsStore.
     */
    public ISessionsStore getSessionsStore();

    /**
     * Initializing the persistent store.
     */
    public void initStore();

    /**
     * Closing the persistent store.
     */
    public void close();
}

