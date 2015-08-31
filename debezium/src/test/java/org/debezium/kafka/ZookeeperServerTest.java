/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 * 
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.debezium.kafka;

import java.io.File;

import org.debezium.Testing;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Randall Hauch
 *
 */
public class ZookeeperServerTest {

    private ZookeeperServer server;
    private File dataDir;

    @Before
    public void beforeEach() {
        dataDir = Testing.Files.createTestingDirectory("zk");
        Testing.Files.delete(dataDir);
        server = new ZookeeperServer();
        server.setStateDirectory(dataDir);
    }

    @After
    public void afterEach() {
        Testing.Files.delete(dataDir);
    }

    @Test
    public void shouldStartServerAndRemoveData() throws Exception {
        Testing.debug("Running 1");
        server.startup();
        server.onEachDirectory(this::assertValidDataDirectory);
        server.shutdown(true);
        server.onEachDirectory(this::assertDoesNotExist);
    }

    @Test
    public void shouldStartServerAndLeaveData() throws Exception {
        Testing.debug("Running 2");
        server.startup();
        server.onEachDirectory(this::assertValidDataDirectory);
        server.shutdown(false);
        server.onEachDirectory(this::assertValidDataDirectory);
    }

    protected void assertValidDataDirectory(File dir) {
        assertThat(dir.exists()).isTrue();
        assertThat(dir.isDirectory()).isTrue();
        assertThat(dir.canWrite()).isTrue();
        assertThat(dir.canRead()).isTrue();
        assertThat(Testing.Files.inTargetDir(dir)).isTrue();
    }

    protected void assertDoesNotExist(File dir) {
        assertThat(dir.exists()).isFalse();
    }
}
