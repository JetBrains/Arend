package com.jetbrains.jetpad.vclang.serialization;

import com.jetbrains.jetpad.vclang.VcError;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.NameArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleSerialization {
  static private final byte[] SIGNATURE = { 'v', 'c', (byte) 0xb1, 0x0b };
  static private final int VERSION = 0;

  static public void writeFile(ClassDefinition def, File outputFile) throws IOException {
    Files.createDirectories(outputFile.getParentFile().toPath());
    ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(byteArrayStream);
    DefinitionsIndices definitionsIndices = new DefinitionsIndices();
    SerializeVisitor visitor = new SerializeVisitor(definitionsIndices, byteArrayStream, dataStream);
    serializeClassDefinition(visitor, def);

    DataOutputStream fileStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));
    fileStream.write(SIGNATURE);
    fileStream.writeInt(VERSION);
    fileStream.writeInt(visitor.getErrors());
    definitionsIndices.serialize(fileStream);
    byteArrayStream.writeTo(fileStream);
    fileStream.close();
  }

  static public ClassDefinition readFile(String className, ClassDefinition parentClass, Path file, ClassDefinition root, List<Definition> toLoad, List<VcError> errors) throws IOException, IncorrectFormat {
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
    stream.readInt();

    Map<Integer, Definition> definitionMap = new HashMap<>();
    definitionMap.put(0, root);
    int size = stream.readInt();
    for (int i = 0; i < size; ++i) {
      int index = stream.readInt();
      int parentIndex = stream.readInt();
      String name = stream.readUTF();
      int code = stream.read();
      Definition parent = definitionMap.get(parentIndex);
      if (!(parent instanceof ClassDefinition)) {
        throw new IncorrectFormat();
      }
      ClassDefinition classParent = (ClassDefinition) parent;
      Definition field = classParent.findField(name, errors);
      if (field == null) {
        Definition definition = newDefinition(code, name, classParent);
        definitionMap.put(index, definition);
        toLoad.add(definition);
      } else {
        definitionMap.put(index, field);
      }
    }

    return deserializeClassDefinition(className, parentClass, stream, errors);
  }

  static public int getDefinitionCode(Definition definition) {
    if (definition instanceof FunctionDefinition) return 0;
    if (definition instanceof DataDefinition) return 1;
    if (definition instanceof ClassDefinition) return 2;
    if (definition instanceof Constructor) return 3;
    throw new IllegalStateException();
  }

  static private Definition newDefinition(int code, String name, ClassDefinition parent) throws IncorrectFormat {
    if (code == 0) return new FunctionDefinition(name, parent, null, null, null, null, null, null);
    if (code == 1) return new DataDefinition(name, parent, null, null, null, null, null);
    if (code == 2) return new ClassDefinition(name, parent, null, null);
    if (code == 3) return new Constructor(-1, name, parent, null, null, null, null, null);
    throw new IncorrectFormat();
  }

  static private void serializeDefinition(SerializeVisitor visitor, Definition definition) throws IOException {
    if (definition instanceof Constructor) return;
    visitor.getDataStream().write(getDefinitionCode(definition));
    visitor.getDataStream().writeUTF(definition.getName());

    if (definition instanceof FunctionDefinition) {
      FunctionDefinition functionDefinition = (FunctionDefinition) definition;
      writeDefinition(visitor.getDataStream(), definition);
      writeArguments(visitor, functionDefinition.getArguments());
      functionDefinition.getResultType().accept(visitor);
      visitor.getDataStream().write(functionDefinition.getArrow() == Abstract.Definition.Arrow.LEFT ? 0 : 1);
      functionDefinition.getTerm().accept(visitor);
    } else
    if (definition instanceof DataDefinition) {
      DataDefinition dataDefinition = (DataDefinition) definition;
      writeDefinition(visitor.getDataStream(), definition);
      writeUniverse(visitor.getDataStream(), definition.getUniverse());
      writeArguments(visitor, dataDefinition.getParameters());
      visitor.getDataStream().writeInt(dataDefinition.getConstructors().size());
      for (Constructor constructor : dataDefinition.getConstructors()) {
        writeArguments(visitor, constructor.getArguments());
      }
    } else
    if (definition instanceof ClassDefinition) {
      serializeClassDefinition(visitor, (ClassDefinition) definition);
    } else {
      throw new IllegalStateException();
    }
  }

  static private Definition deserializeDefinition(ClassDefinition parent, DataInputStream stream, List<VcError> errors) throws IOException, IncorrectFormat {
    int code = stream.read();
    String name = stream.readUTF();
    if (code == 0) {
      return null;
    } else
    if (code == 1) {
      return null;
    } else
    if (code == 2) {
      deserializeClassDefinition(name, parent, stream, errors);
      return null;
    } else {
      throw new IncorrectFormat();
    }
  }

  static private void serializeClassDefinition(SerializeVisitor visitor, ClassDefinition definition) throws IOException {
    writeUniverse(visitor.getDataStream(), definition.getUniverse());
    visitor.getDataStream().writeInt(definition.getFields().size());
    for (Definition field : definition.getFields()) {
      serializeDefinition(visitor, field);
    }
  }

  static private ClassDefinition deserializeClassDefinition(String name, ClassDefinition parent, DataInputStream stream, List<VcError> errors) throws IOException, IncorrectFormat {
    Universe universe = readUniverse(stream);
    int size = stream.readInt();
    List<Definition> fields = new ArrayList<>(size);

    Definition field = parent.findField(name, errors);
    ClassDefinition result;
    if (field == null) {
      result = new ClassDefinition(name, parent, universe, fields);
      parent.getFields().add(result);
    } else {
      if (field instanceof ClassDefinition) {
        result = (ClassDefinition) field;
        result.setUniverse(universe);
      } else {
        throw new IncorrectFormat();
      }
    }

    for (int i = 0; i < size; ++i) {
      fields.add(deserializeDefinition(result, stream, errors));
    }
    return result;
  }

  static private void writeDefinition(DataOutputStream stream, Definition definition) throws IOException {
    stream.write(definition.getPrecedence().associativity == Abstract.Definition.Associativity.LEFT_ASSOC ? 0 : definition.getPrecedence().associativity == Abstract.Definition.Associativity.RIGHT_ASSOC ? 1 : 2);
    stream.write(definition.getPrecedence().priority);
    stream.write(definition.getFixity() == Abstract.Definition.Fixity.PREFIX ? 1 : 0);
  }

  static public void writeUniverse(DataOutputStream stream, Universe universe) throws IOException {
    stream.writeInt(universe.getLevel());
    if (universe instanceof Universe.Type) {
      stream.writeInt(((Universe.Type) universe).getTruncated());
    } else {
      throw new IllegalStateException();
    }
  }

  static public Universe readUniverse(DataInputStream stream) throws IOException {
    int level = stream.readInt();
    int truncated = stream.readInt();
    return new Universe.Type(level, truncated);
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
        visitor.getDataStream().write(name == null ? 0 : 1);
        if (name != null) {
          visitor.getDataStream().writeUTF(name);
        }
      }
      ((TypeArgument) argument).getType().accept(visitor);
    } else
    if (argument instanceof TypeArgument) {
      visitor.getDataStream().write(1);
      ((TypeArgument) argument).getType().accept(visitor);
    } else
    if (argument instanceof NameArgument) {
      visitor.getDataStream().write(2);
      String name = ((NameArgument) argument).getName();
      visitor.getDataStream().write(name == null ? 0 : 1);
      if (name != null) {
        visitor.getDataStream().writeUTF(name);
      }
    } else {
      throw new IllegalStateException();
    }
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
