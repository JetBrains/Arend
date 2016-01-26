package com.jetbrains.jetpad.vclang.serialization;

import com.jetbrains.jetpad.vclang.module.ModuleLoadingResult;
import com.jetbrains.jetpad.vclang.module.RootModule;
import com.jetbrains.jetpad.vclang.module.output.Output;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.LinkList;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.NonDependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.TypedDependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.UntypedDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.pattern.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.BranchElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.EmptyElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;

import java.io.*;
import java.util.*;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Error;

public class ModuleDeserialization {
  private ResolvedName myResolvedName;
  private final List<Binding> myBindingMap = new ArrayList<>();

  public ModuleDeserialization() {
  }

  public ModuleLoadingResult readFile(File file, ResolvedName module) throws IOException {
    return readStream(new DataInputStream(new BufferedInputStream(new FileInputStream(file))), module);
  }

  public static void readStubsFromFile(File file, ResolvedName module) throws IOException {
    readStubsFromStream(new DataInputStream(new BufferedInputStream(new FileInputStream(file))), module);
  }

  public static void readStubsFromStream(DataInputStream stream, ResolvedName module) throws IOException {
    verifySignature(stream);
    readHeader(stream);
    stream.readInt();

    module.parent.getChild(module.name.name);

    readDefIndicies(stream, true, module);
  }

  public static Output.Header readHeaderFromFile(File file) throws IOException {
    return readHeaderFromStream(new DataInputStream(new BufferedInputStream(new FileInputStream(file))));
  }

  public static Output.Header readHeaderFromStream(DataInputStream stream) throws IOException {
    verifySignature(stream);
    return readHeader(stream);
  }

  private static List<String> readFullPath(DataInputStream stream) throws IOException {
    List<String> result = new ArrayList<>();
    int size = stream.readInt();
    for (int i = 0; i < size; i++) {
      result.add(stream.readUTF());
    }

    return result;
  }

  private static Output.Header readHeader(DataInputStream stream) throws IOException {
    Output.Header result = new Output.Header(new ArrayList<List<String>>(), new ArrayList<String>());
    int size = stream.readInt();
    for (int i = 0; i < size; i++) {
      result.provided.add(stream.readUTF());
    }
    size = stream.readInt();
    for (int i = 0; i < size; i++) {
      result.dependencies.add(readFullPath(stream));
    }

    return result;
  }

  private static Name readName(DataInputStream stream) throws IOException {
    String name = stream.readUTF();
    Abstract.Definition.Fixity fixity = stream.read() == 1 ? Abstract.Definition.Fixity.PREFIX : Abstract.Definition.Fixity.INFIX;
    return new Name(name, fixity);
   }

  private static Definition readDefinition(DataInputStream stream, ResolvedName rn, boolean dryRun) throws IOException {
    int codeIdx = stream.readInt();
    if (codeIdx >= ModuleSerialization.DefinitionCodes.values().length)
      throw new IncorrectFormat();
    ModuleSerialization.DefinitionCodes code = ModuleSerialization.DefinitionCodes.values()[codeIdx];

    Abstract.Definition.Precedence precedence = null;
    if (code != ModuleSerialization.DefinitionCodes.CLASS_CODE) {
      Abstract.Definition.Associativity associativity;
      int assoc = stream.read();
      if (assoc == 0) {
        associativity = Abstract.Definition.Associativity.LEFT_ASSOC;
      } else if (assoc == 1) {
        associativity = Abstract.Definition.Associativity.RIGHT_ASSOC;
      } else {
        associativity = Abstract.Definition.Associativity.NON_ASSOC;
      }
      byte priority = stream.readByte();
      precedence = new Abstract.Definition.Precedence(associativity, priority);
    }
    Name name = readName(stream);

    if (dryRun)
      return null;

    Definition definition = code.toDefinition(name, rn.parent, precedence);
    if (rn.name.name.equals("\\parent"))
      ((ClassDefinition) rn.parent.getResolvedName().toDefinition()).addField((ClassField) definition);
    else {
      rn.parent.addDefinition(definition);
    }
    return definition;
  }

  private static void verifySignature(DataInputStream stream) throws IOException {
    byte[] signature = new byte[4];
    stream.readFully(signature);
    if (!Arrays.equals(signature, ModuleSerialization.SIGNATURE)) {
      throw new IncorrectFormat();
    }
    int version = stream.readInt();
    if (version != ModuleSerialization.VERSION) {
      throw new WrongVersion(version);
    }
  }

  private static ResolvedName fullPathToRelativeResolvedName(List<String> path, ResolvedName module) {
    ResolvedName result = module;
    for (String aPath : path) {
      result = result.toNamespace().getChild(aPath).getResolvedName();
    }
    return result;
  }

  private static ResolvedName fullPathToResolvedName(List<String> path) throws IOException {
    return fullPathToRelativeResolvedName(path, RootModule.ROOT.getResolvedName());
  }

  private static Map<Integer, Definition> readDefIndicies(DataInputStream stream, boolean createStubs, ResolvedName module) throws IOException {
    Map<Integer, Definition> result = new HashMap<>();

    int size = stream.readInt();
    for (int i = 0; i < size; i++) {
      ResolvedName rn;
      if (stream.readBoolean()) {
        rn = fullPathToRelativeResolvedName(readFullPath(stream), module);
        readDefinition(stream, rn, !createStubs);
      } else {
        rn = fullPathToResolvedName(readFullPath(stream));
      }
      if (!createStubs) {
        result.put(i, rn.name.name.equals("\\parent") ?
            ((ClassDefinition) rn.parent.getResolvedName().toDefinition()).getField("\\parent") : rn.toDefinition());
      }
    }

    return createStubs ? null : result;
  }

  public ModuleLoadingResult readStream(DataInputStream stream, ResolvedName module) throws IOException {
    myResolvedName = module;

    verifySignature(stream);
    readHeader(stream);
    int errorsNumber = stream.readInt();
    Map<Integer, Definition> definitionMap = readDefIndicies(stream, false, module);
    Definition moduleRoot = definitionMap.get(0);
    deserializeDefinition(stream, definitionMap);
    return new ModuleLoadingResult(new NamespaceMember(moduleRoot.getResolvedName().toNamespace(), null, moduleRoot), false, errorsNumber);
  }

  private Definition deserializeDefinition(DataInputStream stream, Map<Integer, Definition> definitionMap) throws IOException {
    Definition definition = definitionMap.get(stream.readInt());
    if (stream.readBoolean())
      definition.setThisClass((ClassDefinition) definitionMap.get(stream.readInt()));
    definition.hasErrors(stream.readBoolean());

    if (definition instanceof FunctionDefinition) {
      deserializeFunctionDefinition(stream, definitionMap, (FunctionDefinition) definition);
    } else if (definition instanceof DataDefinition) {
      deserializeDataDefinition(stream, definitionMap, (DataDefinition) definition);
    } else if (definition instanceof ClassDefinition) {
      deserializeClassDefinition(stream, definitionMap, (ClassDefinition) definition);
    } else {
      throw new IncorrectFormat();
    }
    return definition;
  }

  private void deserializeDataDefinition(DataInputStream stream, Map<Integer, Definition> definitionMap, DataDefinition definition) throws IOException {
    if (!definition.hasErrors()) {
      definition.setUniverse(readUniverse(stream));
      definition.setParameters(readParameters(stream, definitionMap));
    }

    int constructorsNumber = stream.readInt();
    for (int i = 0; i < constructorsNumber; ++i) {
      Constructor constructor = (Constructor) definitionMap.get(stream.readInt());
      if (constructor == null) {
        throw new IncorrectFormat();
      }
      constructor.setDataType(definition);
      constructor.hasErrors(stream.readBoolean());

      if (!constructor.hasErrors()) {
        if (stream.readBoolean()) {
          int numPatterns = stream.readInt();
          List<PatternArgument> patterns = new ArrayList<>(numPatterns);
          for (int j = 0; j < numPatterns; j++) {
            patterns.add(readPatternArg(stream, definitionMap));
          }
          constructor.setPatterns(new Patterns(patterns));
        }
        constructor.setUniverse(readUniverse(stream));
        constructor.setParameters(readParameters(stream, definitionMap));
      }

      definition.addConstructor(constructor);
      definition.getParentNamespace().addDefinition(constructor);
    }
  }

  private void deserializeFunctionDefinition(DataInputStream stream, Map<Integer, Definition> definitionMap, FunctionDefinition definition) throws IOException {
    deserializeNamespace(stream, definitionMap, definition);

    definition.typeHasErrors(stream.readBoolean());
    if (!definition.typeHasErrors()) {
      definition.setParameters(readParameters(stream, definitionMap));
      definition.setResultType(readExpression(stream, definitionMap));
    }

    if (stream.readBoolean()) {
      definition.setElimTree(readElimTree(stream, definitionMap));
    }
  }

  private ElimTreeNode readElimTree(DataInputStream stream, Map<Integer, Definition> definitionMap) throws IOException {
    switch (stream.readInt()) {
      case 0: {
        Binding binding = myBindingMap.get(stream.readInt());
        int contextTailSize = stream.readInt();
        List<Binding> contextTail = new ArrayList<>(contextTailSize);
        for (int i = 0; i < contextTailSize; i++) {
          contextTail.add(myBindingMap.get(stream.readInt()));
        }
        BranchElimTreeNode elimTree = new BranchElimTreeNode(binding, contextTail);
        int size = stream.readInt();
        for (int i = 0; i < size; i++) {
          Constructor constructor = (Constructor) definitionMap.get(stream.readInt());
          DependentLink parameters = readParameters(stream, definitionMap);
          List<Binding> tailBindings = new ArrayList<>(contextTailSize);
          for (int j = 0; j < contextTailSize; j++) {
            tailBindings.add(readTypedBinding(stream, definitionMap));
          }
          elimTree.addClause(constructor, parameters, tailBindings, readElimTree(stream, definitionMap));
        }
        return elimTree;
      }
      case 1: {
        return new LeafElimTreeNode(stream.readBoolean() ? Abstract.Definition.Arrow.RIGHT : Abstract.Definition.Arrow.LEFT, readExpression(stream, definitionMap));
      }
      case 2: {
        return EmptyElimTreeNode.getInstance();
      }
      default:
        throw new IllegalStateException();
    }
  }

  private void deserializeNamespace(DataInputStream stream, Map<Integer, Definition> definitionMap, Definition parent) throws IOException {
    int size = stream.readInt();
    for (int i = 0; i < size; ++i) {
      if (stream.readBoolean()) {
        deserializeDefinition(stream, definitionMap);
      } else {
        parent.getResolvedName().toNamespace().addMember(definitionMap.get(stream.readInt()).getResolvedName().toNamespaceMember());
      }
    }
  }

  private void deserializeClassDefinition(DataInputStream stream, Map<Integer, Definition> definitionMap, ClassDefinition definition) throws IOException {
    deserializeNamespace(stream, definitionMap, definition);
    definition.setUniverse(readUniverse(stream));

    int numFields = stream.readInt();
    for (int i = 0; i < numFields; i++) {
      ClassField field = (ClassField) definitionMap.get(stream.readInt());
      definition.addField(field);
      field.hasErrors(stream.readBoolean());

      if (!field.hasErrors()) {
        field.setUniverse(readUniverse(stream));
        field.setBaseType(readExpression(stream, definitionMap));
        field.setThisClass(definition);
      }
    }
  }

  public static Universe readUniverse(DataInputStream stream) throws IOException {
    int level = stream.readInt();
    int truncated = stream.readInt();
    return new Universe.Type(level, truncated);
  }

  private TypedBinding readTypedBinding(DataInputStream stream, Map<Integer, Definition> definitionMap) throws IOException {
    String name = null;
    if (stream.readBoolean()) {
      name = stream.readUTF();
    }
    TypedBinding result = new TypedBinding(name, readExpression(stream, definitionMap));
    myBindingMap.add(result);
    return result;
  }

  private DependentLink readParameter(DataInputStream stream, Map<Integer, Definition> definitionMap, int code) throws IOException {
    DependentLink link;

    if (code == 1) {
      boolean isExplicit = stream.readBoolean();
      String name = stream.readUTF();
      Expression type = readExpression(stream, definitionMap);
      link = new TypedDependentLink(isExplicit, name, type, null);
    } else
    if (code == 2) {
      String name = stream.readUTF();
      link = new UntypedDependentLink(name, null);
    } else
    if (code == 3) {
      boolean isExplicit = stream.readBoolean();
      Expression type = readExpression(stream, definitionMap);
      link = new NonDependentLink(type, null);
      link.setExplicit(isExplicit);
    } else {
      throw new IncorrectFormat();
    }

    myBindingMap.add(link);
    return link;
  }

  public DependentLink readParameter1(DataInputStream stream, Map<Integer, Definition> definitionMap) throws IOException {
    int code = stream.read();
    if (code == 0) {
      return null;
    }
    return readParameter(stream, definitionMap, code);
  }

  public DependentLink readParameters(DataInputStream stream, Map<Integer, Definition> definitionMap) throws IOException {
    LinkList list = new LinkList();
    while (true) {
      int code = stream.read();
      if (code == 0) {
        return list.getFirst();
      }
      list.append(readParameter(stream, definitionMap, code));
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
        return definitionMap.get(stream.readInt()).getDefCall();
      }
      case 3: {
        Definition definition = definitionMap.get(stream.readInt());
        int size = stream.readInt();
        if (!(definition instanceof Constructor)) {
          throw new IncorrectFormat();
        }

        List<Expression> parameters = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
          parameters.add(readExpression(stream, definitionMap));
        }
        return ConCall((Constructor) definition, parameters);
      }
      case 4: {
        Definition definition = definitionMap.get(stream.readInt());
        if (!(definition instanceof ClassDefinition)) {
          throw new IncorrectFormat();
        }
        int size = stream.readInt();
        Map<ClassField, ClassCallExpression.ImplementStatement> statements = new HashMap<>();
        for (int i = 0; i < size; ++i) {
          Definition field = definitionMap.get(stream.readInt());
          if (!(field instanceof ClassField)) {
            throw new IncorrectFormat();
          }
          Expression type = stream.readBoolean() ? readExpression(stream, definitionMap) : null;
          Expression term = stream.readBoolean() ? readExpression(stream, definitionMap) : null;
          statements.put((ClassField) field, new ClassCallExpression.ImplementStatement(type, term));
        }
        return ClassCall((ClassDefinition) definition, statements);
      }
      case 5: {
        return Reference(myBindingMap.get(stream.readInt()));
      }
      case 6: {
        Expression body = readExpression(stream, definitionMap);
        return Lam(readParameters(stream, definitionMap), body);
      }
      case 7: {
        DependentLink parameters = readParameters(stream, definitionMap);
        return Pi(parameters, readExpression(stream, definitionMap));
      }
      case 8: {
        return new UniverseExpression(readUniverse(stream));
      }
      case 9: {
        return Error(stream.readBoolean() ? readExpression(stream, definitionMap) : null, new TypeCheckingError(myResolvedName, "Deserialization error", null));
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
        return Sigma(readParameters(stream, definitionMap));
      }
      case 13: {
        Expression expr = readExpression(stream, definitionMap);
        return Proj(expr, stream.readInt());
      }
      case 14: {
        return New(readExpression(stream, definitionMap));
      }
      case 15: {
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
    final DependentLink parameters = readParameters(stream, definitionMap);
    final Expression resultType = stream.readBoolean() ? readExpression(stream, definitionMap) : null;
    return let(name, parameters, resultType, readElimTree(stream, definitionMap));
  }

  public PatternArgument readPatternArg(DataInputStream stream, Map<Integer, Definition> definitionMap) throws IOException {
    boolean isExplicit = stream.readBoolean();
    boolean isHidden = stream.readBoolean();
    return new PatternArgument(readPattern(stream, definitionMap), isExplicit, isHidden);
  }

  public Pattern readPattern(DataInputStream stream, Map<Integer, Definition> definitionMap) throws IOException {
    switch (stream.readInt()) {
      case 0: {
        return new NamePattern(readParameter1(stream, definitionMap));
      }
      case 1: {
        return new AnyConstructorPattern(readParameter1(stream, definitionMap));
      }
      case 2: {
        Definition constructor = definitionMap.get(stream.readInt());
        if (!(constructor instanceof Constructor)) {
          throw new IncorrectFormat();
        }
        int size = stream.readInt();
        List<PatternArgument> arguments = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
          arguments.add(readPatternArg(stream, definitionMap));
        }
        return new ConstructorPattern((Constructor) constructor, new Patterns(arguments));
      }
      default: {
        throw new IllegalStateException();
      }
    }
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
}
