/*
 * Copyright (c) 2012-2015 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The Apache License v2.0 is available at
 * http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package io.moquette.spi.persistence.mapdb;

import static io.moquette.BrokerConstants.AUTOSAVE_INTERVAL_PROPERTY_NAME;
import static io.moquette.BrokerConstants.PERSISTENT_STORE_PROPERTY_NAME;

import io.moquette.parser.proto.MQTTException;
import io.moquette.server.config.IConfig;
import io.moquette.spi.AbstractPersistentStore;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.ISessionsStore;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MapDB main persistence implementation
 */
public class MapDBPersistentStore extends AbstractPersistentStore {

    private static final Logger LOG = LoggerFactory.getLogger(MapDBPersistentStore.class);

    private DB m_db;
    private final String m_storePath;
    private final int m_autosaveInterval; // in seconds

    private final ScheduledExecutorService m_scheduler = Executors.newScheduledThreadPool(1);
    private MapDBMessagesStore m_messageStore;
    private MapDBSessionsStore m_sessionsStore;

    public MapDBPersistentStore(IConfig props) {
        this.m_storePath = props.getProperty(PERSISTENT_STORE_PROPERTY_NAME, "");
        this.m_autosaveInterval = Integer.parseInt(props.getProperty(AUTOSAVE_INTERVAL_PROPERTY_NAME, "30"));
    }

    /** {@inheritDocs} */
    @Override
    public IMessagesStore getMessagesStore() {
        return m_messageStore;
    }

    /** {@inheritDocs} */
    @Override
    public ISessionsStore getSessionsStore() {
        return m_sessionsStore;
    }

    /** {@inheritDocs} */
    @Override
    public void initStore() {
        if (m_storePath == null || m_storePath.isEmpty()) {
            m_db = DBMaker.newMemoryDB().make();
        } else {
            File tmpFile;
            try {
                tmpFile = new File(m_storePath);
                boolean fileNewlyCreated = tmpFile.createNewFile();
                LOG.info("Starting with {} [{}] db file", fileNewlyCreated ? "fresh" : "existing", m_storePath);
            } catch (IOException ex) {
                LOG.error(null, ex);
                throw new MQTTException("Can't create temp file for subscriptions storage [" + m_storePath + "]", ex);
            }
            m_db = DBMaker.newFileDB(tmpFile).make();
        }
        m_scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                m_db.commit();
            }
        }, this.m_autosaveInterval, this.m_autosaveInterval, TimeUnit.SECONDS);

        //TODO check m_db is valid and
        m_messageStore = new MapDBMessagesStore(m_db);
        m_messageStore.initStore();

        m_sessionsStore = new MapDBSessionsStore(m_db, m_messageStore);
        m_sessionsStore.initStore();
    }

    /** {@inheritDocs} */
    @Override
    public void close() {
        if (this.m_db.isClosed()) {
            LOG.debug("already closed");
            return;
        }
        this.m_db.commit();
        //LOG.debug("persisted subscriptions {}", m_persistentSubscriptions);
        this.m_db.close();
        LOG.debug("closed disk storage");
        this.m_scheduler.shutdown();
        LOG.debug("Persistence commit scheduler is shutdown");
    }

    /**
     * @return the executor service of the persistent store.
     */
    public final ScheduledExecutorService getScheduler() {
        return m_scheduler;
    }
}
