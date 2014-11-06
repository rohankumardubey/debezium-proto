/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.debezium.client;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.debezium.core.component.DatabaseId;
import org.debezium.core.util.NamedThreadFactory;


/**
 * @author Randall Hauch
 *
 */
final class DbzClient implements Debezium.Client {

    private final DbzConfiguration config;
    private final DbzNode node;
    private final DbzDatabases databases;
    private final ResponseHandlers responseHandlers;
    private ExecutorService executor;
    
    public DbzClient( Configuration config ) {
        this.config = (DbzConfiguration)config;
        this.node = new DbzNode(this.config.getDocument(),this::executor);
        this.responseHandlers = new ResponseHandlers();
        this.databases = new DbzDatabases(this.responseHandlers);
        this.node.add(this.databases);
        this.node.add(this.responseHandlers); // will be shut down after 'databases'
    }
    
    private Executor executor() {
        return this.executor;
    }
    
    public DbzClient start() {
        boolean useDaemonThreads = false;    // they will keep the VM running if not shutdown properly
        ThreadFactory threadFactory = new NamedThreadFactory("debezium", "consumer",useDaemonThreads);
        executor = Executors.newCachedThreadPool(threadFactory);
        node.start();
        return this;
    }
    
    @Override
    public Database connect(DatabaseId id, String username) {
        return databases.connect(new ExecutionContext(id,username), 10, TimeUnit.SECONDS);
    }
    
    @Override
    public void shutdown( long timeout, TimeUnit unit ) {
        // Shutdown the cluster node, which shuts down all services and the service manager ...
        node.shutdown();
        if ( executor != null ) {
            try {
                executor.shutdown();
            } finally {
                try {
                    executor.awaitTermination(timeout, unit);
                } catch ( InterruptedException e ) {
                    // We were interrupted while blocking, so clear the status ...
                    Thread.interrupted();
                }
            }
        }
    }
}