/*
 * Copyright 2019, Perfect Sense, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gyro.core.reference;

import java.util.List;
import java.util.Map;

import gyro.core.GyroException;
import gyro.core.Type;
import gyro.core.finder.Finder;
import gyro.core.finder.FinderSettings;
import gyro.core.finder.FinderType;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.Resource;
import gyro.core.scope.Scope;

@Type("external-query")
public class FinderReferenceResolver extends ReferenceResolver {

    @Override
    public Object resolve(Scope scope, List<Object> arguments) {
        String type = (String) arguments.remove(0);

        Class<? extends Finder<Resource>> finderClass = scope.getRootScope()
            .getSettings(FinderSettings.class)
            .getFinderClasses()
            .get(type);

        if (finderClass == null) {
            throw new GyroException(String.format(
                "@|bold %s|@ type doesn't support external queries!",
                type));
        }

        Finder<Resource> finder = FinderType.getInstance(finderClass).newInstance(scope);
        List<Resource> resources = null;

        if (!arguments.isEmpty()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> filters = (Map<String, Object>) arguments.remove(0);

            if (!filters.isEmpty()) {
                resources = finder.find(filters);
            }
        }

        if (resources == null) {
            resources = finder.findAll();
        }

        resources.forEach(r -> DiffableInternals.update(r, false));

        return resources;
    }

}
