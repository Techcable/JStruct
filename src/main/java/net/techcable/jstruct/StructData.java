package net.techcable.jstruct;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import java.util.List;

@RequiredArgsConstructor
@Getter
public class StructData {
    private final Type type;
    private final ImmutableList<FieldStructData> fieldTypes;

    public StructData(ClassNode node) {
        this.type = Type.getObjectType(node.name);
        ImmutableList.Builder<FieldStructData> fieldTypes = ImmutableList.builder();
        for (FieldNode field : ((List<FieldNode>)node.fields)) {
            FieldStructData fieldData = new FieldStructData(field);
            fieldTypes.add(fieldData);
        }
        this.fieldTypes = fieldTypes.build();
    }

    @Getter
    @RequiredArgsConstructor
    public static class FieldStructData {
        private final Type fieldType;
        private final String name;

        public FieldStructData(FieldNode node) {
            this(Type.getType(node.desc), node.name);
        }
    }
}
