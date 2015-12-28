package com.jetbrains.jetpad.vclang.serialization;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.module.RootModule;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.NameArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.pattern.PatternArgument;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModuleSerialization {
  public static final byte[] SIGNATURE = {'v', 'c', (byte) 0xb1, 0x0b};
  public static final int VERSION = 1;

  public static void writeFile(ResolvedName resolvedName, File outputFile) throws IOException {
    Files.createDirectories(outputFile.getParentFile().toPath());
    writeStream(resolvedName, new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile))));
  }

  public static void serializeRelativeResolvedName(DataOutputStream stream, ResolvedName rn, ResolvedName module) throws IOException {
   List<String> fPath = new ArrayList<>();
    for (; !rn.equals(module); rn = rn.parent.getResolvedName()) {
      fPath.add(rn.name.name);
    }
    Collections.reverse(fPath);
    stream.writeInt(fPath.size());
    for (String aPath : fPath) {
      stream.writeUTF(aPath);
    }
  }
  public static void serializeResolvedName(DataOutputStream stream, ResolvedName rn) throws IOException {
    serializeRelativeResolvedName(stream, rn, RootModule.ROOT.getResolvedName());
  }

  public static void writeStream(ResolvedName module, DataOutputStream stream) throws IOException {
    assert module.toAbstractDefinition() != null && module.toDefinition() != null;
    assert module.toAbstractDefinition().getParentStatement() == null;

    DefNamesIndices defNamesIndices = new DefNamesIndices();
    ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(byteArrayStream);
    SerializeVisitor visitor = new SerializeVisitor(defNamesIndices, byteArrayStream, dataStream);

    int errors = serializeDefinition(visitor, module.toDefinition());

    stream.write(SIGNATURE);
    stream.writeInt(VERSION);
    defNamesIndices.serializeHeader(stream, module);
    stream.writeInt(errors + visitor.getErrors());
    defNamesIndices.serialize(stream, module);
    byteArrayStream.writeTo(stream);
    stream.close();
  }

  public static int serializeNamespace(SerializeVisitor visitor, Namespace namespace) throws IOException {
    int errors = 0;
    int size = 0;
    for (NamespaceMember member : namespace.getMembers()) {
      if (member.definition != null && (member.getResolvedName().parent != namespace || member.abstractDefinition.getParentStatement() != null && !(member.definition instanceof Constructor) && !(member.definition instanceof ClassField)) ) {
        ++size;
      }
    }
    visitor.getDataStream().writeInt(size);
    for (NamespaceMember member : namespace.getMembers()) {
      if (member.definition != null) {
        if (member.getResolvedName().parent != namespace) {
          visitor.getDataStream().writeBoolean(false);
          visitor.getDataStream().writeInt(visitor.getDefinitionsIndices().getDefNameIndex(member.definition.getResolvedName(), false));
        } else if (member.abstractDefinition.getParentStatement() != null && !(member.definition instanceof ClassField) && !(member.definition instanceof Constructor)) {
          visitor.getDataStream().writeBoolean(true);
          errors += serializeDefinition(visitor, member.definition);
        }
      }
    }
    return errors;
  }

  public static int serializeDefinition(SerializeVisitor visitor, Definition definition) throws IOException {
    visitor.getDataStream().writeInt(visitor.getDefinitionsIndices().getDefNameIndex(definition.getResolvedName(), true));
    visitor.getDataStream().writeBoolean(definition.getThisClass() != null);
    if (definition.getThisClass() != null)
      visitor.getDataStream().writeInt(visitor.getDefinitionsIndices().getDefNameIndex(definition.getThisClass().getResolvedName(), false));
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
      writeUniverse(visitor.getDataStream(), definition.getUniverse());
      writeArguments(visitor, definition.getParameters());
    }
    visitor.getDataStream().writeInt(definition.getConstructors().size());
    for (Constructor constructor : definition.getConstructors()) {
      visitor.getDataStream().writeInt(visitor.getDefinitionsIndices().getDefNameIndex(constructor.getResolvedName(), true));
      visitor.getDataStream().writeBoolean(constructor.hasErrors());
      if (!constructor.hasErrors()) {
        // TODO: serialization test for patterns
        visitor.getDataStream().writeBoolean(constructor.getPatterns() != null);
        if (constructor.getPatterns() != null) {
          visitor.getDataStream().writeInt(constructor.getPatterns().size());
          for (PatternArgument patternArg : constructor.getPatterns()) {
            visitor.visitPatternArg(patternArg);
          }
        }
        writeUniverse(visitor.getDataStream(), constructor.getUniverse());
        writeArguments(visitor, constructor.getArguments());
      } else {
        errors += 1;
      }
    }
    return errors;
  }

  public enum DefinitionCodes {
    FUNCTION_CODE {
      @Override
      FunctionDefinition toDefinition(Name name, Namespace parent, Abstract.Definition.Precedence precedence) {
        return new FunctionDefinition(parent, name, precedence);
      }
    },
    DATA_CODE {
      @Override
      DataDefinition toDefinition(Name name, Namespace parent, Abstract.Definition.Precedence precedence) {
        return new DataDefinition(parent, name, precedence);
      }
    },
    CLASS_CODE {
      @Override
      ClassDefinition toDefinition(Name name, Namespace parent, Abstract.Definition.Precedence precedence) {
        return new ClassDefinition(parent, name);
      }
    },
    CONSTRUCTOR_CODE {
      @Override
      Constructor toDefinition(Name name, Namespace parent, Abstract.Definition.Precedence precedence) {
        return new Constructor(parent, name, precedence, null);
      }
    },
    CLASS_FIELD_CODE {
      @Override
      ClassField toDefinition(Name name, Namespace parent, Abstract.Definition.Precedence precedence) {
        return new ClassField(parent, name, precedence, null, null);
      }
    };

    abstract Definition toDefinition(Name name, Namespace parent, Abstract.Definition.Precedence precedence);

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
    int errors = serializeNamespace(visitor, definition.getParentNamespace().getChild(definition.getName()));

    writeUniverse(visitor.getDataStream(), definition.getUniverse());

    visitor.getDataStream().writeInt(definition.getFields().size());
    for (ClassField field : definition.getFields()) {
      visitor.getDataStream().writeInt(visitor.getDefinitionsIndices().getDefNameIndex(field.getResolvedName(), true));
      visitor.getDataStream().writeBoolean(field.hasErrors());
      if (!field.hasErrors()) {
        writeUniverse(visitor.getDataStream(), field.getUniverse());
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
      writeArguments(visitor, definition.getArguments());
      definition.getResultType().accept(visitor, null);
    }
    visitor.getDataStream().writeBoolean(definition.getElimTree() != null);
    if (definition.getElimTree() != null) {
      definition.getElimTree().accept(visitor, null);
    }

    return errors;
  }

  public static void writeName(DataOutputStream stream, Name name) throws IOException {
    stream.writeUTF(name.name);
    stream.write(name.fixity == Abstract.Definition.Fixity.PREFIX ? 1 : 0);
  }

  public static void writeDefinition(DataOutputStream stream, Definition definition) throws IOException {
    stream.writeInt(DefinitionCodes.getDefinitionCode(definition).ordinal());
    if (!(definition instanceof ClassDefinition)) {
      stream.write(definition.getPrecedence().associativity == Abstract.Definition.Associativity.LEFT_ASSOC ? 0 : definition.getPrecedence().associativity == Abstract.Definition.Associativity.RIGHT_ASSOC ? 1 : 2);
      stream.writeByte(definition.getPrecedence().priority);
    }
    writeName(stream, definition.getName());
  }

  public static void writeUniverse(DataOutputStream stream, Universe universe) throws IOException {
    stream.writeInt(universe.getLevel());
    if (universe instanceof Universe.Type) {
      stream.writeInt(((Universe.Type) universe).getTruncated());
    } else {
      throw new IllegalStateException();
    }
  }

  public static void writeArguments(SerializeVisitor visitor, List<? extends Argument> arguments) throws IOException {
    visitor.getDataStream().writeInt(arguments.size());
    for (Argument argument : arguments) {
      writeArgument(visitor, argument);
    }
  }

  public static void writeArgument(SerializeVisitor visitor, Argument argument) throws IOException {
    visitor.getDataStream().writeBoolean(argument.getExplicit());
    if (argument instanceof TelescopeArgument) {
      visitor.getDataStream().write(0);
      visitor.getDataStream().writeInt(((TelescopeArgument) argument).getNames().size());
      for (String name : ((TelescopeArgument) argument).getNames()) {
        visitor.getDataStream().writeBoolean(name != null);
        if (name != null) {
          visitor.getDataStream().writeUTF(name);
        }
      }
      ((TypeArgument) argument).getType().accept(visitor, null);
    } else if (argument instanceof TypeArgument) {
      visitor.getDataStream().write(1);
      ((TypeArgument) argument).getType().accept(visitor, null);
    } else if (argument instanceof NameArgument) {
      visitor.getDataStream().write(2);
      String name = ((NameArgument) argument).getName();
      visitor.getDataStream().writeBoolean(name != null);
      if (name != null) {
        visitor.getDataStream().writeUTF(name);
      }
    } else {
      throw new IllegalStateException();
    }
  }
}
