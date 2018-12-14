package beam.core;

import beam.lang.BeamConfig;
import beam.lang.BeamContextKey;
import beam.lang.BeamResolvable;
import beam.lang.BeamValue;
import com.google.common.base.CaseFormat;
import org.apache.commons.beanutils.BeanUtils;

import java.lang.reflect.InvocationTargetException;

public class BeamValidatedConfig extends BeamConfig {

    public void populate() {
        String id = getParams().get(0).getValue().toString();
        for (BeamContextKey key : listContextKeys()) {
            if (key.getType() != null) {
                continue;
            }

            BeamResolvable referable = getReferable(key);
            Object value = referable.getValue();

            try {
                String keyId = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, key.getId());
                BeanUtils.setProperty(this, keyId, value);
            } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
                // Ignoring errors from setProperty
            }
        }
    }
}
