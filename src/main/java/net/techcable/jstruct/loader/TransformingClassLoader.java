package net.techcable.jstruct.loader;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import sun.jvm.hotspot.utilities.Assert;
import sun.misc.Resource;
import sun.misc.URLClassPath;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.regex.Pattern;

public class TransformingClassLoader extends URLClassLoader {
    static {
        ClassLoader.registerAsParallelCapable();
    }

    public TransformingClassLoader(ClassLoader parent, URL[] urls) {
        super(urls, parent);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Preconditions.checkNotNull(name, "Null class name");
        if (isExcluded(name)) return super.findClass(name); // Let the super class handle finding the class
        byte[] bytes;
        try {
            bytes = loadBytes(name);
        } catch (IOException e) {
            throw new ClassNotFoundException("Unable to load " + name, e);
        }
        for (ClassTransformer transformer : getBakedTransformers().values()) {
            bytes = transformer.transform(name, bytes);
        }
        return defineClass(name, bytes, 0, bytes.length);
    }

    private byte[] loadBytes(String className) throws IOException {
        String path = className.replace('.', '/').concat(".class");
        Resource res = getClassPath().getResource(path, false);
        return res.getBytes();
    }

    // Superclass Accsess

    private static final Field ucpField;
    static {
        try {
            ucpField = URLClassLoader.class.getField("ucp");
        } catch (NoSuchFieldException e) {
            throw new UnsupportedClassVersionError("Unsupported URLClassLoader implementation");
        }
    }
    public URLClassPath getClassPath() {
        ucpField.setAccessible(true);
        try {
            return (URLClassPath) ucpField.get(this);
        } catch (IllegalAccessException e) {
            throw new AssertionError("Already called setAccessible", e);
        }
    }

    // Internal Collections

    private final Set<Pattern> transformerExceptions = Sets.newConcurrentHashSet();
    {
        transformerExceptions.add(Pattern.compile("net\\.techcable\\.jstruct.+"));
        transformerExceptions.add(Pattern.compile("javax?.+"));
        transformerExceptions.add(Pattern.compile("com\\.google\\.common.+"));
        transformerExceptions.add(Pattern.compile("org\\.objectweb\\.asm.+"));
    }

    private boolean isExcluded(String s) {
        for (Pattern p : transformerExceptions) {
            if (p.matcher(s).matches()) return true;
        }
        return false;
    }

    private final List<String> transformerIds = Collections.synchronizedList(new LinkedList<>());
    @Deprecated // Use getBakedTransformers();
    private volatile LinkedHashMap<String, ClassTransformer> bakedTransformers = new LinkedHashMap<>();
    private final Map<String, ClassTransformer> transformers = new HashMap<>();

    @SuppressWarnings("deprecation")
    private LinkedHashMap<String, ClassTransformer> getBakedTransformers() {
        LinkedHashMap<String, ClassTransformer> bakedTransformers = this.bakedTransformers;
        if (bakedTransformers != null) return bakedTransformers;
        synchronized (transformerIds) {
            bakedTransformers = new LinkedHashMap<>();
            for (String id : transformerIds) {
                ClassTransformer transformer = transformers.get(id);
                bakedTransformers.put(id, transformer);
            }
            this.bakedTransformers = bakedTransformers;
            return bakedTransformers;
        }
    }

    //
    // Public Methods
    //

    public void addTransformer(String id, ClassTransformer transformer) {
        addTransformerAt(id, transformer, Integer.MAX_VALUE);
    }

    public void addTransformerBefore(String id, ClassTransformer transformer, String before) {
        synchronized (transformerIds) {
            int beforeIndex = transformerIds.indexOf(before);
            if (beforeIndex < 0) throw new IllegalArgumentException(before + " not found");
            addTransformerAt(id, transformer, beforeIndex);
        }
    }


    public void addTransformerAfter(String id, ClassTransformer transformer, String after) {
        synchronized (transformerIds) {
            int afterIndex = transformerIds.indexOf(after);
            if (afterIndex < 0) throw new IllegalArgumentException(afterIndex + " not found");
            addTransformerAt(id, transformer, afterIndex + 1);
        }
    }

    public static final int MAX_TRANSFORMERS = Integer.MAX_VALUE << 1; // We want some space left over

    @SuppressWarnings("deprecation")
    private void addTransformerAt(String id, ClassTransformer transformer, int index) {
        synchronized (transformerIds) {
            bakedTransformers = null; //
            Preconditions.checkArgument(transformerIds.size() + 1 < MAX_TRANSFORMERS, "Can't have more than %s transformers", MAX_TRANSFORMERS);
            if (index < transformerIds.size()) {
                transformerIds.add(index, id);
            } else {
                transformerIds.add(id);
            }
            transformers.put(id, transformer);
        }
        Pattern pattern = Pattern.compile(transformer.getClass().getName(), Pattern.LITERAL);
        transformerExceptions.add(pattern);
    }

}
