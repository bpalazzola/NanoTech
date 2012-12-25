package me.main__.nanotech.dirty;

import java.util.HashMap;
import java.util.Map;

public class InterceptingClassLoader extends ClassLoader {
    private final ClassLoader parent;
    private final Map<String, Class<?>> bypassMap;

    public InterceptingClassLoader(final ClassLoader parent, final Class<?>... bypasses) {
        super(parent);
        this.parent = parent;
        this.bypassMap = new HashMap<String, Class<?>>();
        for (Class<?> c : bypasses)
            bypassMap.put(c.getName(), c);
    }

    @Override
    public Class<?> loadClass(final String name) throws ClassNotFoundException {
        return findClass(name);
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        if (bypassMap.containsKey(name))
            return bypassMap.get(name);

        return parent.loadClass(name);
    }
}
