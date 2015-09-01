package com.jetbrains.jetpad.vclang.serialization;

import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.module.ModuleLoadingResult;
import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.module.RootModule;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.*;
import com.jetbrains.jetpad.vclang.term.pattern.ConstructorPattern;
import com.jetbrains.jetpad.vclang.term.pattern.NamePattern;
import com.jetbrains.jetpad.vclang.term.pattern.Pattern;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;

import java.io.*;
import java.util.*;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Error;

public class ModuleDeserialization {
  private final ModuleLoader myModuleLoader;
  private Namespace myParent;

  public ModuleDeserialization(ModuleLoader moduleLoader) {
    myModuleLoader = moduleLoader;
  }

  public int readFile(File file, Namespace namespace, ClassDefinition classDefinition) throws IOException {
    return readStream(new DataInputStream(new BufferedInputStream(new FileInputStream(file))), namespace, classDefinition);
  }

  public int readStream(DataInputStream stream, Namespace namespace, ClassDefinition classDefinition) throws IOException {
    myParent = namespace;
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

    Map<Integer, NamespaceMember> definitionMap = new HashMap<>();
    definitionMap.put(0, RootModule.ROOT);
    int size = stream.readInt();
    for (int i = 0; i < size; ++i) {
      int index = stream.readInt();
      int parentIndex = stream.readInt();
      NamespaceMember child;
      if (parentIndex == 0) {
        child = index == 1 ? RootModule.ROOT : new Namespace(new Utils.Name("<local>"), null);
      } else {
        Abstract.Definition.Fixity fixity = stream.readBoolean() ? Abstract.Definition.Fixity.PREFIX : Abstract.Definition.Fixity.INFIX;
        String name = stream.readUTF();
        Utils.Name name1 = new Utils.Name(name, fixity);
        int code = stream.read();
        boolean isNew = false;
        if (code != ModuleSerialization.NAMESPACE_CODE) {
          isNew = stream.readBoolean();
        }

        NamespaceMember parent = definitionMap.get(parentIndex);
        if (!(parent instanceof Namespace)) {
          throw new IncorrectFormat();
        }

        Namespace parentNamespace = (Namespace) parent;
        if (code == ModuleSerialization.NAMESPACE_CODE) {
          ModuleLoadingResult result = myModuleLoader.load((Namespace) parent, name, true);
          child = result == null || result.classDefinition == null ? parentNamespace.getChild(name1) : result.classDefinition.getNamespace();
        } else {
          if (isNew) {
            child = newDefinition(code, name1, parentNamespace);
            if (parentNamespace.addDefinition((Definition) child) != null) {
              throw new NameIsAlreadyDefined(name1);
            }
          } else {
            child = parentNamespace.getDefinition(name);
            if (child == null) {
              throw new NameDoesNotDefined(name1);
            }
          }
        }
      }

      definitionMap.put(index, child);
    }

    deserializeNamespace(stream, definitionMap);
    if (stream.readBoolean()) {
      if (classDefinition == null) {
        throw new NameIsAlreadyDefined(namespace.getName());
      } else {
        deserializeClassDefinition(stream, definitionMap, classDefinition);
      }
    }
    return errorsNumber;
  }

  public static Definition newDefinition(int code, Utils.Name name, Namespace parent) throws IncorrectFormat {
    if (code == ModuleSerialization.OVERRIDDEN_CODE) {
      return new OverriddenDefinition(parent.getChild(name), Abstract.Definition.DEFAULT_PRECEDENCE, null);
    }
    if (code == ModuleSerialization.FUNCTION_CODE) {
      return new FunctionDefinition(parent.getChild(name), Abstract.Definition.DEFAULT_PRECEDENCE, null);
    }
    if (code == ModuleSerialization.DATA_CODE) {
      return new DataDefinition(parent.getChild(name), Abstract.Definition.DEFAULT_PRECEDENCE);
    }
    if (code == ModuleSerialization.CLASS_CODE) {
      return new ClassDefinition(parent.getChild(name));
    }
    if (code == ModuleSerialization.CONSTRUCTOR_CODE) {
      return new Constructor(-1, parent.getChild(name), Abstract.Definition.DEFAULT_PRECEDENCE, null);
    }
    throw new IncorrectFormat();
  }

  private void deserializeDefinition(DataInputStream stream, Map<Integer, NamespaceMember> definitionMap, Definition definition) throws IOException {
    int code = stream.read();
    definition.hasErrors(stream.readBoolean());

    if (code == ModuleSerialization.FUNCTION_CODE || code == ModuleSerialization.OVERRIDDEN_CODE) {
      if (!(definition instanceof FunctionDefinition)) {
        throw new IncorrectFormat();
      }

      deserializeFunctionDefinition(stream, definitionMap, (FunctionDefinition) definition, code);
    } else
    if (code == ModuleSerialization.DATA_CODE) {
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
      for (int i = 0; i < constructorsNumber; ++i) {
        Constructor constructor = (Constructor) definitionMap.get(stream.readInt());
        if (constructor == null) {
          throw new IncorrectFormat();
        }
        constructor.setIndex(i);
        constructor.setDataType(dataDefinition);
        constructor.hasErrors(stream.readBoolean());
        readDefinition(stream, constructor);

        if (!constructor.hasErrors()) {
          constructor.setUniverse(readUniverse(stream));
          constructor.setArguments(readTypeArguments(stream, definitionMap));
        }

        dataDefinition.addConstructor(constructor);
        dataDefinition.getNamespace().getParent().addDefinition(constructor);
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

  private void deserializeFunctionDefinition(DataInputStream stream, Map<Integer, NamespaceMember> definitionMap, FunctionDefinition definition, int code) throws IOException {
    readDefinition(stream, definition);

    if (code == ModuleSerialization.OVERRIDDEN_CODE) {
      NamespaceMember overridden = definitionMap.get(stream.readInt());
      if (!(overridden instanceof FunctionDefinition && definition instanceof OverriddenDefinition)) {
        throw new IncorrectFormat();
      }
      ((OverriddenDefinition) definition).setOverriddenFunction((FunctionDefinition) overridden);
    }

    definition.typeHasErrors(stream.readBoolean());
    if (!definition.typeHasErrors()) {
      definition.setArguments(readArguments(stream, definitionMap));
      definition.setResultType(readExpression(stream, definitionMap));
    }
    int arrowCode = stream.read();
    if (arrowCode != 0 && arrowCode != 1 && arrowCode != 2) {
      throw new IncorrectFormat();
    }
    definition.setArrow(arrowCode == 0 ? null : arrowCode == 1 ? Abstract.Definition.Arrow.LEFT : Abstract.Definition.Arrow.RIGHT);
    if (!definition.hasErrors() && !definition.isAbstract()) {
      definition.setTerm(readExpression(stream, definitionMap));
    }
  }

  private void deserializeNamespace(DataInputStream stream, Map<Integer, NamespaceMember> definitionMap) throws IOException {
    int size = stream.readInt();
    for (int i = 0; i < size; ++i) {
      NamespaceMember member = definitionMap.get(stream.readInt());
      if (!(member instanceof Namespace)) {
        throw new IncorrectFormat();
      }
      deserializeNamespace(stream, definitionMap);
    }

    size = stream.readInt();
    for (int i = 0; i < size; ++i) {
      NamespaceMember member = definitionMap.get(stream.readInt());
      if (!(member instanceof Definition)) {
        throw new IncorrectFormat();
      }
      deserializeDefinition(stream, definitionMap, (Definition) member);
    }
  }

  private void deserializeClassDefinition(DataInputStream stream, Map<Integer, NamespaceMember> definitionMap, ClassDefinition definition) throws IOException {
    definition.setUniverse(readUniverse(stream));
    NamespaceMember namespaceMember = definitionMap.get(stream.readInt());
    if (!(namespaceMember instanceof Namespace)) {
      throw new IncorrectFormat();
    }
    definition.setLocalNamespace((Namespace) namespaceMember);
    deserializeNamespace(stream, definitionMap);
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

  public List<Argument> readArguments(DataInputStream stream, Map<Integer, NamespaceMember> definitionMap) throws IOException {
    int size = stream.readInt();
    List<Argument> result = new ArrayList<>(size);
    for (int i = 0; i < size; ++i) {
      result.add(readArgument(stream, definitionMap));
    }
    return result;
  }

  public List<NameArgument> readNameArguments(DataInputStream stream, Map<Integer, NamespaceMember> definitionMap) throws IOException {
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

  public List<TypeArgument> readTypeArguments(DataInputStream stream, Map<Integer, NamespaceMember> definitionMap) throws IOException {
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

  public Argument readArgument(DataInputStream stream, Map<Integer, NamespaceMember> definitionMap) throws IOException {
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

  public Expression readExpression(DataInputStream stream, Map<Integer, NamespaceMember> definitionMap) throws IOException {
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
        NamespaceMember definition = definitionMap.get(stream.readInt());
        if (!(definition instanceof Definition)) {
          throw new IncorrectFormat();
        }
        int size = stream.readInt();
        List<Expression> parameters = size == 0 ? null : new ArrayList<Expression>(size);
        for (int i = 0; i < size; ++i) {
          parameters.add(readExpression(stream, definitionMap));
        }
        return DefCall(expression, (Definition) definition, parameters);
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
          clauses.add(readClause(stream, definitionMap));
        }
        ElimExpression result = Elim(Index(index), clauses);
        for (Clause clause : result.getClauses()) {
          clause.setElimExpression(result);
        }

        return result;
      }
      case 14: {
        Expression expr = readExpression(stream, definitionMap);
        return Proj(expr, stream.readInt());
      }
      case 15: {
        NamespaceMember definition = definitionMap.get(stream.readInt());
        if (!(definition instanceof ClassDefinition)) {
          throw new IncorrectFormat();
        }
        Map<FunctionDefinition, OverriddenDefinition> map = new HashMap<>();
        int size = stream.readInt();
        for (int i = 0; i < size; ++i) {
          NamespaceMember overridden = definitionMap.get(stream.readInt());
          if (!(overridden instanceof FunctionDefinition)) {
            throw new IncorrectFormat();
          }
          OverriddenDefinition overriding = (OverriddenDefinition) newDefinition(ModuleSerialization.OVERRIDDEN_CODE, overridden.getName(), overridden.getNamespace().getParent());
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

  private LetClause readLetClause(DataInputStream stream, Map<Integer, NamespaceMember> definitionMap) throws IOException {
    final String name = stream.readUTF();
    final List<Argument> arguments = readArguments(stream, definitionMap);
    final Expression resultType = stream.readBoolean() ? readExpression(stream, definitionMap) : null;
    final Abstract.Definition.Arrow arrow = stream.readBoolean() ? Abstract.Definition.Arrow.RIGHT : Abstract.Definition.Arrow.LEFT;
    final Expression term = readExpression(stream, definitionMap);
    return let(name, arguments, resultType, arrow, term);
  }

  public Pattern readPattern(DataInputStream stream, Map<Integer, NamespaceMember> definitionMap) throws IOException {
    boolean isExplicit = stream.readBoolean();
    if (stream.readBoolean()) {
      String name = stream.readBoolean() ? stream.readUTF() : null;
      return new NamePattern(name, isExplicit);
    } else {
      NamespaceMember constructor = definitionMap.get(stream.readInt());
      if (!(constructor instanceof Constructor)) {
        throw new IncorrectFormat();
      }
      int size = stream.readInt();
      List<Pattern> arguments = new ArrayList<>(size);
      for (int i = 0; i < size; ++i) {
        arguments.add(readPattern(stream, definitionMap));
      }
      return new ConstructorPattern((Constructor) constructor, arguments, isExplicit);
    }
  }

  public Clause readClause(DataInputStream stream, Map<Integer, NamespaceMember> definitionMap) throws IOException {
    Pattern pattern = readPattern(stream, definitionMap);
    Abstract.Definition.Arrow arrow = stream.readBoolean() ? Abstract.Definition.Arrow.RIGHT : Abstract.Definition.Arrow.LEFT;
    return new Clause(pattern, arrow, readExpression(stream, definitionMap), null);
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

  public static class WrongVersion extends DeserializationException {
    WrongVersion(int version) {
      super("Version of the file format (" + version + ") differs from the version of the program + (" + ModuleSerialization.VERSION + ")");
    }
  }

  public static class NameIsAlreadyDefined extends DeserializationException {
    private final Utils.Name myName;

    public NameIsAlreadyDefined(Utils.Name name) {
      super("Name is already defined");
      myName = name;
    }

    @Override
    public String toString() {
      return myName + ": " + super.toString();
    }
  }

  public static class NameDoesNotDefined extends DeserializationException {
    private final Utils.Name myName;

    public NameDoesNotDefined(Utils.Name name) {
      super("Name does not defined");
      myName = name;
    }

    @Override
    public String toString() {
      return myName + ": " + super.toString();
    }
  }
}
