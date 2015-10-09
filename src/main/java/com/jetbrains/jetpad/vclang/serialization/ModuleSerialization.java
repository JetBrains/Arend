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
import java.util.List;

public class ModuleSerialization {
  public static final byte[] SIGNATURE = {'v', 'c', (byte) 0xb1, 0x0b};
  public static final int VERSION = 0;

  public static void writeFile(ResolvedName resolvedName, ClassDefinition classDefinition, File outputFile) throws IOException {
    Files.createDirectories(outputFile.getParentFile().toPath());
    writeStream(resolvedName, classDefinition, new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile))));
  }

  public static void writeStream(ResolvedName resolvedName, ClassDefinition classDefinition, DataOutputStream stream) throws IOException {
    ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(byteArrayStream);
    DefinitionsIndices definitionsIndices = new DefinitionsIndices();
    SerializeVisitor visitor = new SerializeVisitor(definitionsIndices, byteArrayStream, dataStream);
    definitionsIndices.getDefinitionIndex(resolvedName.toDefinition() /* TODO */, false);
    int errors = serializeNamespace(visitor, resolvedName.toNamespace());
    if (classDefinition != null) {
      visitor.getDataStream().writeBoolean(true);
      errors += serializeClassDefinition(visitor, classDefinition);
    } else {
      visitor.getDataStream().writeBoolean(false);
    }

    stream.write(SIGNATURE);
    stream.writeInt(VERSION);
    stream.writeInt(errors + visitor.getErrors());
    definitionsIndices.serialize(stream);
    byteArrayStream.writeTo(stream);
    stream.close();
  }

  public static int serializeNamespace(SerializeVisitor visitor, Namespace namespace) throws IOException {
    int errors = 0;
    int size = 0;
    for (NamespaceMember member : namespace.getMembers()) {
      if (member.definition != null && member.definition.getParentNamespace() == namespace) {
        ++size;
      }
    }
    visitor.getDataStream().writeInt(size);

    for (NamespaceMember member : namespace.getMembers()) {
      if (member.definition != null && member.definition.getParentNamespace() == namespace) {
        // TODO
        /*
        visitor.getDataStream().writeInt(visitor.getDefinitionsIndices().getDefinitionIndex(member.definition.getNamespace(), false));
        errors += serializeNamespace(visitor, member.definition.getNamespace());
        if (member.definition != null && !(member.definition instanceof Constructor) && member.definition.getNamespace().getParent() == namespace) {
          visitor.getDataStream().writeBoolean(true);
          visitor.getDataStream().writeInt(visitor.getDefinitionsIndices().getDefinitionIndex(member.definition, true));
          errors += serializeDefinition(visitor, member.definition);
        } else {
          visitor.getDataStream().writeBoolean(false);
        }
        */
      }
    }
    return errors;
  }

  public static int serializeDefinition(SerializeVisitor visitor, Definition definition) throws IOException {
    visitor.getDataStream().write(getDefinitionCode(definition));
    visitor.getDataStream().writeBoolean(definition.hasErrors());

    if (definition instanceof FunctionDefinition) {
      return serializeFunctionDefinition(visitor, (FunctionDefinition) definition);
    } else
    if (definition instanceof DataDefinition) {
      int errors = definition.hasErrors() ? 1 : 0;
      DataDefinition dataDefinition = (DataDefinition) definition;
      writeDefinition(visitor.getDataStream(), definition);
      if (!definition.hasErrors()) {
        writeUniverse(visitor.getDataStream(), definition.getUniverse());
        writeArguments(visitor, dataDefinition.getParameters());
      }
      visitor.getDataStream().writeInt(dataDefinition.getConstructors().size());
      for (Constructor constructor : dataDefinition.getConstructors()) {
        visitor.getDataStream().writeInt(visitor.getDefinitionsIndices().getDefinitionIndex(constructor, true));
        visitor.getDataStream().writeBoolean(constructor.hasErrors());
        writeDefinition(visitor.getDataStream(), constructor);
        if (!constructor.hasErrors()) {
          writeUniverse(visitor.getDataStream(), constructor.getUniverse());
          writeArguments(visitor, constructor.getArguments());
        } else {
          errors += 1;
        }
      }
      return errors;
    } else
    if (definition instanceof ClassDefinition) {
      return serializeClassDefinition(visitor, (ClassDefinition) definition);
    } else {
      throw new IllegalStateException();
    }
  }

  public static int FUNCTION_CODE = 0;
  public static int DATA_CODE = 1;
  public static int CLASS_CODE = 2;
  public static int CONSTRUCTOR_CODE = 3;
  public static int OVERRIDDEN_CODE = 4;
  public static int NAMESPACE_CODE = 5;

  public static int getDefinitionCode(Definition definition) {
    if (definition instanceof OverriddenDefinition) return OVERRIDDEN_CODE;
    if (definition instanceof FunctionDefinition) return FUNCTION_CODE;
    if (definition instanceof DataDefinition) return DATA_CODE;
    if (definition instanceof ClassDefinition) return CLASS_CODE;
    if (definition instanceof Constructor) return CONSTRUCTOR_CODE;
    // TODO
    // if (definition instanceof Namespace) return NAMESPACE_CODE;
    throw new IllegalStateException();
  }

  private static int serializeClassDefinition(SerializeVisitor visitor, ClassDefinition definition) throws IOException {
    writeUniverse(visitor.getDataStream(), definition.getUniverse());
    // TODO
    // visitor.getDataStream().writeInt(visitor.getDefinitionsIndices().getDefinitionIndex(definition.getLocalNamespace(), false));
    return serializeNamespace(visitor, definition.getLocalNamespace());
  }

  private static int serializeFunctionDefinition(SerializeVisitor visitor, FunctionDefinition definition) throws IOException {
    int errors = definition.hasErrors() ? 1 : 0;

    writeDefinition(visitor.getDataStream(), definition);
    if (definition instanceof OverriddenDefinition)
      visitor.getDataStream().writeInt(visitor.getDefinitionsIndices().getDefinitionIndex(((OverriddenDefinition) definition).getOverriddenFunction(), false));

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
