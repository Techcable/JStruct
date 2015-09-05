package net.techcable.jstruct.visitor;

import net.techcable.jstruct.StructData;
import net.techcable.jstruct.StructManager;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.util.*;

public class PassByValueMethodVisitor extends InstructionAdapter {
    private final StructManager structManager;
    private final LocalVariablesSorter localVariables;
    private final AnalyzerAdapter analyzer;
    private final Type declaringType;
    public PassByValueMethodVisitor(StructManager structManager, Type declaringType, MethodVisitor parent, LocalVariablesSorter localVariables, AnalyzerAdapter analyzer) {
        super(parent);
        this.structManager = structManager;
        this.analyzer = analyzer;
        this.localVariables = localVariables;
        this.declaringType = declaringType;
    }

    @Override
    public void invokevirtual(String owner, String name, String desc, boolean itf) {
        String[] array = onInvoke(owner, name, desc);
        owner = array[0];
        name = array[1];
        desc = array[2];
        super.invokevirtual(owner, name, desc, itf);
    }

    @Override
    public void invokestatic(String owner, String name, String desc, boolean itf) {
        String[] array = onInvoke(owner, name, desc);
        owner = array[0];
        name = array[1];
        desc = array[2];
        super.invokestatic(owner, name, desc, itf);
    }

    @Override
    public void invokeinterface(String owner, String name, String desc) {
        String[] array = onInvoke(owner, name, desc);
        owner = array[0];
        name = array[1];
        desc = array[2];
        super.invokeinterface(owner, name, desc);
    }

    @Override
    public void invokespecial(String owner, String name, String desc, boolean itf) {
        String[] array = onInvoke(owner, name, desc);
        owner = array[0];
        name = array[1];
        desc = array[2];
        super.invokespecial(owner, name, desc, itf);
    }

    private String[] onInvoke(String owner, String name, String desc) {
        if (structManager.isStruct(getTopOfStack())) {
            // Do some sanity checks for structs
            if ("wait".equals(name) || "notify".equals(name) && Type.getInternalName(Object.class).equals(owner)) {
                throwNewException(UnsupportedOperationException.class, "Can't call " + name + " on a struct");
                return new String[] {owner, name, desc};
            }
        }
        Type methodType = Type.getMethodType(desc);
        for (Type structType : methodType.getArgumentTypes()) {
            if (!structManager.isStruct(structType)) continue;
            int structVar = localVariables.newLocal(structType); // Create a local variable which will hold the struct
            StructData structData = structManager.getStruct(structType);
            for (StructData.FieldStructData fieldData : structData.getFieldTypes()) {
                load(structVar, structType); // Load the struct onto the stack
                Type fieldType = fieldData.getFieldType();
                getfield(structType.getInternalName(), fieldData.getName(), fieldType.getDescriptor()); // Now get the field from the type
            }
            // Duplicate the fields on the stack
        }
        return new String[] {owner, name, desc};
    }

    @Override
    public void monitorenter() {
        if (structManager.isStruct(getTopOfStack())) throwNewException(UnsupportedOperationException.class, "Cant synchronize on a monitor");
    }

    @Override
    public void ificmpeq(Label label) {
        onCompare(label, true);
        super.ificmpeq(label);
    }

    @Override
    public void ificmpne(Label label) {
        onCompare(label, false);
        super.ificmpne(label);
    }

    private void onCompare(Label label, boolean equals) {
        Queue<Type> stack = getStack();
        boolean comparingStructs;
        Type t = stack.poll();
        comparingStructs = structManager.isStruct(t);
        t = stack.poll(); // Right below the top
        comparingStructs = comparingStructs || structManager.isStruct(t);
        if (comparingStructs) throwNewException(UnsupportedOperationException.class, "Can't compare structs");
    }


    private void throwNewException(Class<? extends Throwable> exceptionType, String msg) {
        throwNewException(Type.getType(exceptionType), msg);
    }

    private void throwNewException(Type exceptionType, String msg) {
        anew(exceptionType);
        Type desc = msg == null ? Type.getMethodType(Type.VOID_TYPE) :  Type.getMethodType(Type.VOID_TYPE, Type.getType(String.class));
        if (msg != null) {
            aconst(msg);
        }
        invokespecial(exceptionType.getInternalName(), "<init>", desc.getDescriptor(), false); // Call the constructor
        athrow();
    }

    private Type getTopOfStack() {
        return getStack().poll();
    }

    private Queue<Type> getStack() {
        List<Object> stackList = analyzer.stack;
        Queue<Object> objStack = new LinkedList<>();
        Collections.reverse(stackList); // bottom -> top, top -> bottom
        stackList.forEach(objStack::add); // Since it is a FIFO queue, adding the top first will cause the top to be last out
        Queue<Type> typeStack = new LinkedList<>();
        Object obj;
        while ((obj = objStack.poll()) != null) {
            Type type;
            if (obj instanceof Integer) {
                int id = ((Integer) obj);
                if (id == Opcodes.INTEGER) {
                    type = Type.INT_TYPE;
                } else if (id == Opcodes.FLOAT) {
                    type = Type.FLOAT_TYPE;
                } else if (id == Opcodes.LONG) {
                    type = Type.LONG_TYPE;
                } else if (id == Opcodes.DOUBLE) {
                    type = Type.DOUBLE_TYPE;
                } else if (id == Opcodes.NULL) {
                    type = null; //
                } else if (id == Opcodes.UNINITIALIZED_THIS) {
                    type = declaringType;
                } else continue; // Doesn't matter
            } else if (obj instanceof String) {
                String internalName = ((String) obj);
                type = Type.getObjectType(internalName);
            } else type = null; // Unknown type
            typeStack.add(type);
        }
        return typeStack;
    }
}
