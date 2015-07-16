package com.jetbrains.jetpad.vclang.serialization;

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

  public static void writeFile(ClassDefinition def, File outputFile) throws IOException {
    Files.createDirectories(outputFile.getParentFile().toPath());
    writeStream(def, new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile))));
  }

  public static void writeStream(ClassDefinition def, DataOutputStream stream) throws IOException {
    ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(byteArrayStream);
    DefinitionsIndices definitionsIndices = new DefinitionsIndices();
    SerializeVisitor visitor = new SerializeVisitor(definitionsIndices, byteArrayStream, dataStream);
    int errors = serializeClassDefinition(visitor, def);

    stream.write(SIGNATURE);
    stream.writeInt(VERSION);
    stream.writeInt(errors + visitor.getErrors());
    definitionsIndices.serialize(stream);
    byteArrayStream.writeTo(stream);
    stream.close();
  }

  public static int serializeDefinition(SerializeVisitor visitor, Definition definition) throws IOException {
    visitor.getDataStream().write(getDefinitionCode(definition));
    visitor.getDataStream().writeBoolean(definition.hasErrors());
    if (!(definition instanceof Constructor)) {
      visitor.getDataStream().writeInt(definition.getDependencies() == null ? 0 : definition.getDependencies().size());
      if (definition.getDependencies() != null) {
        for (Definition dependency : definition.getDependencies()) {
          visitor.getDataStream().writeInt(visitor.getDefinitionsIndices().getDefinitionIndex(dependency));
        }
      }
    }

    if (definition instanceof FunctionDefinition) {
      FunctionDefinition functionDefinition = (FunctionDefinition) definition;
      writeDefinition(visitor.getDataStream(), definition);
      if (definition instanceof OverriddenDefinition) {
        visitor.getDataStream().writeInt(visitor.getDefinitionsIndices().getDefinitionIndex(((OverriddenDefinition) definition).getOverriddenFunction()));
      }
      visitor.getDataStream().writeBoolean(functionDefinition.typeHasErrors());
      if (!functionDefinition.typeHasErrors()) {
        writeArguments(visitor, functionDefinition.getArguments());
        functionDefinition.getResultType().accept(visitor);
      }
      visitor.getDataStream().write(functionDefinition.getArrow() == null ? 0 : functionDefinition.getArrow() == Abstract.Definition.Arrow.LEFT ? 1 : 2);
      if (!definition.hasErrors() && !functionDefinition.isAbstract()) {
        functionDefinition.getTerm().accept(visitor);
      }
      return definition.hasErrors() ? 1 : 0;
    } else if (definition instanceof DataDefinition) {
      int errors = definition.hasErrors() ? 1 : 0;
      DataDefinition dataDefinition = (DataDefinition) definition;
      writeDefinition(visitor.getDataStream(), definition);
      if (!definition.hasErrors()) {
        writeUniverse(visitor.getDataStream(), definition.getUniverse());
        writeArguments(visitor, dataDefinition.getParameters());
      }
      visitor.getDataStream().writeInt(dataDefinition.getConstructors().size());
      for (Constructor constructor : dataDefinition.getConstructors()) {
        visitor.getDataStream().writeInt(visitor.getDefinitionsIndices().getDefinitionIndex(constructor));
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
    } else if (definition instanceof ClassDefinition) {
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

    int errors = 0;
    if (definition.getPublicFields() != null) {
      visitor.getDataStream().writeInt(definition.getPublicFields().size());
      for (Definition field : definition.getPublicFields()) {
        visitor.getDataStream().writeInt(visitor.getDefinitionsIndices().getDefinitionIndex(field));
        errors += serializeDefinition(visitor, field);
      }
    } else {
      visitor.getDataStream().writeInt(0);
    }

    if (definition.getStaticFields() != null) {
      visitor.getDataStream().writeInt(definition.getStaticFields().size());
      for (Definition field : definition.getStaticFields()) {
        visitor.getDataStream().writeInt(visitor.getDefinitionsIndices().getDefinitionIndex(field));
      }
    } else {
      visitor.getDataStream().writeInt(0);
    }

    return errors;
  }

  private static void writeDefinition(DataOutputStream stream, Definition definition) throws IOException {
    stream.write(definition.getPrecedence().associativity == Abstract.Definition.Associativity.LEFT_ASSOC ? 0 : definition.getPrecedence().associativity == Abstract.Definition.Associativity.RIGHT_ASSOC ? 1 : 2);
    stream.writeByte(definition.getPrecedence().priority);
    stream.write(definition.getFixity() == Abstract.Definition.Fixity.PREFIX ? 1 : 0);
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
