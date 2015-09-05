package net.techcable.jstruct;

import static org.objectweb.asm.Opcodes.*;

public class ASMUtils {
    private ASMUtils() {}

    public static boolean isFinal(int access) {
        return (access & ACC_FINAL) == ACC_FINAL;
    }
}
