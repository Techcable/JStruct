package net.techcable.jstruct.visitor;

import com.google.common.base.Preconditions;
import net.techcable.jstruct.StructManager;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.sql.Struct;

public class PassByValueVisitor extends ClassVisitor {
    private final StructManager structManager;
    public PassByValueVisitor(StructManager structManager, ClassVisitor cv) {
        super(Opcodes.ASM5, cv);

        this.structManager = structManager;
    }
    private Type classType;

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        classType = Type.getObjectType(name);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {;
        // Ordering PassByValueMethodVisitor -> LocalVariablesSorter -> AnalyzerAdapter -> next visitor
        MethodVisitor nextVisitor = null;
        if (super.cv != null) {
            nextVisitor = super.cv.visitMethod(access, name, desc, signature, exceptions);
        }
        AnalyzerAdapter analyzer = new AnalyzerAdapter(classType.getInternalName(), access, name, desc, nextVisitor);
        LocalVariablesSorter localVariables = new LocalVariablesSorter(access, desc, analyzer);
        return new PassByValueMethodVisitor(structManager, classType, localVariables, localVariables, analyzer);
    }
}
