package com.jetbrains.jetpad.vclang.serialization;

import com.jetbrains.jetpad.vclang.module.Root;
import com.jetbrains.jetpad.vclang.module.SerializableModuleID;
import com.jetbrains.jetpad.vclang.naming.Namespace;
import com.jetbrains.jetpad.vclang.naming.NamespaceMember;
import com.jetbrains.jetpad.vclang.naming.ResolvedName;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.TypedDependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.UntypedDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.pattern.PatternArgument;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModuleSerialization {
  public static final byte[] SIGNATURE = {'v', 'c', (byte) 0xb1, 0x0b};
  public static final int VERSION = 1;

  public static void writeFile(SerializableModuleID moduleID, File outputFile) throws IOException {
    Files.createDirectories(outputFile.getParentFile().toPath());
    writeStream(moduleID, new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile))));
  }

  public static void serializeResolvedName(DataOutputStream stream, ResolvedName rn) throws IOException {
   List<String> fPath = new ArrayList<>();
    for (; rn.getParent() != null; rn = rn.getParent()) {
      fPath.add(rn.getName());
    }
    Collections.reverse(fPath);
    stream.writeInt(fPath.size());
    for (String aPath : fPath) {
      stream.writeUTF(aPath);
    }
  }

  public static void writeStream(SerializableModuleID curModuleID, DataOutputStream stream) throws IOException {
    DefNamesIndices defNamesIndices = new DefNamesIndices();
    ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(byteArrayStream);
    SerializeVisitor visitor = new SerializeVisitor(defNamesIndices, byteArrayStream, dataStream);

    int errors = serializeDefinition(visitor, Root.getModule(curModuleID).definition);

    stream.write(SIGNATURE);
    stream.writeInt(VERSION);
    defNamesIndices.serializeHeader(stream, curModuleID);
    stream.writeInt(errors + visitor.getErrors());
    defNamesIndices.serialize(stream, curModuleID);
    byteArrayStream.writeTo(stream);
    stream.close();
  }

  public static int serializeNamespace(SerializeVisitor visitor, Namespace namespace) throws IOException {
    int errors = 0;
    int size = 0;
    for (NamespaceMember member : namespace.getMembers()) {
      if (member.getResolvedName().getParent() != namespace.getResolvedName() || !(member.definition instanceof Constructor) && !(member.definition instanceof ClassField) ) {
        ++size;
      }
    }

    visitor.getDataStream().writeInt(size);
    for (NamespaceMember member : namespace.getMembers()) {
      if (member.getResolvedName().getParent() != namespace.getResolvedName()) {
        visitor.getDataStream().writeBoolean(false);
        visitor.getDataStream().writeInt(visitor.getDefinitionsIndices().getDefNameIndex(member.definition.getResolvedName()));
      } else if (!(member.definition instanceof ClassField) && !(member.definition instanceof Constructor)) {
        visitor.getDataStream().writeBoolean(true);
        errors += serializeDefinition(visitor, member.definition);
      }
    }
    return errors;
  }

  public static int serializeDefinition(SerializeVisitor visitor, Definition definition) throws IOException {
    visitor.getDataStream().writeInt(visitor.getDefinitionsIndices().getDefNameIndex(definition.getResolvedName()));
    visitor.getDataStream().writeBoolean(definition.getThisClass() != null);
    if (definition.getThisClass() != null)
      visitor.getDataStream().writeInt(visitor.getDefinitionsIndices().getDefNameIndex(definition.getThisClass().getResolvedName()));
    visitor.getDataStream().writeBoolean(definition.hasErrors());

    if (definition instanceof FunctionDefinition) {
      return serializeFunctionDefinition(visitor, (FunctionDefinition) definition);
    } else
    if (definition instanceof DataDefinition) {
      return serializeDataDefinition(visitor, (DataDefinition) definition);
    } else
    if (definition instanceof ClassDefinition) {
      return serializeClassDefinition(visitor, (ClassDefinition) definition);
    } else {
        throw new IllegalStateException();
    }
  }

  private static int serializeDataDefinition(SerializeVisitor visitor, DataDefinition definition) throws IOException {
    int errors = definition.hasErrors() ? 1 : 0;
    if (!definition.hasErrors()) {
      writeUniverse(visitor, definition.getUniverse());
      writeParameters(visitor, definition.getParameters());
    }

    visitor.getDataStream().writeInt(definition.getConstructors().size());
    for (Constructor constructor : definition.getConstructors()) {
      visitor.getDataStream().writeInt(visitor.getDefinitionsIndices().getDefNameIndex(constructor.getResolvedName()));
      visitor.getDataStream().writeBoolean(constructor.hasErrors());
      if (!constructor.hasErrors()) {
        visitor.getDataStream().writeBoolean(constructor.getPatterns() != null);
        if (constructor.getPatterns() != null) {
          writeParameters(visitor, constructor.getPatterns().getParameters());
          visitor.getDataStream().writeInt(constructor.getPatterns().getPatterns().size());
          for (PatternArgument patternArg : constructor.getPatterns().getPatterns()) {
            visitor.visitPatternArg(patternArg);
          }
        }
        writeUniverse(visitor, constructor.getUniverse());
        writeParameters(visitor, constructor.getParameters());
      } else {
        errors += 1;
      }
    }

    visitor.getDataStream().writeInt(definition.getConditions().size());
    for (Condition condition : definition.getConditions()) {
      visitor.getDataStream().writeInt(visitor.getDefinitionsIndices().getDefNameIndex(condition.getConstructor().getResolvedName()));
      condition.getElimTree().accept(visitor, null);
    }
    return errors;
  }

  public enum DefinitionCodes {
    FUNCTION_CODE {
      @Override
      FunctionDefinition toDefinition(ResolvedName rn, Abstract.Definition.Precedence precedence) {
        return new FunctionDefinition(rn, precedence);
      }
    },
    DATA_CODE {
      @Override
      DataDefinition toDefinition(ResolvedName rn, Abstract.Definition.Precedence precedence) {
        return new DataDefinition(rn, precedence);
      }
    },
    CLASS_CODE {
      @Override
      ClassDefinition toDefinition(ResolvedName rn, Abstract.Definition.Precedence precedence) {
        return new ClassDefinition(rn);
      }
    },
    CONSTRUCTOR_CODE {
      @Override
      Constructor toDefinition(ResolvedName rn, Abstract.Definition.Precedence precedence) {
        return new Constructor(rn, precedence, null);
      }
    },
    CLASS_FIELD_CODE {
      @Override
      ClassField toDefinition(ResolvedName rn, Abstract.Definition.Precedence precedence) {
        return new ClassField(rn, precedence, null, null, null);
      }
    };

    abstract Definition toDefinition(ResolvedName rn, Abstract.Definition.Precedence precedence);

    public static DefinitionCodes getDefinitionCode(Definition definition) {
      if (definition instanceof FunctionDefinition) return FUNCTION_CODE;
      if (definition instanceof DataDefinition) return DATA_CODE;
      if (definition instanceof ClassDefinition) return CLASS_CODE;
      if (definition instanceof Constructor) return CONSTRUCTOR_CODE;
      if (definition instanceof ClassField) return CLASS_FIELD_CODE;
      throw new IllegalStateException();
    }
  }

  private static int serializeClassDefinition(SerializeVisitor visitor, ClassDefinition definition) throws IOException {
    int errors = serializeNamespace(visitor, definition.getResolvedName().toNamespace());

    writeUniverse(visitor, definition.getUniverse());

    visitor.getDataStream().writeInt(definition.getFields().size());
    for (ClassField field : definition.getFields()) {
      visitor.getDataStream().writeInt(visitor.getDefinitionsIndices().getDefNameIndex(field.getResolvedName()));
      writeParameters(visitor, field.getThisParameter());
      visitor.getDataStream().writeBoolean(field.hasErrors());
      if (!field.hasErrors()) {
        writeUniverse(visitor, field.getUniverse());
        field.getType().accept(visitor, null);
      }
    }

    return errors;
  }

  private static int serializeFunctionDefinition(SerializeVisitor visitor, FunctionDefinition definition) throws IOException {
    int errors = definition.hasErrors() ? 1 : 0;

    serializeNamespace(visitor, definition.getStaticNamespace());

    visitor.getDataStream().writeBoolean(definition.typeHasErrors());
    if (!definition.typeHasErrors()) {
      writeParameters(visitor, definition.getParameters());
      definition.getResultType().accept(visitor, null);
      visitor.getDataStream().writeBoolean(!definition.hasErrors() && definition.getElimTree() != null);
      if (definition.getElimTree() != null) {
        definition.getElimTree().accept(visitor, null);
      }
    }

    return errors;
  }

  public static void writeDefinition(DataOutputStream stream, Definition definition) throws IOException {
    stream.writeInt(DefinitionCodes.getDefinitionCode(definition).ordinal());
    if (!(definition instanceof ClassDefinition)) {
      stream.write(definition.getPrecedence().associativity == Abstract.Binding.Associativity.LEFT_ASSOC ? 0 : definition.getPrecedence().associativity == Abstract.Binding.Associativity.RIGHT_ASSOC ? 1 : 2);
      stream.writeByte(definition.getPrecedence().priority);
    }
  }

  public static void writeUniverse(SerializeVisitor visitor, TypeUniverse universe) throws IOException {
    universe.getPLevel().accept(visitor, null);
    universe.getHLevel().accept(visitor, null);
  }

  private static void writeString(SerializeVisitor visitor, String str) throws IOException {
    visitor.getDataStream().writeUTF(str == null ? "" : str);
  }

  public static void writeParameters(SerializeVisitor visitor, DependentLink link) throws IOException {
    for (; link.hasNext(); link = link.getNext()) {
      if (link instanceof TypedDependentLink) {
        visitor.getDataStream().write(1);
        visitor.getDataStream().writeBoolean(link.isExplicit());
        writeString(visitor, link.getName());
        link.getType().accept(visitor, null);
      } else
      if (link instanceof UntypedDependentLink) {
        visitor.getDataStream().write(2);
        writeString(visitor, link.getName());
      } else {
        throw new IllegalStateException();
      }

      visitor.addBinding(link);
    }
    visitor.getDataStream().write(0);
  }

  public static void writeTypedBinding(SerializeVisitor visitor, Binding binding) throws IOException {
    visitor.getDataStream().writeBoolean(binding.getName() != null);
    if (binding.getName() != null)
      visitor.getDataStream().writeUTF(binding.getName());
    binding.getType().accept(visitor, null);
    visitor.addBinding(binding);
  }
}
