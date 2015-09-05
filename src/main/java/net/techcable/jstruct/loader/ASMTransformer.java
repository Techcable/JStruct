package net.techcable.jstruct.loader;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

public abstract class ASMTransformer implements ClassTransformer {

    @Override
    public byte[] transform(String name, byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        Type type = Type.getObjectType(name.replace('.', '/'));
        ClassVisitor transformer = newTransformer(type, writer);
        if (transformer != null && transformer != writer) {
            reader.accept(transformer, ClassReader.EXPAND_FRAMES);
            bytes = writer.toByteArray();
        }
        return bytes;
    }

    /**
     * Create a visitor that will properly transform the classes
     *
     * @param type the type of the class to transform
     * @param out the visitor that will write the classes
     * @return the visitor to read into
     */
    public abstract ClassVisitor newTransformer(Type type, ClassVisitor out);
}
