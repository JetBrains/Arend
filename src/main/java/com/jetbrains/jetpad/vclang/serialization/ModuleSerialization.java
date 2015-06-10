package com.jetbrains.jetpad.vclang.serialization;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.NameArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ModuleSerialization {
  static private final byte[] SIGNATURE = { 'c', 'v', 0x0b, (byte) 0xb1 };
  static private final int VERSION = 0;

  static public void writeFile(ClassDefinition def, Path outputDir) throws IOException {
    Files.createDirectories(outputDir);
    ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(byteArrayStream);
    DefinitionsIndices definitionsIndices = new DefinitionsIndices();
    SerializeVisitor visitor = new SerializeVisitor(definitionsIndices, byteArrayStream, dataStream);
    serializeClassDefinition(visitor, def);

    DataOutputStream fileStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputDir.resolve(def.getName() + ".vcc").toFile())));
    fileStream.write(SIGNATURE);
    fileStream.writeInt(VERSION);
    fileStream.writeInt(visitor.getErrors());
    definitionsIndices.serialize(fileStream);
    byteArrayStream.writeTo(fileStream);
    fileStream.close();
  }

  static private void serializeDefinition(SerializeVisitor visitor, Definition definition) throws IOException {
    if (definition instanceof FunctionDefinition) {
      visitor.getDataStream().write(0);
      serializeFunctionDefinition(visitor, (FunctionDefinition) definition);
    } else
    if (definition instanceof DataDefinition) {
      visitor.getDataStream().write(1);
      serializeDataDefinition(visitor, (DataDefinition) definition);
    } else
    if (definition instanceof ClassDefinition) {
      visitor.getDataStream().write(2);
      serializeClassDefinition(visitor, (ClassDefinition) definition);
    } else
    if (!(definition instanceof Constructor)) {
      throw new IllegalStateException();
    }
  }

  static private void serializeFunctionDefinition(SerializeVisitor visitor, FunctionDefinition definition) throws IOException {
    writeDefinition(visitor.getDataStream(), definition);
    writeArguments(visitor, definition.getArguments());
    definition.getResultType().accept(visitor);
    visitor.getDataStream().write(definition.getArrow() == Abstract.Definition.Arrow.LEFT ? 0 : 1);
    definition.getTerm().accept(visitor);
  }

  static private void serializeDataDefinition(SerializeVisitor visitor, DataDefinition definition) throws IOException {
    writeDefinition(visitor.getDataStream(), definition);
    writeArguments(visitor, definition.getParameters());
    visitor.getDataStream().writeInt(definition.getConstructors().size());
    for (Constructor constructor : definition.getConstructors()) {
      writeArguments(visitor, constructor.getArguments());
    }
  }

  static private void serializeClassDefinition(SerializeVisitor visitor, ClassDefinition definition) throws IOException {
    writeUniverse(visitor.getDataStream(), definition.getUniverse());
    for (Definition field : definition.getFields()) {
      serializeDefinition(visitor, field);
    }
  }

  static private void writeDefinition(DataOutputStream stream, Definition definition) throws IOException {
    stream.write(definition.getPrecedence().associativity == Abstract.Definition.Associativity.LEFT_ASSOC ? 0 : definition.getPrecedence().associativity == Abstract.Definition.Associativity.RIGHT_ASSOC ? 1 : 2);
    stream.write(definition.getPrecedence().priority);
    stream.write(definition.getFixity() == Abstract.Definition.Fixity.PREFIX ? 1 : 0);
    writeUniverse(stream, definition.getUniverse());
  }

  static public void writeUniverse(DataOutputStream stream, Universe universe) throws IOException {
    stream.writeInt(universe.getLevel());
    if (universe instanceof Universe.Type) {
      stream.writeInt(((Universe.Type) universe).getTruncated());
    } else {
      throw new IllegalStateException();
    }
  }

  static public void writeArguments(SerializeVisitor visitor, List<? extends Argument> arguments) throws IOException {
    visitor.getDataStream().writeInt(arguments.size());
    for (Argument argument : arguments) {
      writeArgument(visitor, argument);
    }
  }

  static public void writeArgument(SerializeVisitor visitor, Argument argument) throws IOException {
    visitor.getDataStream().write(argument.getExplicit() ? 1 : 0);
    if (argument instanceof TelescopeArgument) {
      visitor.getDataStream().write(0);
      visitor.getDataStream().writeInt(((TelescopeArgument) argument).getNames().size());
      for (String name : ((TelescopeArgument) argument).getNames()) {
        visitor.getDataStream().writeBytes(name);
      }
      ((TypeArgument) argument).getType().accept(visitor);
    } else
    if (argument instanceof TypeArgument) {
      visitor.getDataStream().write(1);
      ((TypeArgument) argument).getType().accept(visitor);
    }
    if (argument instanceof NameArgument) {
      visitor.getDataStream().write(2);
      visitor.getDataStream().writeBytes(((NameArgument) argument).getName());
    } else {
      throw new IllegalStateException();
    }
  }

  static public ClassDefinition readFile(Path file) throws IOException, IncorrectFormat {
    DataInputStream stream = new DataInputStream(new BufferedInputStream(new FileInputStream(file.toFile())));
    byte[] signature = new byte[4];
    stream.readFully(signature);
    if (signature != SIGNATURE) {
      throw new IncorrectFormat();
    }
    int version = stream.readInt();
    if (version != VERSION) {
      throw new WrongVersion(version);
    }
    return null;
  }

  static class IncorrectFormat extends Exception {
    @Override
    public String toString() {
      return "Incorrect format";
    }
  }

  static class WrongVersion extends IncorrectFormat {
    private final int myVersion;

    WrongVersion(int version) {
      myVersion = version;
    }

    @Override
    public String toString() {
      return "Version of the file format (" + myVersion + ") differs from the version of the program + (" + VERSION + ")";
    }
  }
}
