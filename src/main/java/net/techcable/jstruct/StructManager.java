package net.techcable.jstruct;

import com.google.common.base.Preconditions;
import net.techcable.jstruct.loader.ASMTransformer;
import net.techcable.jstruct.loader.TransformingClassLoader;
import net.techcable.jstruct.visitor.JStructVisitor;
import net.techcable.jstruct.visitor.PassByValueVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import java.net.URL;
import java.util.*;

import static net.techcable.jstruct.ASMUtils.*;

public class StructManager {
    private final Map<String, StructData> structs = new HashMap<>();
    /* We don't recursively pass by values
     * Therefore it is only necicary to make sure all fields are final, as mutable objects will be passed by reference
    private final Set<Type> knownImmutables = new HashSet<>();

    private void addImmutable(Class<?> c) {
        if (c.isPrimitive()) addImmutable(Primitives.wrap(c)); // Add the wrapper type to
        Type type = Type.getType(c);
        knownImmutables.add(type);
    }

    {
        // Primitive Types (automatically adds wrappers)
        addImmutable(boolean.class);
        addImmutable(char.class);
        addImmutable(byte.class);
        addImmutable(short.class);
        addImmutable(int.class);
        addImmutable(long.class);
        addImmutable(float.class);
        addImmutable(double.class);

        // JDK Immutables
        addImmutable(String.class);

        // Guava Immutables
        addImmutable(ImmutableCollection.class);
        addImmutable(ImmutableSet.class);
        addImmutable(ImmutableList.class);
        addImmutable(ImmutableMap.class);
        addImmutable(ImmutableMultimap.class);
    }
    */

    public void addStruct(ClassNode struct) {
        assertImmutable(struct);
        Type structType = Type.getObjectType(struct.name);
        StructData structData = new StructData(struct);
        structs.put(structType.getInternalName(), structData);
    }

    public boolean isStruct(Type type) {
        return getStruct(type) != null;
    }

    public StructData getStruct(Type type) {
        if (type == null) return null;
        String internalName = type.getInternalName();
        return structs.get(internalName);
    }

    public TransformingClassLoader newClassLoader(ClassLoader parent, URL... urls) {
        TransformingClassLoader loader = new TransformingClassLoader(parent, urls);
        loader.addTransformer("jstruct", new ASMTransformer() {
            @Override
            public ClassVisitor newTransformer(Type type, ClassVisitor out) {
                if (!isStruct(type)) return null;
                return new JStructVisitor(out);
            }
        });
        loader.addTransformer("pass by value", new ASMTransformer() {
            @Override
            public ClassVisitor newTransformer(Type type, ClassVisitor out) {
                return new PassByValueVisitor(StructManager.this, out);
            }
        });
        return loader;
    }

    public void assertImmutable(ClassNode struct) {
        Preconditions.checkArgument(isFinal(struct.access), "%s is not final", struct.name);
        for (FieldNode field : ((List<FieldNode>)struct.fields)) {
            Preconditions.checkArgument(isFinal(field.access), "Field %s in %s is not final", field.name, struct.name);
        }
    }

}
