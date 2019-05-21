package gyro.core.finder;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import gyro.core.NamespaceUtils;
import gyro.core.resource.ResourceType;
import gyro.core.resource.Scope;

public class FinderType<F extends Finder> {

    private static final LoadingCache<Class<? extends Finder>, FinderType<? extends Finder>> INSTANCES = CacheBuilder
            .newBuilder()
            .build(new CacheLoader<Class<? extends Finder>, FinderType<? extends Finder>>() {

                @Override
                public FinderType<? extends Finder> load(Class<? extends Finder> finderClass) throws IntrospectionException {
                    return new FinderType<>(finderClass);
                }
            });

    private final Class<F> finderClass;
    private final String name;
    private final List<FinderField> fields;
    private final Map<String, FinderField> fieldByJavaName;
    private final Map<String, FinderField> fieldByGyroName;

    @SuppressWarnings("unchecked")
    public static <F extends Finder> FinderType<F> getInstance(Class<F> finderClass) {
        try {
            return (FinderType<F>) INSTANCES.get(finderClass);

        } catch (ExecutionException error) {
            Throwable cause = error.getCause();

            throw cause instanceof RuntimeException
                    ? (RuntimeException) cause
                    : new RuntimeException(cause);
        }
    }

    private FinderType(Class<F> finderClass) throws IntrospectionException {
        this.finderClass = finderClass;
        this.name = NamespaceUtils.getNamespacePrefix(finderClass ) + finderClass.getAnnotation(ResourceType.class).value();

        ImmutableList.Builder<FinderField> fields = ImmutableList.builder();
        ImmutableMap.Builder<String, FinderField> fieldByJavaName = ImmutableMap.builder();
        ImmutableMap.Builder<String, FinderField> fieldByGyroName = ImmutableMap.builder();

        for (PropertyDescriptor prop : Introspector.getBeanInfo(finderClass).getPropertyDescriptors()) {
            Method getter = prop.getReadMethod();
            Method setter = prop.getWriteMethod();

            if (getter != null && setter != null) {
                Type getterType = getter.getGenericReturnType();
                Type setterType = setter.getGenericParameterTypes()[0];

                if (getterType.equals(setterType)) {
                    FinderField field = new FinderField(prop.getName(), getter, setter, getterType);

                    fields.add(field);
                    fieldByJavaName.put(field.getJavaName(), field);
                    fieldByGyroName.put(field.getGyroName(), field);
                }
            }
        }

        this.fields = fields.build();
        this.fieldByJavaName = fieldByJavaName.build();
        this.fieldByGyroName = fieldByGyroName.build();
    }

    public String getName() {
        return name;
    }

    public List<FinderField> getFields() {
        return fields;
    }

    public FinderField getFieldByJavaName(String javaName) {
        return fieldByJavaName.get(javaName);
    }

    public FinderField getFieldByGyroName(String gyroName) {
        return fieldByGyroName.get(gyroName);
    }

    public F newInstance(Scope scope) {
        F finder;

        try {
            finder = finderClass.newInstance();

        } catch (IllegalAccessException | InstantiationException error) {
            throw new RuntimeException(error);
        }

        finder.scope = scope;

        return finder;
    }

}
