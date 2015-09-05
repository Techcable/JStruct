package net.techcable.jstruct.loader;

public interface ClassTransformer {
    public byte[] transform(String name, byte[] bytes);
}
