package beam.core;

import beam.lang.BeamConfig;
import beam.lang.BeamContext;
import beam.lang.BeamContextKey;
import beam.lang.BeamList;
import beam.lang.BeamReference;
import beam.lang.BeamScalar;

public class BeamObject extends BeamValidatedConfig {

    private boolean added;

    @Override
    protected boolean resolve(BeamContext parent, BeamContext root) {
        boolean progress = false;
        String id = getParams().get(0).getValue().toString();
        BeamContextKey key = new BeamContextKey(id, getType());
        if (parent.hasKey(key)) {
            BeamConfig existingConfig = (BeamConfig) parent.getReferable(key);

            if (existingConfig.getClass() != this.getClass()) {
                parent.addReferable(key, this);
                progress = true;
            }
        } else {
            parent.addReferable(key, this);
            progress = true;
        }

        if (getScope().isEmpty()) {
            getScope().addAll(parent.getScope());
            getScope().add(key);
        }

        if (!added) {
            BeamContextKey fieldKey = new BeamContextKey(getType());
            if (parent.hasKey(fieldKey) && parent.getReferable(fieldKey) instanceof BeamList) {
                BeamList beamList = (BeamList) parent.getReferable(fieldKey);
                BeamReference reference = new BeamReference();
                reference.getScopeChain().addAll(getScope());
                BeamScalar scalar = new BeamScalar();
                scalar.getElements().add(reference);

                boolean found = false;
                for (BeamScalar beamScalar : beamList.getList()) {
                    if (beamScalar.getElements().size() == 1) {
                        if (reference.equals(beamScalar.getElements().get(0))) {
                            found = true;
                        }
                    }
                }

                if (!found) {
                    beamList.getList().add(scalar);
                }

            } else {
                BeamReference reference = new BeamReference();
                reference.getScopeChain().addAll(getScope());
                parent.addReferable(fieldKey, reference);
            }
            added = true;
        }

        return resolve(root) || progress;
    }

    @Override
    public boolean resolve(BeamContext context) {
        boolean progress = super.resolve(context);
        populate();

        return progress;
    }
}
