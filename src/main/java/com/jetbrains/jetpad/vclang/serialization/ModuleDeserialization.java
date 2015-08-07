package com.jetbrains.jetpad.vclang.serialization;

import com.jetbrains.jetpad.vclang.module.Module;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.definition.visitor.TypeChecking;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.*;

import java.io.*;
import java.util.*;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Error;

public class ModuleDeserialization {
  private final ModuleLoader myModuleLoader;
  private Definition myParent;

  public ModuleDeserialization(ModuleLoader moduleLoader) {
    myModuleLoader = moduleLoader;
  }

  public int readFile(File file, ClassDefinition module) throws IOException {
    return readStream(new DataInputStream(new BufferedInputStream(new FileInputStream(file))), module);
  }

  public int readStream(DataInputStream stream, ClassDefinition module) throws IOException {
    myParent = module;
    byte[] signature = new byte[4];
    stream.readFully(signature);
    if (!Arrays.equals(signature, ModuleSerialization.SIGNATURE)) {
      throw new IncorrectFormat();
    }
    int version = stream.readInt();
    if (version != ModuleSerialization.VERSION) {
      throw new WrongVersion(version);
    }
    int errorsNumber = stream.readInt();

    Map<Integer, Definition> definitionMap = new HashMap<>();
    definitionMap.put(0, myModuleLoader.rootModule());
    int size = stream.readInt();
    for (int i = 0; i < size; ++i) {
      int index = stream.readInt();
      Definition childModule;
      if (index == 0) {
        childModule = myModuleLoader.rootModule();
      } else {
        int parentIndex = stream.readInt();
        String name = stream.readUTF();
        int code = stream.read();

        Definition parent = definitionMap.get(parentIndex);
        if (parent == null) {
          throw new IncorrectFormat();
        }

        childModule = parent.getStaticField(name);
        if (childModule != null && childModule.getParent() != parent && parent instanceof ClassDefinition) {
          childModule = parent.getField(name);
        }

        if (childModule == null) {
          if (parent instanceof ClassDefinition && code == ModuleSerialization.CLASS_CODE) {
            childModule = myModuleLoader.loadModule(new Module((ClassDefinition) parent, name), true);
          }
          if (childModule == null) {
            childModule = newDefinition(code, new Utils.Name(name, Abstract.Definition.Fixity.PREFIX), parent);
          }
        }
      }

      definitionMap.put(index, childModule);
    }

    deserializeClassDefinition(stream, definitionMap, module);
    return errorsNumber;
  }

  public static Definition newDefinition(int code, Utils.Name name, Definition parent) throws IncorrectFormat {
    if (code == ModuleSerialization.OVERRIDDEN_CODE) {
      return new OverriddenDefinition(name, parent, Abstract.Definition.DEFAULT_PRECEDENCE, null);
    }
    if (code == ModuleSerialization.FUNCTION_CODE) {
      return new FunctionDefinition(name, parent, Abstract.Definition.DEFAULT_PRECEDENCE, null);
    }
    if (code == ModuleSerialization.DATA_CODE) {
      return new DataDefinition(name, parent, Abstract.Definition.DEFAULT_PRECEDENCE, null);
    }
    if (code == ModuleSerialization.CLASS_CODE) {
      ClassDefinition definition = new ClassDefinition(name.name, parent);
      definition.hasErrors(true);
      return definition;
    }
    if (code == ModuleSerialization.CONSTRUCTOR_CODE) {
      if (!(parent instanceof DataDefinition)) {
        throw new IncorrectFormat();
      }
      return new Constructor(-1, name, (DataDefinition) parent, Abstract.Definition.DEFAULT_PRECEDENCE);
    }
    throw new IncorrectFormat();
  }

  private void deserializeDefinition(DataInputStream stream, Map<Integer, Definition> definitionMap, Definition definition) throws IOException {
    int code = stream.read();
    definition.hasErrors(stream.readBoolean());
    if (code != ModuleSerialization.CONSTRUCTOR_CODE) {
      int size = stream.readInt();
      Set<Definition> dependencies = new HashSet<>();
      for (int i = 0; i < size; ++i) {
        dependencies.add(definitionMap.get(stream.readInt()));
      }
      definition.setDependencies(dependencies);
    }

    if (code == ModuleSerialization.FUNCTION_CODE || code == ModuleSerialization.OVERRIDDEN_CODE) {
      if (!(definition instanceof FunctionDefinition)) {
        throw new IncorrectFormat();
      }
      readDefinition(stream, definition);
      FunctionDefinition functionDefinition = (FunctionDefinition) definition;
      if (code == ModuleSerialization.OVERRIDDEN_CODE) {
        Definition overridden = definitionMap.get(stream.readInt());
        if (!(overridden instanceof FunctionDefinition && functionDefinition instanceof OverriddenDefinition)) {
          throw new IncorrectFormat();
        }
        ((OverriddenDefinition) functionDefinition).setOverriddenFunction((FunctionDefinition) overridden);
      }

      functionDefinition.typeHasErrors(stream.readBoolean());
      if (!functionDefinition.typeHasErrors()) {
        functionDefinition.setArguments(readArguments(stream, definitionMap));
        functionDefinition.setResultType(readExpression(stream, definitionMap));
      }
      int arrowCode = stream.read();
      if (arrowCode != 0 && arrowCode != 1 && arrowCode != 2) {
        throw new IncorrectFormat();
      }
      functionDefinition.setArrow(arrowCode == 0 ? null : arrowCode == 1 ? Abstract.Definition.Arrow.LEFT : Abstract.Definition.Arrow.RIGHT);
      if (!functionDefinition.hasErrors() && !functionDefinition.isAbstract()) {
        functionDefinition.setTerm(readExpression(stream, definitionMap));
      }

      int size = stream.readInt();
      for (int i = 0; i < size; ++i) {
        Definition member = definitionMap.get(stream.readInt());
        if (member.getParent() == definition) {
          deserializeDefinition(stream, definitionMap, member);
        }
        definition.addField(member, myModuleLoader.getErrors());
      }

    } else if (code == ModuleSerialization.DATA_CODE) {
      if (!(definition instanceof DataDefinition)) {
        throw new IncorrectFormat();
      }
      readDefinition(stream, definition);

      DataDefinition dataDefinition = (DataDefinition) definition;
      if (!dataDefinition.hasErrors()) {
        dataDefinition.setUniverse(readUniverse(stream));
        dataDefinition.setParameters(readTypeArguments(stream, definitionMap));
      }
      int constructorsNumber = stream.readInt();
      dataDefinition.setConstructors(new ArrayList<Constructor>(constructorsNumber), myModuleLoader.getErrors());
      for (int i = 0; i < constructorsNumber; ++i) {
        Constructor constructor = (Constructor) definitionMap.get(stream.readInt());
        if (constructor == null) {
          throw new IncorrectFormat();
        }
        constructor.setIndex(i);
        constructor.hasErrors(stream.readBoolean());
        readDefinition(stream, constructor);

        if (!constructor.hasErrors()) {
          constructor.setUniverse(readUniverse(stream));
          constructor.setArguments(readTypeArguments(stream, definitionMap));
        }

        dataDefinition.addConstructor(constructor, myModuleLoader.getErrors());
      }
    } else
    if (code == ModuleSerialization.CLASS_CODE) {
      if (!(definition instanceof ClassDefinition)) {
        throw new IncorrectFormat();
      }

      deserializeClassDefinition(stream, definitionMap, (ClassDefinition) definition);
    } else {
      throw new IncorrectFormat();
    }
  }

  private void deserializeClassDefinition(DataInputStream stream, Map<Integer, Definition> definitionMap, ClassDefinition definition) throws IOException {
    definition.setUniverse(readUniverse(stream));

    int size = stream.readInt();
    for (int i = 0; i < size; ++i) {
      Definition member = definitionMap.get(stream.readInt());
      if (member.getParent() == definition) {
        deserializeDefinition(stream, definitionMap, member);
      }
      definition.addField(member, myModuleLoader.getErrors());
      if (!definition.hasErrors()) {
        TypeChecking.checkOnlyStatic(myModuleLoader, definition, member, member.getName());
      }
    }

    size = stream.readInt();
    for (int i = 0; i < size; ++i) {
      Definition member = definitionMap.get(stream.readInt());
      definition.addField(member, myModuleLoader.getErrors());
    }

    definition.hasErrors(false);
  }

  private void readDefinition(DataInputStream stream, Definition definition) throws IOException {
    int assocCode = stream.read();
    Abstract.Definition.Associativity assoc;
    if (assocCode == 0) {
      assoc = Abstract.Definition.Associativity.LEFT_ASSOC;
    } else if (assocCode == 1) {
      assoc = Abstract.Definition.Associativity.RIGHT_ASSOC;
    } else if (assocCode == 2) {
      assoc = Abstract.Definition.Associativity.NON_ASSOC;
    } else {
      throw new IncorrectFormat();
    }
    definition.setPrecedence(new Abstract.Definition.Precedence(assoc, stream.readByte()));

    int fixityCode = stream.read();
    if (fixityCode == 0) {
      definition.setFixity(Abstract.Definition.Fixity.INFIX);
    } else if (fixityCode == 1) {
      definition.setFixity(Abstract.Definition.Fixity.PREFIX);
    } else {
      throw new IncorrectFormat();
    }
  }

  public Universe readUniverse(DataInputStream stream) throws IOException {
    int level = stream.readInt();
    int truncated = stream.readInt();
    return new Universe.Type(level, truncated);
  }

  public List<Argument> readArguments(DataInputStream stream, Map<Integer, Definition> definitionMap) throws IOException {
    int size = stream.readInt();
    List<Argument> result = new ArrayList<>(size);
    for (int i = 0; i < size; ++i) {
      result.add(readArgument(stream, definitionMap));
    }
    return result;
  }

  public List<NameArgument> readNameArguments(DataInputStream stream, Map<Integer, Definition> definitionMap) throws IOException {
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

  public List<TypeArgument> readTypeArguments(DataInputStream stream, Map<Integer, Definition> definitionMap) throws IOException {
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

  public Argument readArgument(DataInputStream stream, Map<Integer, Definition> definitionMap) throws IOException {
    boolean explicit = stream.readBoolean();
    int code = stream.read();
    if (code == 0) {
      int size = stream.readInt();
      List<String> names = new ArrayList<>(size);
      for (int i = 0; i < size; ++i) {
        names.add(stream.readBoolean() ? stream.readUTF() : null);
      }
      return new TelescopeArgument(explicit, names, readExpression(stream, definitionMap));
    } else if (code == 1) {
      return new TypeArgument(explicit, readExpression(stream, definitionMap));
    } else if (code == 2) {
      return new NameArgument(explicit, stream.readBoolean() ? stream.readUTF() : null);
    } else {
      throw new IncorrectFormat();
    }
  }

  public Expression readExpression(DataInputStream stream, Map<Integer, Definition> definitionMap) throws IOException {
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
        boolean isFieldAcc = stream.readBoolean();
        Expression expression = isFieldAcc ? readExpression(stream, definitionMap) : null;
        Definition definition = definitionMap.get(stream.readInt());
        if (definition == null) {
          throw new IncorrectFormat();
        }
        int size = stream.readInt();
        List<Expression> parameters = size == 0 ? null : new ArrayList<Expression>(size);
        for (int i = 0; i < size; ++i) {
          parameters.add(readExpression(stream, definitionMap));
        }
        return DefCall(expression, definition, parameters);
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
        return Error(stream.readBoolean() ? readExpression(stream, definitionMap) : null, new TypeCheckingError(myParent, "Deserialized error", null, null));
      }
      case 10: {
        int size = stream.readInt();
        List<Expression> fields = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
          fields.add(readExpression(stream, definitionMap));
        }
        return Tuple(fields, (SigmaExpression) readExpression(stream, definitionMap));
      }
      case 11: {
        return Sigma(readTypeArguments(stream, definitionMap));
      }
      case 12: {
        int index = stream.readInt();
        int clausesNumber = stream.readInt();
        List<Clause> clauses = new ArrayList<>(clausesNumber);
        for (int i = 0; i < clausesNumber; ++i) {
          clauses.add(readClause(stream, definitionMap, false));
        }
        ElimExpression result = Elim(Index(index), clauses, stream.readBoolean() ? readClause(stream, definitionMap, true) : null);
        for (Clause clause : result.getClauses()) {
          if (clause != null) {
            clause.setElimExpression(result);
          }
        }
        if (result.getOtherwise() != null) {
          result.getOtherwise().setElimExpression(result);
        }
        return result;
      }
      case 14: {
        Expression expr = readExpression(stream, definitionMap);
        return Proj(expr, stream.readInt());
      }
      case 15: {
        Definition definition = definitionMap.get(stream.readInt());
        if (!(definition instanceof ClassDefinition)) {
          throw new IncorrectFormat();
        }
        Map<FunctionDefinition, OverriddenDefinition> map = new HashMap<>();
        int size = stream.readInt();
        for (int i = 0; i < size; ++i) {
          Definition overridden = definitionMap.get(stream.readInt());
          if (!(overridden instanceof FunctionDefinition)) {
            throw new IncorrectFormat();
          }
          OverriddenDefinition overriding = (OverriddenDefinition) newDefinition(ModuleSerialization.OVERRIDDEN_CODE, overridden.getName(), overridden.getParent());
          deserializeDefinition(stream, definitionMap, overriding);
          map.put((FunctionDefinition) overridden, overriding);
        }
        return ClassExt((ClassDefinition) definition, map, readUniverse(stream));
      }
      case 16: {
        return New(readExpression(stream, definitionMap));
      }
      case 17: {
        final int numClauses = stream.readInt();
        final List<LetClause> clauses = new ArrayList<>(numClauses);
        for (int i = 0; i < numClauses; i++) {
          clauses.add(readLetClause(stream, definitionMap));
        }
        final Expression expr = readExpression(stream, definitionMap);
        return Let(clauses, expr);
      }
      default: {
        throw new IncorrectFormat();
      }
    }
  }

  private LetClause readLetClause(DataInputStream stream, Map<Integer, Definition> definitionMap) throws IOException {
    final String name = stream.readUTF();
    final List<Argument> arguments = readArguments(stream, definitionMap);
    final Expression resultType = stream.readBoolean() ? readExpression(stream, definitionMap) : null;
    final Abstract.Definition.Arrow arrow = stream.readBoolean() ? Abstract.Definition.Arrow.RIGHT : Abstract.Definition.Arrow.LEFT;
    final Expression term = readExpression(stream, definitionMap);
    return let(name, arguments, resultType, arrow, term);

  }

  public Clause readClause(DataInputStream stream, Map<Integer, Definition> definitionMap, boolean isOtherwise) throws IOException {
    Definition definition = null;
    List<NameArgument> arguments = null;
    if (!isOtherwise) {
      int index = stream.readInt();
      if (index == -1) {
        return null;
      }

      definition = definitionMap.get(index);
      if (!(definition instanceof Constructor)) {
        throw new IncorrectFormat();
      }
      arguments = readNameArguments(stream, definitionMap);
    }
    Abstract.Definition.Arrow arrow = stream.readBoolean() ? Abstract.Definition.Arrow.RIGHT : Abstract.Definition.Arrow.LEFT;
    return new Clause((Constructor) definition, arguments, arrow, readExpression(stream, definitionMap), null);
  }

  public static class DeserializationException extends IOException {
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

  public class WrongVersion extends DeserializationException {
    WrongVersion(int version) {
      super("Version of the file format (" + version + ") differs from the version of the program + (" + ModuleSerialization.VERSION + ")");
    }
  }
}
