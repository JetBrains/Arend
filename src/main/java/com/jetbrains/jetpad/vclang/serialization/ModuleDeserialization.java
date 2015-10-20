package com.jetbrains.jetpad.vclang.serialization;

import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.module.ModuleLoadingResult;
import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.NameArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.pattern.AnyConstructorPattern;
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
  private ResolvedName myResolvedName;

  public ModuleDeserialization(ModuleLoader moduleLoader) {
    myModuleLoader = moduleLoader;
  }

  public ModuleLoadingResult readFile(File file, ResolvedName resolvedName) throws IOException {
    return readStream(new DataInputStream(new BufferedInputStream(new FileInputStream(file))), resolvedName);
  }

  public ModuleLoadingResult readStream(DataInputStream stream, ResolvedName resolvedName) throws IOException {
    ClassDefinition classDefinition = new ClassDefinition(resolvedName.parent, resolvedName.name);

    myResolvedName = resolvedName;
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
    // TODO
    /*
    definitionMap.put(0, RootModule.ROOT);
    int size = stream.readInt();
    for (int i = 0; i < size; ++i) {
      int index = stream.readInt();
      int parentIndex = stream.readInt();
      NamespaceMember child;
      if (parentIndex == 0) {
        child = index == 1 ? RootModule.ROOT : new Namespace("<local>");
      } else {
        Abstract.Definition.Fixity fixity = stream.readBoolean() ? Abstract.Definition.Fixity.PREFIX : Abstract.Definition.Fixity.INFIX;
        String name = stream.readUTF();
        Name name1 = new Name(name, fixity);
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
          ModuleLoadingResult result = myModuleLoader.load(new ResolvedName((Namespace) parent, name), true);
          child = result == null || result.definition == null ? parentNamespace.getChild(name1) : result.namespace;
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
    */

    deserializeNamespace(stream, definitionMap);
    if (stream.readBoolean()) {
      deserializeClassDefinition(stream, definitionMap, classDefinition);
    }
    return new ModuleLoadingResult(new NamespaceMember(classDefinition.getParentNamespace().getChild(classDefinition.getName()), null, classDefinition), false, errorsNumber);
  }

  public static Definition newDefinition(int code, Name name, Namespace parent) throws IncorrectFormat {
    if (code == ModuleSerialization.OVERRIDDEN_CODE) {
      return new OverriddenDefinition(parent, name, Abstract.Definition.DEFAULT_PRECEDENCE, null);
    }
    if (code == ModuleSerialization.FUNCTION_CODE) {
      return new FunctionDefinition(parent, name, Abstract.Definition.DEFAULT_PRECEDENCE, null);
    }
    if (code == ModuleSerialization.DATA_CODE) {
      return new DataDefinition(parent, name, Abstract.Definition.DEFAULT_PRECEDENCE);
    }
    if (code == ModuleSerialization.CLASS_CODE) {
      return new ClassDefinition(parent, name);
    }
    if (code == ModuleSerialization.CONSTRUCTOR_CODE) {
      return new Constructor(parent, name, Abstract.Definition.DEFAULT_PRECEDENCE, null);
    }
    throw new IncorrectFormat();
  }

  private void deserializeDefinition(DataInputStream stream, Map<Integer, Definition> definitionMap, Definition definition) throws IOException {
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
        constructor.setDataType(dataDefinition);
        constructor.hasErrors(stream.readBoolean());
        readDefinition(stream, constructor);

        if (!constructor.hasErrors()) {
          constructor.setUniverse(readUniverse(stream));
          constructor.setArguments(readTypeArguments(stream, definitionMap));
        }

        dataDefinition.addConstructor(constructor);
        dataDefinition.getParentNamespace().addDefinition(constructor);
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

  private void deserializeFunctionDefinition(DataInputStream stream, Map<Integer, Definition> definitionMap, FunctionDefinition definition, int code) throws IOException {
    readDefinition(stream, definition);

    if (code == ModuleSerialization.OVERRIDDEN_CODE) {
      Definition overridden = definitionMap.get(stream.readInt());
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

  private void deserializeNamespace(DataInputStream stream, Map<Integer, Definition> definitionMap) throws IOException {
    int size = stream.readInt();
    for (int i = 0; i < size; ++i) {
      definitionMap.get(stream.readInt());
      deserializeNamespace(stream, definitionMap);
      if (stream.readBoolean()) {
        Definition definition = definitionMap.get(stream.readInt());
        deserializeDefinition(stream, definitionMap, definition);
      }
    }
  }

  private void deserializeClassDefinition(DataInputStream stream, Map<Integer, Definition> definitionMap, ClassDefinition definition) throws IOException {
    definition.setUniverse(readUniverse(stream));
    /* TODO
    Definition definition1 = definitionMap.get(stream.readInt());
    if (!(namespaceMember instanceof Namespace)) {
      throw new IncorrectFormat();
    }
    definition.setLocalNamespace((Namespace) namespaceMember);
    */
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
        return Error(stream.readBoolean() ? readExpression(stream, definitionMap) : null, new TypeCheckingError(myResolvedName, "Deserialized error", null, null));
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
        int numExpressions = stream.readInt();
        List<IndexExpression> expressions = new ArrayList<>(numExpressions);
        for (int i = 0; i < numExpressions; i++) {
          expressions.add(Index(stream.readInt()));
        }
        int clausesNumber = stream.readInt();
        List<Clause> clauses = new ArrayList<>(clausesNumber);
        for (int i = 0; i < clausesNumber; ++i) {
          clauses.add(readClause(stream, definitionMap));
        }
        ElimExpression result = Elim(expressions, clauses);
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
        Expression baseClassExpression = readExpression(stream, definitionMap);
        if (!(baseClassExpression instanceof DefCallExpression)) {
          throw new IncorrectFormat();
        }
        Map<FunctionDefinition, OverriddenDefinition> map = new HashMap<>();
        int size = stream.readInt();
        for (int i = 0; i < size; ++i) {
          Definition overridden = definitionMap.get(stream.readInt());
          if (!(overridden instanceof FunctionDefinition)) {
            throw new IncorrectFormat();
          }
          OverriddenDefinition overriding = (OverriddenDefinition) newDefinition(ModuleSerialization.OVERRIDDEN_CODE, overridden.getName(), overridden.getParentNamespace());
          deserializeDefinition(stream, definitionMap, overriding);
          map.put((FunctionDefinition) overridden, overriding);
        }
        return ClassExt((DefCallExpression) baseClassExpression, map, readUniverse(stream));
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

  public Pattern readPattern(DataInputStream stream, Map<Integer, Definition> definitionMap) throws IOException {
    boolean isExplicit = stream.readBoolean();
    switch (stream.readInt()) {
      case 0: {
        String name = stream.readBoolean() ? stream.readUTF() : null;
        return new NamePattern(name, isExplicit);
      }
      case 1: {
        return new AnyConstructorPattern(isExplicit);
      }
      case 2: {
        Definition constructor = definitionMap.get(stream.readInt());
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
      default: {
        throw new IllegalStateException();
      }
    }
  }

  public Clause readClause(DataInputStream stream, Map<Integer, Definition> definitionMap) throws IOException {
    int numPatterns = stream.readInt();
    List<Pattern> patterns = new ArrayList<>(numPatterns);
    for (int i = 0; i < numPatterns; i++)
      patterns.add(readPattern(stream, definitionMap));
    Abstract.Definition.Arrow arrow = stream.readBoolean() ? Abstract.Definition.Arrow.RIGHT : Abstract.Definition.Arrow.LEFT;
    return new Clause(patterns, arrow, readExpression(stream, definitionMap), null);
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
    private final Name myName;

    public NameIsAlreadyDefined(Name name) {
      super("Name is already defined");
      myName = name;
    }

    @Override
    public String toString() {
      return myName + ": " + super.toString();
    }
  }

  public static class NameDoesNotDefined extends DeserializationException {
    private final Name myName;

    public NameDoesNotDefined(Name name) {
      super("Name is not defined");
      myName = name;
    }

    @Override
    public String toString() {
      return myName + ": " + super.toString();
    }
  }
}
