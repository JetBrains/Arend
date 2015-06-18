package com.jetbrains.jetpad.vclang.serialization;

import com.jetbrains.jetpad.vclang.VcError;
import com.jetbrains.jetpad.vclang.module.Module;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.NameArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Error;

public class ModuleSerialization {
  static private final byte[] SIGNATURE = { 'v', 'c', (byte) 0xb1, 0x0b };
  static private final int VERSION = 0;

  static public void writeFile(ClassDefinition def, File outputFile) throws IOException {
    Files.createDirectories(outputFile.getParentFile().toPath());
    ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(byteArrayStream);
    DefinitionsIndices definitionsIndices = new DefinitionsIndices();
    SerializeVisitor visitor = new SerializeVisitor(definitionsIndices, byteArrayStream, dataStream);
    int errors = serializeClassDefinition(visitor, def);

    DataOutputStream fileStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));
    fileStream.write(SIGNATURE);
    fileStream.writeInt(VERSION);
    fileStream.writeInt(errors + visitor.getErrors());
    definitionsIndices.serialize(fileStream);
    byteArrayStream.writeTo(fileStream);
    fileStream.close();
  }

  static public void readFile(File file, ClassDefinition module, List<ModuleLoader.TypeCheckingUnit> typeCheckingUnits, List<ModuleLoader.OutputUnit> outputUnits, List<VcError> errors) throws IOException, DeserializationException {
    DataInputStream stream = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
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
    definitionMap.put(0, ModuleLoader.rootModule());
    int size = stream.readInt();
    for (int i = 0; i < size; ++i) {
      int index = stream.readInt();
      int parentIndex = stream.readInt();
      String name = stream.readUTF();
      Definition parent = definitionMap.get(parentIndex);
      Definition child = parent.findChild(name);
      if (child == null) {
        if (parent instanceof ClassDefinition) {
          child = ModuleLoader.loadModule(new Module((ClassDefinition) parent, name), typeCheckingUnits, outputUnits, errors);
        }
        if (child == null) {
          throw new DeserializationException(name + " is not defined in " + parent.getFullName());
        }
      }
      definitionMap.put(index, child);
    }

    deserializeClassDefinition(stream, definitionMap, module, errors);
  }

  static private int serializeDefinition(SerializeVisitor visitor, Definition definition) throws IOException {
    if (definition instanceof Constructor) return 0;
    visitor.getDataStream().write(getDefinitionCode(definition));
    visitor.getDataStream().writeUTF(definition.getName());
    visitor.getDataStream().writeBoolean(definition.hasErrors());

    if (definition instanceof FunctionDefinition) {
      FunctionDefinition functionDefinition = (FunctionDefinition) definition;
      writeDefinition(visitor.getDataStream(), definition);
      visitor.getDataStream().writeBoolean(functionDefinition.typeHasErrors());
      if (!functionDefinition.typeHasErrors()) {
        writeArguments(visitor, functionDefinition.getArguments());
        functionDefinition.getResultType().accept(visitor);
      }
      visitor.getDataStream().writeBoolean(functionDefinition.getArrow() == Abstract.Definition.Arrow.RIGHT);
      if (!definition.hasErrors()) {
        functionDefinition.getTerm().accept(visitor);
      }
      return definition.hasErrors() ? 1 : 0;
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
        visitor.getDataStream().writeUTF(constructor.getName());
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

  static public int getDefinitionCode(Definition definition) {
    if (definition instanceof FunctionDefinition) return 0;
    if (definition instanceof DataDefinition) return 1;
    if (definition instanceof ClassDefinition) return 2;
    if (definition instanceof Constructor) return 3;
    throw new IllegalStateException();
  }

  static private void deserializeDefinition(DataInputStream stream, Map<Integer, Definition> definitionMap, ClassDefinition parent, List<VcError> errors) throws IOException, DeserializationException {
    int code = stream.read();
    String name = stream.readUTF();
    boolean hasErrors = stream.readBoolean();

    Definition definition = parent.findChild(name);
    if (definition != null && !(definition instanceof ClassDefinition && code == 2)) {
      throw new DeserializationException(name + " is already defined in " + parent.getFullName());
    }

    if (code == 0) {
      FunctionDefinition functionDefinition = (FunctionDefinition) readDefinition(stream, code, name, parent);
      functionDefinition.hasErrors(hasErrors);
      functionDefinition.typeHasErrors(stream.readBoolean());
      if (!functionDefinition.typeHasErrors()) {
        functionDefinition.setArguments(readTelescopeArguments(stream, definitionMap));
        functionDefinition.setResultType(readExpression(stream, definitionMap));
      }
      functionDefinition.setArrow(stream.readBoolean() ? Abstract.Definition.Arrow.RIGHT : Abstract.Definition.Arrow.LEFT);
      if (!functionDefinition.hasErrors()) {
        functionDefinition.setTerm(readExpression(stream, definitionMap));
      }
      parent.getFields().add(functionDefinition);
    } else
    if (code == 1) {
      DataDefinition dataDefinition = (DataDefinition) readDefinition(stream, code, name, parent);
      if (!dataDefinition.hasErrors()) {
        dataDefinition.setUniverse(readUniverse(stream));
        dataDefinition.setParameters(readTypeArguments(stream, definitionMap));
      }
      int constructorsNumber = stream.readInt();
      dataDefinition.setConstructors(new ArrayList<Constructor>(constructorsNumber));
      for (int i = 0; i < constructorsNumber; ++i) {
        String constructorName = stream.readUTF();
        boolean constructorHasErrors = stream.readBoolean();
        Constructor constructor = (Constructor) readDefinition(stream, 3, constructorName, dataDefinition);
        constructor.setIndex(i);
        constructor.hasErrors(constructorHasErrors);
        if (!constructorHasErrors) {
          constructor.setUniverse(readUniverse(stream));
          constructor.setArguments(readTypeArguments(stream, definitionMap));
        }
        dataDefinition.getConstructors().add(constructor);
      }
      parent.getFields().add(dataDefinition);
    } else
    if (code == 2) {
      ClassDefinition classDefinition = definition == null ? new ClassDefinition(name, parent, new ArrayList<Definition>()) : (ClassDefinition) definition;
      classDefinition.hasErrors(hasErrors);
      deserializeClassDefinition(stream, definitionMap, classDefinition, errors);
      if (definition == null) {
        parent.getFields().add(classDefinition);
      }
    } else {
      throw new IncorrectFormat();
    }
  }

  static private int serializeClassDefinition(SerializeVisitor visitor, ClassDefinition definition) throws IOException {
    writeUniverse(visitor.getDataStream(), definition.getUniverse());
    visitor.getDataStream().writeInt(definition.getFields().size());
    int errors = 0;
    for (Definition field : definition.getFields()) {
      errors += serializeDefinition(visitor, field);
    }
    return errors;
  }

  static private void deserializeClassDefinition(DataInputStream stream, Map<Integer, Definition> definitionMap, ClassDefinition definition, List<VcError> errors) throws IOException, DeserializationException {
    Universe universe = readUniverse(stream);
    int size = stream.readInt();
    definition.setUniverse(universe);
    for (int i = 0; i < size; ++i) {
      deserializeDefinition(stream, definitionMap, definition, errors);
    }
  }

  static private void writeDefinition(DataOutputStream stream, Definition definition) throws IOException {
    stream.write(definition.getPrecedence().associativity == Abstract.Definition.Associativity.LEFT_ASSOC ? 0 : definition.getPrecedence().associativity == Abstract.Definition.Associativity.RIGHT_ASSOC ? 1 : 2);
    stream.writeByte(definition.getPrecedence().priority);
    stream.write(definition.getFixity() == Abstract.Definition.Fixity.PREFIX ? 1 : 0);
  }

  static private Definition readDefinition(DataInputStream stream, int code, String name, Definition parent) throws IncorrectFormat, IOException {
    int assocCode = stream.read();
    Abstract.Definition.Associativity assoc;
    if (assocCode == 0) {
      assoc = Abstract.Definition.Associativity.LEFT_ASSOC;
    } else
    if (assocCode == 1) {
      assoc = Abstract.Definition.Associativity.RIGHT_ASSOC;
    } else
    if (assocCode == 2) {
      assoc = Abstract.Definition.Associativity.NON_ASSOC;
    } else {
      throw new IncorrectFormat();
    }
    Abstract.Definition.Precedence precedence = new Abstract.Definition.Precedence(assoc, stream.readByte());
    int fixityCode = stream.read();
    Abstract.Definition.Fixity fixity;
    if (fixityCode == 0) {
      fixity = Abstract.Definition.Fixity.INFIX;
    } else
    if (fixityCode == 1) {
      fixity = Abstract.Definition.Fixity.PREFIX;
    } else {
      throw new IncorrectFormat();
    }

    if (code == 0) return new FunctionDefinition(name, parent, precedence, fixity, null);
    if (code == 1) return new DataDefinition(name, parent, precedence, fixity, null);
    if (code == 2) return new ClassDefinition(name, parent, new ArrayList<Definition>());
    if (code == 3) return new Constructor(-1, name, parent, precedence, fixity);
    throw new IncorrectFormat();
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

  static public List<Argument> readArguments(DataInputStream stream, Map<Integer, Definition> definitionMap) throws IOException, IncorrectFormat {
    int size = stream.readInt();
    List<Argument> result = new ArrayList<>(size);
    for (int i = 0; i < size; ++i) {
      result.add(readArgument(stream, definitionMap));
    }
    return result;
  }

  static public List<NameArgument> readNameArguments(DataInputStream stream, Map<Integer, Definition> definitionMap) throws IOException, IncorrectFormat {
    int size = stream.readInt();
    List<NameArgument> result = new ArrayList<>(size);
    for (int i = 0; i < size; ++i) {
      Argument argument = readArgument(stream, definitionMap);
      if (!(argument instanceof NameArgument)) {
        throw new IncorrectFormat();
      }
      result.add((NameArgument) argument);
    }
    return result;
  }

  static public List<TypeArgument> readTypeArguments(DataInputStream stream, Map<Integer, Definition> definitionMap) throws IOException, IncorrectFormat {
    int size = stream.readInt();
    List<TypeArgument> result = new ArrayList<>(size);
    for (int i = 0; i < size; ++i) {
      Argument argument = readArgument(stream, definitionMap);
      if (!(argument instanceof TypeArgument)) {
        throw new IncorrectFormat();
      }
      result.add((TypeArgument) argument);
    }
    return result;
  }

  static public List<TelescopeArgument> readTelescopeArguments(DataInputStream stream, Map<Integer, Definition> definitionMap) throws IOException, IncorrectFormat {
    int size = stream.readInt();
    List<TelescopeArgument> result = new ArrayList<>(size);
    for (int i = 0; i < size; ++i) {
      Argument argument = readArgument(stream, definitionMap);
      if (!(argument instanceof TelescopeArgument)) {
        throw new IncorrectFormat();
      }
      result.add((TelescopeArgument) argument);
    }
    return result;
  }

  static public void writeArgument(SerializeVisitor visitor, Argument argument) throws IOException {
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
    } else
    if (argument instanceof TypeArgument) {
      visitor.getDataStream().write(1);
      ((TypeArgument) argument).getType().accept(visitor);
    } else
    if (argument instanceof NameArgument) {
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

  static public Argument readArgument(DataInputStream stream, Map<Integer, Definition> definitionMap) throws IOException, IncorrectFormat {
    boolean explicit = stream.readBoolean();
    int code = stream.read();
    if (code == 0) {
      int size = stream.readInt();
      List<String> names = new ArrayList<>(size);
      for (int i = 0; i < size; ++i) {
        names.add(stream.readBoolean() ? stream.readUTF() : null);
      }
      return new TelescopeArgument(explicit, names, readExpression(stream, definitionMap));
    } else
    if (code == 1) {
      return new TypeArgument(explicit, readExpression(stream, definitionMap));
    } else
    if (code == 2) {
      return new NameArgument(explicit, stream.readBoolean() ? stream.readUTF() : null);
    } else {
      throw new IncorrectFormat();
    }
  }

  static public Expression readExpression(DataInputStream stream, Map<Integer, Definition> definitionMap) throws IOException, IncorrectFormat {
    int code = stream.read();
    switch (code) {
      case 1: {
        Expression function = readExpression(stream, definitionMap);
        boolean explicit = stream.readBoolean();
        boolean hidden = stream.readBoolean();
        Expression argument = readExpression(stream, definitionMap);
        return Apps(function, new ArgumentExpression(argument, explicit, hidden));
      }
      case 2: {
        Definition definition = definitionMap.get(stream.readInt());
        if (definition == null) {
          throw new IncorrectFormat();
        }
        return DefCall(definition);
      }
      case 3: {
        return Index(stream.readInt());
      }
      case 4: {
        Expression body = readExpression(stream, definitionMap);
        return Lam(readArguments(stream, definitionMap), body);
      }
      case 5: {
        List<TypeArgument> arguments = readTypeArguments(stream, definitionMap);
        return Pi(arguments, readExpression(stream, definitionMap));
      }
      case 6: {
        return new UniverseExpression(readUniverse(stream));
      }
      case 9: {
        return Error(stream.readBoolean() ? readExpression(stream, definitionMap) : null, new TypeCheckingError("Deserialized error", null, null));
      }
      case 10: {
        int size = stream.readInt();
        List<Expression> fields = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
          fields.add(readExpression(stream, definitionMap));
        }
        return new TupleExpression(fields);
      }
      case 11: {
        return Sigma(readTypeArguments(stream, definitionMap));
      }
      case 12: {
        Abstract.ElimExpression.ElimType elimType = stream.readBoolean() ? Abstract.ElimExpression.ElimType.ELIM : Abstract.ElimExpression.ElimType.CASE;
        int index = stream.readInt();
        int clausesNumber = stream.readInt();
        List<Clause> clauses = new ArrayList<>(clausesNumber);
        for (int i = 0; i < clausesNumber; ++i) {
          clauses.add(readClause(stream, definitionMap));
        }
        ElimExpression result = Elim(elimType, Index(index), clauses, stream.readBoolean() ? readClause(stream, definitionMap) : null);
        for (Clause clause : result.getClauses()) {
          clause.setElimExpression(result);
        }
        if (result.getOtherwise() != null) {
          result.getOtherwise().setElimExpression(result);
        }
        return result;
      }
      case 13: {
        Expression expr = readExpression(stream, definitionMap);
        Definition definition = definitionMap.get(stream.readInt());
        if (definition == null) {
          throw new IncorrectFormat();
        }
        return FieldAcc(expr, definition);
      }
      case 14: {
        Expression expr = readExpression(stream, definitionMap);
        return Proj(expr, stream.readInt());
      }
      default: {
        throw new IncorrectFormat();
      }
    }
  }

  static public Clause readClause(DataInputStream stream, Map<Integer, Definition> definitionMap) throws IOException, IncorrectFormat {
    Definition definition = definitionMap.get(stream.readInt());
    if (!(definition instanceof Constructor)) {
      throw new IncorrectFormat();
    }
    List<NameArgument> arguments = readNameArguments(stream, definitionMap);
    Abstract.Definition.Arrow arrow = stream.readBoolean() ? Abstract.Definition.Arrow.RIGHT : Abstract.Definition.Arrow.LEFT;
    return new Clause((Constructor) definition, arguments, arrow, readExpression(stream, definitionMap), null);
  }

  public static class DeserializationException extends Exception {
    private final String myMessage;

    public DeserializationException(String message) {
      myMessage = message;
    }

    @Override
    public String toString() {
      return myMessage;
    }
  }

  public static class IncorrectFormat extends DeserializationException {
    public IncorrectFormat() {
      super("Incorrect format");
    }
  }

  public static class WrongVersion extends DeserializationException {
    WrongVersion(int version) {
      super("Version of the file format (" + version + ") differs from the version of the program + (" + VERSION + ")");
    }
  }
}
