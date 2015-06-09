package com.jetbrains.jetpad.vclang.serialization;

import com.jetbrains.jetpad.vclang.term.definition.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModuleSerialization {
  static private final byte[] SIGNATURE = { 'c', 'v', 0x0b, (byte) 0xb1 };
  static private final int VERSION = 0;

  static public void writeFile(ClassDefinition def, Path outputDir) throws IOException {
    Files.createDirectories(outputDir);
    ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(byteArrayStream);
    DefinitionsIndices definitionsIndices = new DefinitionsIndices();
    int errors = serializeClassDefinition(definitionsIndices, byteArrayStream, dataStream, def);

    DataOutputStream fileStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputDir.resolve(def.getName() + ".vcc").toFile())));
    fileStream.write(SIGNATURE);
    fileStream.writeInt(VERSION);
    fileStream.writeInt(errors);
    definitionsIndices.serialize(fileStream);
    byteArrayStream.writeTo(fileStream);
    fileStream.close();
  }

  static private int serializeDefinition(DefinitionsIndices definitionsIndices, ByteArrayOutputStream stream, DataOutputStream dataStream, Definition definition) {
    if (definition instanceof FunctionDefinition) {
      stream.write(0);
      return serializeFunctionDefinition(definitionsIndices, stream, dataStream, (FunctionDefinition) definition);
    } else
    if (definition instanceof DataDefinition) {
      stream.write(1);
      return serializeDataDefinition(definitionsIndices, stream, dataStream, (DataDefinition) definition);
    } else
    if (definition instanceof ClassDefinition) {
      stream.write(2);
      return serializeClassDefinition(definitionsIndices, stream, dataStream, (ClassDefinition) definition);
    } else
    if (definition instanceof Constructor) {
      stream.write(3);
      return serializeConstructor(definitionsIndices, stream, dataStream, (Constructor) definition);
    } else {
      throw new IllegalStateException();
    }
  }

  static private int serializeFunctionDefinition(DefinitionsIndices definitionsIndices, ByteArrayOutputStream stream, DataOutputStream dataStream, FunctionDefinition definition) {
    return 0;
  }

  static private int serializeDataDefinition(DefinitionsIndices definitionsIndices, ByteArrayOutputStream stream, DataOutputStream dataStream, DataDefinition definition) {
    return 0;
  }

  static private int serializeClassDefinition(DefinitionsIndices definitionsIndices, ByteArrayOutputStream stream, DataOutputStream dataStream, ClassDefinition definition) {
    int count = 0;
    for (Definition field : definition.getFields()) {
      count += serializeDefinition(definitionsIndices, stream, dataStream, field);
    }
    return count;
  }

  static private int serializeConstructor(DefinitionsIndices definitionsIndices, ByteArrayOutputStream stream, DataOutputStream dataStream, Constructor definition) {
    return 0;
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
