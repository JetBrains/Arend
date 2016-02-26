package com.jetbrains.jetpad.vclang.serialization;

import com.jetbrains.jetpad.vclang.module.ModuleID;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.module.Root;
import com.jetbrains.jetpad.vclang.module.SerializableModuleID;
import com.jetbrains.jetpad.vclang.module.output.Output;
import com.jetbrains.jetpad.vclang.naming.ModuleResolvedName;
import com.jetbrains.jetpad.vclang.naming.Namespace;
import com.jetbrains.jetpad.vclang.naming.NamespaceMember;
import com.jetbrains.jetpad.vclang.naming.ResolvedName;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.context.LinkList;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.term.context.binding.UnknownInferenceBinding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.TypedDependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.UntypedDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.pattern.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeDeserialization;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;

import java.io.*;
import java.util.*;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Error;

public class ModuleDeserialization {
  private SerializableModuleID myModuleID;
  private List<Binding> myBindingMap;
  private final ElimTreeDeserialization myElimTreeDeserialization;

  public ModuleDeserialization() {
    myElimTreeDeserialization = new ElimTreeDeserialization(this);
  }

  public ModuleLoader.Result readFile(File file, SerializableModuleID module) throws IOException {
    return readStream(new DataInputStream(new BufferedInputStream(new FileInputStream(file))), module);
  }

  public static void readStubsFromFile(File file, SerializableModuleID module) throws IOException {
    readStubsFromStream(new DataInputStream(new BufferedInputStream(new FileInputStream(file))), module);
  }

  public static void readStubsFromStream(DataInputStream stream, SerializableModuleID moduleID) throws IOException {
    verifySignature(stream);
    readHeader(stream, moduleID);
    stream.readInt();
    Root.addModule(moduleID, new NamespaceMember(new Namespace(moduleID), null, null));
    readDefIndices(stream, true, moduleID);
  }

  public static Output.Header readHeaderFromFile(File file, SerializableModuleID moduleID) throws IOException {
    return readHeaderFromStream(new DataInputStream(new BufferedInputStream(new FileInputStream(file))), moduleID);
  }

  public static Output.Header readHeaderFromStream(DataInputStream stream, SerializableModuleID moduleID) throws IOException {
    verifySignature(stream);
    return readHeader(stream, moduleID);
  }

  private static List<String> readFullPath(DataInputStream stream) throws IOException {
    List<String> result = new ArrayList<>();
    int size = stream.readInt();
    for (int i = 0; i < size; i++) {
      result.add(stream.readUTF());
    }

    return result;
  }

  private static Output.Header readHeader(DataInputStream stream, SerializableModuleID moduleID) throws IOException {
    Output.Header result = new Output.Header(new ArrayList<ModuleID>());
    int size = stream.readInt();
    for (int i = 0; i < size; i++) {
      result.dependencies.add(moduleID.deserialize(stream));
    }

    return result;
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

    if (dryRun)
      return null;

    Definition definition = code.toDefinition(rn, precedence);
    if (rn.getName().equals("\\parent"))
      ((ClassDefinition) rn.getParent().toDefinition()).addField((ClassField) definition);
    else {
      rn.toNamespaceMember().definition = definition;
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

  private static ResolvedName fullPathToResolvedName(List<String> path, ModuleID moduleID) {
    ResolvedName result = new ModuleResolvedName(moduleID);
    for (String aPath : path) {
      result = result.toNamespace().getChild(aPath).getResolvedName();
    }
    return result;
  }

  private static Map<Integer, Definition> readDefIndices(DataInputStream stream, boolean createStubs, SerializableModuleID moduleID) throws IOException {
    Map<Integer, Definition> result = new HashMap<>();

    int size = stream.readInt();
    for (int i = 0; i < size; i++) {
      ResolvedName rn;
      if (stream.readBoolean()) {
        rn = fullPathToResolvedName(readFullPath(stream), moduleID);
        readDefinition(stream, rn, !createStubs);
      } else {
        ModuleID depModuleID = stream.readBoolean() ? moduleID.deserialize(stream) : Prelude.moduleID;
        rn = fullPathToResolvedName(readFullPath(stream), depModuleID);
      }
      if (!createStubs) {
        result.put(i, rn.getName().equals("\\parent") ?
            ((ClassDefinition) rn.getParent().toDefinition()).getField("\\parent") : rn.toDefinition());
      }
    }

    return createStubs ? null : result;
  }

  public ModuleLoader.Result readStream(DataInputStream stream, SerializableModuleID moduleID) throws IOException {
    myModuleID = moduleID;
    myBindingMap = new ArrayList<>();
    verifySignature(stream);
    readHeader(stream, moduleID);
    int errorsNumber = stream.readInt();
    Map<Integer, Definition> definitionMap = readDefIndices(stream, false, moduleID);
    Definition moduleRoot = definitionMap.get(0);
    deserializeDefinition(stream, definitionMap);
    return new ModuleLoader.Result(new NamespaceMember(moduleRoot.getResolvedName().toNamespace(), null, moduleRoot), false, errorsNumber);
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
          DependentLink link = readParameters(stream, definitionMap);
          int numPatterns = stream.readInt();
          List<PatternArgument> patterns = new ArrayList<>(numPatterns);
          for (int j = 0; j < numPatterns; j++) {
            LinkedDeserializationResult<PatternArgument> result = readPatternArg(stream, definitionMap, link);
            patterns.add(result.pattern);
            link = result.link;
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
      if (stream.readBoolean()) {
        definition.setElimTree(myElimTreeDeserialization.readElimTree(stream, definitionMap));
      }
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
      field.setThisParameter(readParameters(stream, definitionMap));
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

  public TypedBinding readTypedBinding(DataInputStream stream, Map<Integer, Definition> definitionMap) throws IOException {
    String name = null;
    if (stream.readBoolean()) {
      name = stream.readUTF();
    }
    TypedBinding result = new TypedBinding(name, readExpression(stream, definitionMap));
    myBindingMap.add(result);
    return result;
  }

  private String readString(DataInputStream stream) throws IOException {
    String str = stream.readUTF();
    return str.isEmpty() ? null : str;
  }

  public Binding readBinding(DataInputStream stream, Map<Integer, Definition> definitionMap) throws IOException {
    int index = stream.readInt();
    if (index == -1) {
      String name = stream.readUTF();
      Expression type = readExpression(stream, definitionMap);
      return new UnknownInferenceBinding(name, type);
    } else {
      if (index >= myBindingMap.size()) {
        throw new IncorrectFormat();
      }
      return myBindingMap.get(index);
    }
  }

  public DependentLink readParameters(DataInputStream stream, Map<Integer, Definition> definitionMap) throws IOException {
    LinkList result = new LinkList();
    for (int code = stream.read(); code != 0; code = stream.read()) {
      List<String> untypedNames = new ArrayList<>();
      int untypedBindingIdx = myBindingMap.size();
      while (code == 2) {
        untypedNames.add(stream.readUTF());
        code = stream.read();
        myBindingMap.add(null);
      }

      DependentLink link;
      switch (code) {
        case 1: {
          boolean isExplicit = stream.readBoolean();
          String name = readString(stream);
          Expression type = readExpression(stream, definitionMap);
          link = new TypedDependentLink(isExplicit, name, type, EmptyDependentLink.getInstance());
        }
        break;
        default:
          throw new IncorrectFormat();
      }

      for (int i = untypedNames.size() - 1; i >= 0; i--) {
        link = new UntypedDependentLink(untypedNames.get(i), link);
      }
      for (DependentLink link1 = link; link1.hasNext(); link1 = link1.getNext()) {
        if (link1 instanceof UntypedDependentLink) {
          myBindingMap.set(untypedBindingIdx++, link1);
        } else {
          myBindingMap.add(link1);
        }
      }

      result.append(link);
    }
    return result.getFirst();
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
        return Reference(readBinding(stream, definitionMap));
      }
      case 6: {
        return Lam(readParameters(stream, definitionMap), readExpression(stream, definitionMap));
      }
      case 7: {
        DependentLink parameters = readParameters(stream, definitionMap);
        return Pi(parameters, readExpression(stream, definitionMap));
      }
      case 8: {
        return new UniverseExpression(readUniverse(stream));
      }
      case 9: {
        return Error(stream.readBoolean() ? readExpression(stream, definitionMap) : null, new TypeCheckingError(new ModuleResolvedName(myModuleID), "Deserialization error", null));
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
    LetClause result = let(name, parameters, resultType, myElimTreeDeserialization.readElimTree(stream, definitionMap));
    myBindingMap.add(result);
    return result;
  }

  public LinkedDeserializationResult<PatternArgument> readPatternArg(DataInputStream stream, Map<Integer, Definition> definitionMap, DependentLink link) throws IOException {
    boolean isExplicit = stream.readBoolean();
    boolean isHidden = stream.readBoolean();
    LinkedDeserializationResult<Pattern> result = readPattern(stream, definitionMap, link);
    return new LinkedDeserializationResult<>(new PatternArgument(result.pattern, isExplicit, isHidden), result.link);
  }

  private static class LinkedDeserializationResult<T> {
    private final T pattern;
    private final DependentLink link;

    private LinkedDeserializationResult(T pattern, DependentLink link) {
      this.pattern = pattern;
      this.link = link;
    }
  }

  private LinkedDeserializationResult<Pattern> readPattern(DataInputStream stream, Map<Integer, Definition> definitionMap, DependentLink link) throws IOException {
    switch (stream.readInt()) {
      case 0: {
        return new LinkedDeserializationResult<Pattern>(new NamePattern(link), link.getNext());
      }
      case 1: {
        return new LinkedDeserializationResult<Pattern>(new AnyConstructorPattern(link), link.getNext());
      }
      case 2: {
        Definition constructor = definitionMap.get(stream.readInt());
        if (!(constructor instanceof Constructor)) {
          throw new IncorrectFormat();
        }
        int size = stream.readInt();
        List<PatternArgument> arguments = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
          LinkedDeserializationResult<PatternArgument> result = readPatternArg(stream, definitionMap, link);
          arguments.add(result.pattern);
          link = result.link;
        }
        return new LinkedDeserializationResult<Pattern>(new ConstructorPattern((Constructor) constructor, new Patterns(arguments)), link);
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
