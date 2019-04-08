package gyro.core.diff;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import gyro.core.BeamUI;
import gyro.lang.Resource;
import gyro.lang.ast.PairNode;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.KeyBlockNode;
import gyro.lang.ast.scope.DiffableScope;
import gyro.lang.ast.value.BooleanNode;
import gyro.lang.ast.value.ListNode;
import gyro.lang.ast.value.MapNode;
import gyro.lang.ast.value.NumberNode;
import gyro.lang.ast.value.LiteralStringNode;
import com.google.common.collect.ImmutableSet;

public abstract class Diffable {

    private DiffableScope scope;
    private Diffable parent;
    private Change change;
    private Set<String> configuredFields;

    public DiffableScope scope() {
        return scope;
    }

    public void scope(DiffableScope scope) {
        this.scope = scope;
    }

    public Diffable parent() {
        return parent;
    }

    public Diffable parent(Diffable parent) {
        this.parent = parent;
        return this;
    }

    public Resource parentResource() {
        for (Diffable d = parent(); d != null; d = d.parent()) {
            if (d instanceof Resource) {
                return (Resource) d;
            }
        }

        return null;
    }

    public Change change() {
        return change;
    }

    public void change(Change change) {
        this.change = change;
    }

    public Set<String> configuredFields() {
        return configuredFields != null ? configuredFields : Collections.emptySet();
    }

    public void initialize(Map<String, Object> values) {
        if (configuredFields == null) {

            // Current state contains an explicit list of configured fields
            // that were in the original diffable definition.
            @SuppressWarnings("unchecked")
            Collection<String> cf = (Collection<String>) values.get("_configured-fields");

            if (cf == null) {

                // Only save fields that are in the diffable definition and
                // exclude the ones that were copied from the current state.
                if (values instanceof DiffableScope) {
                    cf = ((DiffableScope) values).getAddedKeys();

                } else {
                    cf = values.keySet();
                }
            }

            configuredFields = ImmutableSet.copyOf(cf);
        }

        for (DiffableField field : DiffableType.getInstance(getClass()).getFields()) {
            String key = field.getBeamName();

            if (!values.containsKey(key)) {
                continue;
            }

            Class<?> itemClass = field.getItemClass();
            Object value = values.get(key);

            if (Diffable.class.isAssignableFrom(itemClass)) {
                @SuppressWarnings("unchecked")
                Class<? extends Diffable> diffableClass = (Class<? extends Diffable>) itemClass;

                if (value instanceof List) {
                    value = ((List<?>) value).stream()
                            .map(v -> toDiffable(key, diffableClass, v))
                            .collect(Collectors.toList());

                } else if (value instanceof DiffableScope) {
                    value = toDiffable(key, diffableClass, value);
                }
            }

            field.setValue(this, value);
        }
    }

    private Object toDiffable(String key, Class<? extends Diffable> diffableClass, Object object) {
        if (!(object instanceof DiffableScope)) {
            return object;
        }

        DiffableScope scope = (DiffableScope) object;
        Diffable diffable;

        try {
            diffable = diffableClass.getConstructor().newInstance();

        } catch (IllegalAccessException
                | InstantiationException
                | NoSuchMethodException error) {

            throw new IllegalStateException(error);

        } catch (InvocationTargetException error) {
            Throwable cause = error.getCause();

            throw cause instanceof RuntimeException
                    ? (RuntimeException) cause
                    : new RuntimeException(cause);
        }

        if (diffable instanceof Resource) {
            ((Resource) diffable).resourceType(key);
        }

        diffable.scope(scope);
        diffable.parent(this);
        diffable.initialize(scope);

        return diffable;
    }

    public abstract String primaryKey();

    public abstract String toDisplayString();

    public boolean writePlan(BeamUI ui, Change change) {
        return false;
    }

    public boolean writeExecution(BeamUI ui, Change change) {
        return false;
    }

    public List<Node> toBodyNodes() {
        List<Node> body = new ArrayList<>();

        if (configuredFields != null) {
            body.add(new PairNode("_configured-fields",
                    new ListNode(configuredFields.stream()
                            .map(LiteralStringNode::new)
                            .collect(Collectors.toList()))));
        }

        for (DiffableField field : DiffableType.getInstance(getClass()).getFields()) {
            Object value = field.getValue(this);

            if (value == null) {
                continue;
            }

            String key = field.getBeamName();

            if (value instanceof Boolean) {
                body.add(new PairNode(key, new BooleanNode(Boolean.TRUE.equals(value))));

            } else if (value instanceof Date) {
                body.add(new PairNode(key, new LiteralStringNode(value.toString())));

            } else if (value instanceof Diffable) {
                body.add(new KeyBlockNode(key, ((Diffable) value).toBodyNodes()));

            } else if (value instanceof List) {
                if (Diffable.class.isAssignableFrom(field.getItemClass())) {
                    for (Object item : (List<?>) value) {
                        body.add(new KeyBlockNode(key, ((Diffable) item).toBodyNodes()));
                    }

                } else {
                    body.add(new PairNode(key, toNode(value)));
                }

            } else if (value instanceof Map) {
                body.add(new PairNode(key, toNode(value)));

            } else if (value instanceof Number) {
                body.add(new PairNode(key, new NumberNode((Number) value)));

            } else if (value instanceof String) {
                body.add(new PairNode(key, new LiteralStringNode((String) value)));

            } else {
                throw new UnsupportedOperationException(String.format(
                        "Can't convert an instance of [%s] into a node!",
                        value.getClass().getName()));
            }
        }

        return body;
    }

    private Node toNode(Object value) {
        if (value instanceof Boolean) {
            return new BooleanNode(Boolean.TRUE.equals(value));

        } else if (value instanceof List) {
            List<Node> items = new ArrayList<>();

            for (Object item : (List<?>) value) {
                if (item != null) {
                    items.add(toNode(item));
                }
            }

            return new ListNode(items);

        } else if (value instanceof Map) {
            List<PairNode> entries = new ArrayList<>();

            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                Object v = entry.getValue();

                if (v != null) {
                    entries.add(new PairNode((String) entry.getKey(), toNode(v)));
                }
            }

            return new MapNode(entries);

        } else if (value instanceof Number) {
            return new NumberNode((Number) value);

        } else if (value instanceof String) {
            return new LiteralStringNode((String) value);

        } else {
            throw new UnsupportedOperationException(String.format(
                    "Can't convert an instance of [%s] into a node!",
                    value.getClass().getName()));
        }
    }

}
