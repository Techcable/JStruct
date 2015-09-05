package net.techcable.jstruct.visitor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

public class JStructVisitor extends ClassVisitor {

    public JStructVisitor(ClassVisitor visitor) {
        super(Opcodes.ASM5, visitor);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        access |= Opcodes.ACC_PUBLIC; // Make it public so we can get it later with
        return super.visitField(access, name, desc, signature, value);
    }
}
