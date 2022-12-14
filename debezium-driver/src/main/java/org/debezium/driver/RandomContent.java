/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 * 
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.debezium.driver;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.debezium.core.component.Entity;
import org.debezium.core.component.EntityId;
import org.debezium.core.component.EntityType;
import org.debezium.core.component.Identifier;
import org.debezium.core.doc.Document;
import org.debezium.core.doc.Value;
import org.debezium.core.message.Batch;
import org.debezium.core.message.Patch;
import org.debezium.core.message.Patch.Action;
import org.debezium.core.message.Patch.Editor;
import org.debezium.driver.Debezium.BatchBuilder;

/**
 * Logic for generating large numbers of {@link Batch batches} that create, modified, and delete entities based upon
 * random selections of field names and values.
 * 
 * @author Randall Hauch
 */
public final class RandomContent {

    private static final AtomicLong GENERATED_ID = new AtomicLong(0);

    public static interface IdGenerator {
        EntityId[] generateEditableIds();

        EntityId[] generateRemovableIds();
    }

    /**
     * A generator of batch content.
     */
    public interface ContentGenerator {
        /**
         * Generate a batch that edits the entities with the supplied IDs.
         * 
         * @param editedIds the identifiers of the entities to be modified/created
         * @param removedIds the identifiers of the entities to be deleted
         * @return the batch; never null
         */
        public Batch<EntityId> generateBatch(EntityId[] editedIds, EntityId[] removedIds);

        /**
         * Generate a batch that edits and removes a random number of entities.
         * 
         * @param idGenerator the {@link IdGenerator} instance that should be used to randomly generate IDs
         * @return the batch; never null
         */
        default public Batch<EntityId> generateBatch(IdGenerator idGenerator) {
            if (idGenerator == null) return generateBatch(null, null);
            return generateBatch(idGenerator.generateEditableIds(), idGenerator.generateRemovableIds());
        }

        /**
         * Generate an entity representation.
         * 
         * @param id the identifier of the entity; may not be null
         * @return the entity representation; never null
         */
        default public Entity generateEntity(EntityId id) {
            Batch<EntityId> batch = generateBatch(new EntityId[] { id }, null);
            Patch<EntityId> patch = batch.patch(0);
            Document doc = Document.create();
            patch.apply(doc, (op) -> {
            });
            return Entity.with(id, doc);
        }

        /**
         * Add the specified number of edits and specified number of removes to the given {@link BatchBuilder}.
         * 
         * @param builder the batch builder; never null
         * @param numEdits the number of edits (including creates); must be greater than or equal to 0
         * @param numRemoves the number of removes; must be greater than or equal to 0
         * @param type the type of entity to create; may not be null
         * @return the supplied batch builder; never null
         */
        default public BatchBuilder addToBatch(BatchBuilder builder, int numEdits, int numRemoves, EntityType type) {
            IdGenerator generator = generateIds(numEdits,numRemoves,type);
            generateBatch(generator).forEach(builder::changeEntity);
            return builder;
        }
    }
    
    private static IdGenerator generateIds(int editCount, int removeCount, EntityType type) {
        return new IdGenerator() {
            @Override
            public EntityId[] generateEditableIds() {
                return generateIds(editCount, type);
            }

            @Override
            public EntityId[] generateRemovableIds() {
                return generateIds(removeCount, type);
            }

            private EntityId[] generateIds(int count, EntityType type) {
                if (count <= 0) return null;
                EntityId[] ids = new EntityId[count];
                for (int i = 0; i != count; ++i) {
                    ids[i] = Identifier.of(type, Long.toString(GENERATED_ID.incrementAndGet()));
                }
                return ids;
            }
        };
    }

    /**
     * Load the random content data from the {@code load-data.txt} file on the classpath.
     * 
     * @return the random content data; never null
     */
    public static RandomContent load() {
        try {
            return load(RandomContent.class.getClassLoader().getResource("load-data.txt").toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Load the random content data from the file at the specified URI.
     * 
     * @param uri the URI of the data file; may not be null
     * @return the random content data; never null
     */
    public static RandomContent load(URI uri) {
        return load(Paths.get(uri));
    }

    /**
     * Load the random content data from the file at the specified Path.
     * 
     * @param path the path of the data file; may not be null
     * @return the random content data; never null
     */
    public static RandomContent load(Path path) {
        Reader reader = new Reader();
        try (Stream<String> lines = Files.lines(path)) {
            lines.forEach(reader::process);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new RandomContent(reader);
    }

    private static class Reader {
        private List<FieldValues> fieldValues = new ArrayList<>();
        private FieldValues current = null;

        public void process(String line) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                current = null;
            } else {
                if (current != null && Character.isWhitespace(line.charAt(0))) {
                    current.addValue(trimmed);
                } else {
                    current = new FieldValues(trimmed);
                    fieldValues.add(current);
                }
            }
        }

        public FieldValues[] content() {
            return fieldValues.stream().toArray(FieldValues[]::new);
        }
    }

    private static class FieldValues {
        public final String fieldName;
        private final List<String> values = new ArrayList<>();

        FieldValues(String fieldName) {
            this.fieldName = fieldName;
        }

        void addValue(String value) {
            this.values.add(value);
        }

        String get(Random rng) {
            return values.get(rng.nextInt(values.size()));
        }
    }

    protected final FieldValues[] values;
    private volatile int generated;
    private final long initialSeed = System.currentTimeMillis();
    private final Random fieldCountSelector = new Random(initialSeed);
    private final int minFields;
    private final int maxFields;
    private final Action[] actions = new Action[] {
            Action.ADD, Action.ADD, Action.ADD, Action.ADD, Action.ADD,
            Action.ADD, Action.ADD, Action.ADD, Action.ADD, Action.ADD,
            Action.ADD, Action.ADD, Action.ADD, Action.ADD, Action.ADD,
            Action.REMOVE, Action.REMOVE,
            // Action.COPY,
            // Action.MOVE,
            // Action.REPLACE
    };
    private final int numValues;
    private final int numActions = actions.length;

    private RandomContent(Reader reader) {
        this(reader, 4, 6);
    }

    private RandomContent(Reader reader, int minFields, int maxFields) {
        this.values = reader.content();
        this.maxFields = Math.min(maxFields, this.values.length);
        this.minFields = Math.min(minFields, this.maxFields);
        assert this.maxFields > 0;
        assert this.minFields > 0;
        assert this.values != null;
        this.numValues = this.values.length;
    }

    /**
     * Create a new content generator. Generators are not thread-safe, so this method should be called to create a new
     * content generator for each thread.
     * 
     * @return the new content generator; never null
     */
    public ContentGenerator createGenerator() {
        ++generated;
        int fieldCount = minFields + fieldCountSelector.nextInt(maxFields - minFields);
        return new ContentGenerator() {
            Random rng = new Random(initialSeed * generated);

            @Override
            public Batch<EntityId> generateBatch(EntityId[] editedIds, EntityId[] removedIds) {
                Batch.Builder<EntityId> builder = Batch.entities();
                if (editedIds != null) {
                    for (int j = 0; j != editedIds.length; ++j) {
                        Editor<Batch.Builder<EntityId>> editor = builder.edit(editedIds[j]);
                        for (int i = 0; i != fieldCount; ++i) {
                            FieldValues vals = randomField();
                            String fieldName = vals.fieldName;
                            switch (actions[rng.nextInt(numActions)]) {
                                case ADD:
                                    editor.add(fieldName, Value.create(vals.get(rng)));
                                    break;
                                case REMOVE:
                                    editor.remove(fieldName);
                                    break;
                                case COPY:
                                    editor.copy(fieldName, randomField().fieldName);
                                    break;
                                case MOVE:
                                    editor.move(fieldName, randomField().fieldName);
                                    break;
                                case REPLACE:
                                    editor.replace(fieldName, Value.create(vals.get(rng)));
                                    break;
                                case INCREMENT:
                                    editor.increment(fieldName, 1);
                                    break;
                                case REQUIRE:
                                    editor.require(fieldName, Value.create(vals.get(rng)));
                                    break;
                            }
                        }
                        editor.end();
                    }
                }
                if (removedIds != null) {
                    for (int j = 0; j != removedIds.length; ++j) {
                        builder.remove(removedIds[j]);
                    }
                }
                return builder.build();
            }

            private FieldValues randomField() {
                return values[rng.nextInt(numValues)];
            }
        };
    }

}
