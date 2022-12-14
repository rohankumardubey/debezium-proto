/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 * 
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.debezium.service;

import org.apache.samza.config.Config;
import org.debezium.Testing;
import org.debezium.core.component.DatabaseId;
import org.debezium.core.component.EntityCollection.FieldType;
import org.debezium.core.component.EntityType;
import org.debezium.core.component.Identifier;
import org.debezium.core.component.SchemaEditor;
import org.debezium.core.doc.Document;
import org.debezium.core.message.Message;
import org.debezium.core.message.Message.Status;
import org.debezium.core.message.Patch;
import org.debezium.core.message.Topic;
import org.debezium.samza.AbstractServiceTest;
import org.fest.assertions.Fail;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 *
 */
public class SchemaStorageServiceTest extends AbstractServiceTest {

    private static final String CLIENT_ID = "some-unique-client";
    private static final String USER = "jane.smith";
    private static final long REQUEST_ID = 1234L;
    private static final long TIMESTAMP = System.currentTimeMillis();
    private static final DatabaseId DBID = Identifier.of("testdb");
    private static final Document PHONEBOOK_SCHEMA_DOC;
    private static final EntityType CONTACTS = Identifier.of(DBID, "contacts");
    private static final EntityType CALLS = Identifier.of(DBID, "calls");

    static {
        Document schema = Document.create();
        Document contacts = SchemaEditor.getOrCreateComponent(CONTACTS, schema);
        Document calls = SchemaEditor.getOrCreateComponent(CALLS, schema);

        // Edit the 'contacts' type ...
        Patch.Editor<Patch<EntityType>> contactsEditor = Patch.edit(CONTACTS);
        SchemaEditor.createField(contactsEditor, "firstName").type(FieldType.STRING).optional(false).description("First name");
        SchemaEditor.createField(contactsEditor, "lastName").type(FieldType.STRING).optional(false).description("Last name");
        SchemaEditor.createField(contactsEditor, "middleName").type(FieldType.STRING).optional(true).description("Middle name");
        contactsEditor.end().apply(contacts, (op) -> Fail.fail("failed to patch 'contacts': " + op));

        // Edit the 'calls' type ...
        Patch.Editor<Patch<EntityType>> callsEditor = Patch.edit(CALLS);
        SchemaEditor.createField(callsEditor, "time").type(FieldType.TIMESTAMP).optional(false).description("Time of call");
        SchemaEditor.createField(callsEditor, "from").type(FieldType.STRING).optional(false).description("Caller number");
        SchemaEditor.createField(callsEditor, "to").type(FieldType.STRING).optional(false).description("Called number");
        SchemaEditor.createField(callsEditor, "duration").type(FieldType.INTEGER).optional(false)
                    .description("Duration of call in minutes");
        callsEditor.end().apply(calls, (op) -> Fail.fail("failed to patch 'calls': " + op));

        SchemaEditor.setLearning(schema, true);

        // Testing.Print.enable();
        Testing.print(schema);

        PHONEBOOK_SCHEMA_DOC = schema;
    }

    private SchemaStorageService service;
    private Config config;

    @Before
    public void beforeEach() {
        service = new SchemaStorageService();
        config = testConfig();
    }

    @Test
    public void shouldStoreSchemaUponCreationPatchWithEmptyInitialDocumentAndSubmitResponse() {
        shouldStoreSchemaUponCreationPatchWithEmptyInitialDocument();
    }

    public void shouldStoreSchemaUponCreationPatchWithEmptyInitialDocument() {
        service.init(config, testContext());

        // Build a request to create the database schema ...
        Patch<DatabaseId> patch = Patch.create(DBID, PHONEBOOK_SCHEMA_DOC);
        Document msg = Document.create();
        Message.addHeaders(msg, CLIENT_ID, REQUEST_ID, USER, TIMESTAMP);
        msg = Message.createPatchRequest(msg, patch);

        // Create the expected output ...
        Document after = Document.create();
        patch.apply(after, (op) -> Fail.fail("failed to apply patch to 'after': " + op));
        Document expected = Document.create();
        Message.addHeaders(expected, CLIENT_ID, REQUEST_ID, USER, TIMESTAMP);
        Message.setStatus(expected, Status.SUCCESS);
        Message.addId(expected, DBID);
        expected.setDocument("after", after);

        // Submit the request ...
        OutputMessages output = process(service, DBID.asString(), msg);
        assertNextMessage(output).hasStream(Topic.SCHEMA_UPDATES).hasKey(DBID).hasMessage(expected);
        assertNextMessage(output).hasStream(Topic.PARTIAL_RESPONSES).hasKey(DBID).hasMessage().with("after", after);
        assertNoMoreMessages(output);

        // Attempt to read it again ...
        Patch<DatabaseId> read = Patch.read(DBID);
        Document msg2 = Document.create();
        Message.addHeaders(msg2, CLIENT_ID, REQUEST_ID, USER, TIMESTAMP);
        msg2 = Message.createPatchRequest(msg2, read);
        output = process(service, DBID.asString(), msg2);

        // Verify the result is a read response ...
        assertNextMessage(output).hasStream(Topic.PARTIAL_RESPONSES).hasKey(DBID).hasMessage().with("after", after);
        assertNoMoreMessages(output);
    }
}
