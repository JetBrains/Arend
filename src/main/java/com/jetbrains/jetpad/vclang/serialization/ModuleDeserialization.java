package com.jetbrains.jetpad.vclang.serialization;

import com.jetbrains.jetpad.vclang.module.ModuleLoadingResult;
import com.jetbrains.jetpad.vclang.module.RootModule;
import com.jetbrains.jetpad.vclang.module.output.Output;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.term.context.param.*;
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
  private ResolvedName myResolvedName;
  private final ElimTreeDeserialization myElimTreeDeserialization;
  private final List<Binding> myBindingMap = new ArrayList<>();

  public ModuleDeserialization() {
    myElimTreeDeserialization = new ElimTreeDeserialization(this);
  }

  public Binding getBinding(int bindingIdx) {
    return myBindingMap.get(bindingIdx);
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

    readDefIndices(stream, true, module);
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

  private static Map<Integer, Definition> readDefIndices(DataInputStream stream, boolean createStubs, ResolvedName module) throws IOException {
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
    Map<Integer, Definition> definitionMap = readDefIndices(stream, false, module);
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

  public DependentLink readParameters(DataInputStream stream, Map<Integer, Definition> definitionMap) throws IOException {
    int code = stream.read();
    if (code == 0) {
      return EmptyDependentLink.getInstance();
    }

    List<String> untypedNames = new ArrayList<>();
    while (code == 2) {
      untypedNames.add(stream.readUTF());
      code = stream.read();
    }

    DependentLink link;
    switch (code) {
      case 0: {
        return EmptyDependentLink.getInstance();
      }
      case 1: {
        boolean isExplicit = stream.readBoolean();
        String name = readString(stream);
        Expression type = readExpression(stream, definitionMap);
        link = new TypedDependentLink(isExplicit, name, type, EmptyDependentLink.getInstance());
      } break;
      case 3: {
         boolean isExplicit = stream.readBoolean();
         Expression type = readExpression(stream, definitionMap);
         link = new NonDependentLink(type, EmptyDependentLink.getInstance());
         link.setExplicit(isExplicit);
       } break;
       default:
         throw new IncorrectFormat();
    }

    for (int i = untypedNames.size() - 1; i >= 0; i--) {
      link = new UntypedDependentLink(untypedNames.get(i), link);
    }
    for (DependentLink link1 = link; link1.hasNext(); link1 = link1.getNext()) {
      myBindingMap.add(link1);
    }

    link.setNext(readParameters(stream, definitionMap));
    return link;
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
    return let(name, parameters, resultType, myElimTreeDeserialization.readElimTree(stream, definitionMap));
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
