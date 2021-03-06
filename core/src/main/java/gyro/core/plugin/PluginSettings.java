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

package gyro.core.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import gyro.core.GyroException;
import gyro.core.Reflections;
import gyro.core.scope.RootScope;
import gyro.core.scope.Settings;

public class PluginSettings extends Settings {

    private List<Plugin> plugins;
    private List<Class<?>> otherClasses;

    private final LoadingCache<Plugin, LoadingCache<Class<?>, Boolean>> call = CacheBuilder.newBuilder()
        .build(new CacheLoader<Plugin, LoadingCache<Class<?>, Boolean>>() {

            @Override
            public LoadingCache<Class<?>, Boolean> load(Plugin plugin) {
                return CacheBuilder.newBuilder()
                    .build(new CacheLoader<Class<?>, Boolean>() {

                        @Override
                        public Boolean load(Class<?> otherClass) throws Exception {
                            plugin.onEachClass((RootScope) getScope(), otherClass);
                            return Boolean.TRUE;
                        }
                    });
            }
        });

    public List<Plugin> getPlugins() {
        if (plugins == null) {
            plugins = new ArrayList<>();
        }

        return plugins;
    }

    public void setPlugins(List<Plugin> plugins) {
        this.plugins = plugins;
    }

    public List<Class<?>> getOtherClasses() {
        if (otherClasses == null) {
            otherClasses = new ArrayList<>();
        }

        return otherClasses;
    }

    public void setOtherClasses(List<Class<?>> otherClasses) {
        this.otherClasses = otherClasses;
    }

    public void addClasses(Set<Class<?>> classes) {
        List<Plugin> plugins = getPlugins();
        List<Class<?>> otherClasses = getOtherClasses();

        for (Class<?> c : classes) {
            if (Plugin.class.isAssignableFrom(c)) {
                plugins.add((Plugin) Reflections.newInstance(c));

            } else {
                otherClasses.add(c);
            }
        }

        for (Plugin plugin : plugins) {
            for (Class<?> otherClass : otherClasses) {
                try {
                    call.get(plugin).get(otherClass);

                } catch (ExecutionException error) {
                    throw new GyroException(
                        String.format(
                            "Can't load @|bold %s|@ using the @|bold %s|@ plugin!",
                            otherClass.getName(),
                            plugin.getClass().getName()),
                        error.getCause());
                }
            }
        }
    }

}
