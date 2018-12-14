package beam.lang;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BeamScalar extends BeamValue {

    private List<BeamLiteral> elements;

    private Object value;

    public List<BeamLiteral> getElements() {
        if (elements == null) {
            elements = new ArrayList<>();
        }

        return elements;
    }

    public void setElements(List<BeamLiteral> elements) {
        this.elements = elements;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public boolean resolve(BeamContext context) {
        if (getElements().isEmpty()) {
            throw new BeamLangException("Unable to resolve scalar with zero elements!");
        }

        boolean progress = false;
        if (getElements().size() == 1) {
            BeamLiteral literal = getElements().get(0);
            progress = literal.resolve(context);

            if (literal.getValue() != null) {
                value = literal.getValue();
            }

            return progress;

        } else {
            if (value != null) {
                return false;
            }

            StringBuilder sb = new StringBuilder();
            for (BeamLiteral literal : getElements()) {
                progress = literal.resolve(context) || progress;

                if (literal.getValue() != null) {
                    Object resolvedLiteral = literal.getValue();

                    // Enforce string concat
                    if (!(resolvedLiteral instanceof String)) {
                        throw new BeamLangException(String.format("Illegal placement of %s in %s, expect a String", literal.getLiteral(), this));
                    } else {
                        sb.append(resolvedLiteral);
                    }
                } else {
                    return progress;
                }
            }

            value = sb.toString();
            return true;
        }
    }

    @Override
    public Set<BeamReference> getDependencies(BeamBlock config) {
        Set<BeamReference> dependencies = new HashSet<>();
        if (getValue() != null) {
            return dependencies;
        }

        if (getElements().size() == 1) {
            BeamLiteral literal = getElements().get(0);
            dependencies.addAll(literal.getDependencies(config));

        } else {
            for (BeamLiteral literal : getElements()) {
                dependencies.addAll(literal.getDependencies(config));
            }
        }

        return dependencies;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Object element : getElements()) {
            sb.append(element);
        }

        return sb.toString();
    }
}
