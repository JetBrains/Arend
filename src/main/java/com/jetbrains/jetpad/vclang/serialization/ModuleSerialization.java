package com.jetbrains.jetpad.vclang.serialization;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.NameArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class ModuleSerialization {
  public static final byte[] SIGNATURE = {'v', 'c', (byte) 0xb1, 0x0b};
  public static final int VERSION = 0;

  public static void writeFile(ResolvedName resolvedName, File outputFile) throws IOException {
    Files.createDirectories(outputFile.getParentFile().toPath());
    writeStream(resolvedName, new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile))));
  }

  public static void serializeResolvedName(ResolvedName rn, DataOutputStream stream) throws IOException {
    List<String> fPath = new ArrayList<>();
    for (; rn.parent != null; rn = rn.parent.getResolvedName()) {
      fPath.add(rn.name.name);
    }
    Collections.reverse(fPath);
    stream.writeInt(fPath.size());
    for (String aPath : fPath) {
      stream.writeUTF(aPath);
    }
  }

  public static void writeStream(ResolvedName resolvedName, DataOutputStream stream) throws IOException {
    assert resolvedName.toAbstractDefinition() != null && resolvedName.toDefinition() != null;
    assert true; /* This is really a name of a module */

    DefNamesIndicies defNamesIndicies = new DefNamesIndicies();
    ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(byteArrayStream);
    SerializeVisitor visitor = new SerializeVisitor(defNamesIndicies, byteArrayStream, dataStream);

    int errors = serializeDefinition(visitor, resolvedName.toDefinition());

    stream.write(SIGNATURE);
    stream.writeInt(VERSION);
    stream.writeInt(errors + visitor.getErrors());
    defNamesIndicies.serializeHeader(stream);
    defNamesIndicies.serialize(stream);
    byteArrayStream.writeTo(stream);
    stream.close();
  }

  public static int serializeNamespace(SerializeVisitor visitor, Namespace namespace) throws IOException {
    int errors = 0;
    int size = 0;
    for (NamespaceMember member : namespace.getMembers()) {
      if (member.getResolvedName().parent != namespace || member.abstractDefinition.getParentStatement() != null) {
        ++size;
      }
    }
    visitor.getDataStream().writeInt(size);
    for (NamespaceMember member : namespace.getMembers()) {
      if (member.abstractDefinition.getParentStatement() != null) {
        visitor.getDataStream().writeBoolean(true);
        visitor.getDataStream().writeInt(visitor.getDefinitionsIndices().getDefNameIndex(member.definition.getResolvedName(), true));
        errors += serializeDefinition(visitor, member.definition);
      } else if (member.getResolvedName().parent != namespace) {
        visitor.getDataStream().writeBoolean(false);
        visitor.getDataStream().writeInt(visitor.getDefinitionsIndices().getDefNameIndex(member.definition.getResolvedName(), true));
      }
    }
    return errors;
  }

  public static int serializeDefinition(SerializeVisitor visitor, Definition definition) throws IOException {
    visitor.getDefinitionsIndices().getDefNameIndex(definition.getResolvedName(), true);
    visitor.getDataStream().write(getDefinitionCode(definition));
    visitor.getDataStream().writeBoolean(definition.hasErrors());

    if (definition instanceof FunctionDefinition) {
      return serializeFunctionDefinition(visitor, (FunctionDefinition) definition);
    } else
    if (definition instanceof DataDefinition) {
      return serializeDataDefinition(visitor, (DataDefinition) definition);
    } else
    if (definition instanceof ClassDefinition) {
      return serializeClassDefinition(visitor, (ClassDefinition) definition);
    } else
    if (definition instanceof Constructor) {
      return 0; // Serialized in data definition
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
        writeUniverse(visitor.getDataStream(), constructor.getUniverse());
        writeArguments(visitor, constructor.getArguments());
      } else {
        errors += 1;
      }
    }
    return errors;
  }

  public static int FUNCTION_CODE = 0;
  public static int DATA_CODE = 1;
  public static int CLASS_CODE = 2;
  public static int CONSTRUCTOR_CODE = 3;
  public static int OVERRIDDEN_CODE = 4;

  public static int getDefinitionCode(Definition definition) {
    if (definition instanceof OverriddenDefinition) return OVERRIDDEN_CODE;
    if (definition instanceof FunctionDefinition) return FUNCTION_CODE;
    if (definition instanceof DataDefinition) return DATA_CODE;
    if (definition instanceof ClassDefinition) return CLASS_CODE;
    if (definition instanceof Constructor) return CONSTRUCTOR_CODE;
    throw new IllegalStateException();
  }

  private static int serializeClassDefinition(SerializeVisitor visitor, ClassDefinition definition) throws IOException {
    writeUniverse(visitor.getDataStream(), definition.getUniverse());
    serializeNamespace(visitor, definition.getParentNamespace().getChild(definition.getName()));
    return 0;
  }

  private static int serializeFunctionDefinition(SerializeVisitor visitor, FunctionDefinition definition) throws IOException {
    int errors = definition.hasErrors() ? 1 : 0;

    serializeNamespace(visitor, definition.getStaticNamespace());

    /*
    if (definition instanceof OverriddenDefinition)
      visitor.getDataStream().writeInt(visitor.getDefinitionsIndices().getDefNameIndex(((OverriddenDefinition) definition).getOverriddenFunction(), false));
    */

    visitor.getDataStream().writeBoolean(definition.typeHasErrors());
    if (!definition.typeHasErrors()) {
      writeArguments(visitor, definition.getArguments());
      definition.getResultType().accept(visitor);
    }
    visitor.getDataStream().write(definition.getArrow() == null ? 0 : definition.getArrow() == Abstract.Definition.Arrow.LEFT ? 1 : 2);
    if (!definition.hasErrors() && !definition.isAbstract()) {
      definition.getTerm().accept(visitor);
    }

    return errors;
  }

  private static void writeDefinition(DataOutputStream stream, Definition definition) throws IOException {
    stream.write(definition.getPrecedence().associativity == Abstract.Definition.Associativity.LEFT_ASSOC ? 0 : definition.getPrecedence().associativity == Abstract.Definition.Associativity.RIGHT_ASSOC ? 1 : 2);
    stream.writeByte(definition.getPrecedence().priority);
    stream.write(definition.getName().fixity == Abstract.Definition.Fixity.PREFIX ? 1 : 0);
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
      ((TypeArgument) argument).getType().accept(visitor);
    } else if (argument instanceof TypeArgument) {
      visitor.getDataStream().write(1);
      ((TypeArgument) argument).getType().accept(visitor);
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
