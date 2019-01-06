package beam.lang;

import beam.core.BeamCore;
import beam.core.BeamException;
import com.google.common.base.CaseFormat;
import org.apache.commons.beanutils.BeanUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ResourceNode extends ContainerNode {

    private String type;
    private String name;

    private Set<ResourceNode> dependencies;
    private Set<ResourceNode> dependents;
    private Map<String, List<ResourceNode>> subResources;
    private BeamCore core;

    public Set<ResourceNode> dependencies() {
        if (dependencies == null) {
            dependencies = new LinkedHashSet<>();
        }

        return dependencies;
    }

    public Set<ResourceNode> dependents() {
        if (dependents == null) {
            dependents = new LinkedHashSet<>();
        }

        return dependents;
    }

    public Map<String, List<ResourceNode>> subResources() {
        if (subResources == null) {
            subResources = new HashMap<>();
        }

        return subResources;
    }

    public String resourceType() {
        return type;
    }

    public void setResourceType(String type) {
        this.type = type;
    }

    public String resourceIdentifier() {
        return name;
    }

    public void setResourceIdentifier(String name) {
        this.name = name;
    }

    public ResourceKey resourceKey() {
        return new ResourceKey(resourceType(), resourceIdentifier());
    }

    @Override
    public boolean resolve() {
        boolean resolved = super.resolve();

        for (List<ResourceNode> resources : subResources().values()) {
            for (ResourceNode resource : resources) {
                if (!resource.resolve()) {
                    throw new BeamLanguageException("Unable to resolve configuration.", resource);
                }
            }
        }

        if (resolved) {
            syncInternalToProperties();
        }

        return resolved;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(resourceType()).append(" ");

        if (resourceIdentifier() != null) {
            sb.append(resourceIdentifier()).append("\n");
        } else {
            sb.append("\n");
        }

        sb.append(super.toString());

        sb.append("end\n\n");

        return sb.toString();
    }

    protected final void syncInternalToProperties() {
        for (String key : keys()) {
            Object value = get(key).getValue();

            try {
                String convertedKey = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, key);

                if (!BeanUtils.describe(this).containsKey(convertedKey)) {
                    ValueNode valueNode = get(key);
                    String message = String.format("invalid attribute '%s' found on line %s", key, valueNode.getLine());

                    throw new BeamException(message);
                }

                BeanUtils.setProperty(this, convertedKey, value);
            } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
                // Ignoring errors from setProperty
            }
        }

        for (String subResourceField : subResources().keySet()) {
            List<ResourceNode> subResources = subResources().get(subResourceField);

            try {
                BeanUtils.setProperty(this, subResourceField, subResources);
            } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
                // Ignoring errors from setProperty
                e.printStackTrace();
            }
        }
    }

    /**
     * `execute()` is called during the parsing of the configuration. This
     * allows extensions to perform any necessary actions to load themselves.
     */
    public void execute() {

    }

    final void executeInternal() {
        syncInternalToProperties();
        execute();
    }

    public BeamCore core() {
        return core;
    }

    public void setCore(BeamCore core) {
        this.core = core;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ResourceNode that = (ResourceNode) o;

        return Objects.equals(type, that.type) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name);
    }

}