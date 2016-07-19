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
import com.jetbrains.jetpad.vclang.term.Preprelude;
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
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;
import com.jetbrains.jetpad.vclang.term.expr.sort.LevelMax;
import com.jetbrains.jetpad.vclang.term.expr.sort.Sort;
import com.jetbrains.jetpad.vclang.term.expr.sort.SortMax;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
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
        associativity = Abstract.Binding.Associativity.LEFT_ASSOC;
      } else if (assoc == 1) {
        associativity = Abstract.Binding.Associativity.RIGHT_ASSOC;
      } else {
        associativity = Abstract.Binding.Associativity.NON_ASSOC;
      }
      byte priority = stream.readByte();
      precedence = new Abstract.Definition.Precedence(associativity, priority);
    }

    if (dryRun)
      return null;

    Definition definition = null; // code.toDefinition(rn, precedence);  // FIXME[serial]
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
        int is_prelude = stream.readInt();
        ModuleID depModuleID = is_prelude == 2 ? moduleID.deserialize(stream) : is_prelude == 1 ? Preprelude.moduleID : Prelude.moduleID;
        rn = fullPathToResolvedName(readFullPath(stream), depModuleID);
      }
      if (!createStubs) {
        result.put(i, rn.getName().equals("\\parent") ?
            ((ClassDefinition) rn.getParent().toDefinition()).getParentField() : rn.toDefinition());
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
    ClassDefinition moduleRoot = (ClassDefinition) definitionMap.get(0);
    deserializeDefinition(stream, definitionMap);
    return new ModuleLoader.Result(null, moduleRoot, false, errorsNumber);
  }

  private Definition deserializeDefinition(DataInputStream stream, Map<Integer, Definition> definitionMap) throws IOException {
    Definition definition = definitionMap.get(stream.readInt());
    if (stream.readBoolean())
      definition.setThisClass((ClassDefinition) definitionMap.get(stream.readInt()));
    definition.hasErrors(stream.readBoolean());

    List<Binding> polyParams = new ArrayList<>(stream.readInt());

    for (int i = 0; i < polyParams.size(); ++i) {
      polyParams.set(i, readBinding(stream, definitionMap));
    }

    definition.setPolyParams(polyParams);

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
      definition.setSorts(readSortMax(stream, definitionMap));
      definition.setParameters(readParameters(stream, definitionMap));
    }

    int constructorsNumber = stream.readInt();
    for (int i = 0; i < constructorsNumber; i++) {
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
        constructor.setParameters(readParameters(stream, definitionMap));
      }

      // FIXME[serial]
//      definition.addConstructor(constructor);
//      definition.getParentNamespace().addDefinition(constructor);
    }

    int conditionsNumber = stream.readInt();
    for (int i = 0; i < conditionsNumber; i++) {
      Definition constructor = definitionMap.get(stream.readInt());
      if (!(constructor instanceof Constructor && ((Constructor) constructor).getDataType() == definition)) {
        throw new IncorrectFormat();
      }
      definition.addCondition(new Condition((Constructor) constructor, myElimTreeDeserialization.readElimTree(stream, definitionMap)));
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
        // FIXME[serial]
        //parent.getResolvedName().toNamespace().addMember(definitionMap.get(stream.readInt()).getResolvedName().toNamespaceMember());
      }
    }
  }

  private void deserializeClassDefinition(DataInputStream stream, Map<Integer, Definition> definitionMap, ClassDefinition definition) throws IOException {
    deserializeNamespace(stream, definitionMap, definition);
    definition.setSorts(readSortMax(stream, definitionMap));

    int numFields = stream.readInt();
    for (int i = 0; i < numFields; i++) {
      String name = stream.readUTF();
      DependentLink thisParameter = null;
      Expression implementation = null;
      if (stream.readBoolean()) {
        thisParameter = readParameters(stream, definitionMap);
        implementation = readExpression(stream, definitionMap);
      }
      ClassField field = (ClassField) definitionMap.get(stream.readInt());
      definition.addField(field, name, thisParameter, implementation);
      field.setThisParameter(readParameters(stream, definitionMap));
      field.hasErrors(stream.readBoolean());

      if (!field.hasErrors()) {
        field.setSorts(readSortMax(stream, definitionMap));
        field.setBaseType(readExpression(stream, definitionMap));
        field.setThisClass(definition);
      }
    }
  }

  public Level readLevel(DataInputStream stream, Map<Integer, Definition> definitionMap) throws IOException {
    Binding var = stream.readBoolean() ? readBinding(stream, definitionMap) : null;
    return new Level(var, stream.readInt());
  }

  public LevelMax readLevelMax(DataInputStream stream, Map<Integer, Definition> definitionMap) throws IOException {
    LevelMax result = new LevelMax();
    int numLevels = stream.readInt();
    for (int i = 0; i < numLevels; i++) {
      result.add(readLevel(stream, definitionMap));
    }
    return result;
  }

  public Sort readSort(DataInputStream stream, Map<Integer, Definition> definitionMap) throws IOException {
    Level pLevel = readLevel(stream, definitionMap);
    Level hLevel = readLevel(stream, definitionMap);
    return new Sort(pLevel, hLevel);
  }

  public SortMax readSortMax(DataInputStream stream, Map<Integer, Definition> definitionMap) throws IOException {
    LevelMax pLevel = readLevelMax(stream, definitionMap);
    LevelMax hLevel = readLevelMax(stream, definitionMap);
    return new SortMax(pLevel, hLevel);
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

  public LevelSubstitution readSubstitution(DataInputStream stream, Map<Integer, Definition> definitionMap) throws IOException {
    int num_vars = stream.readInt();
    LevelSubstitution subst = new LevelSubstitution();

    for (int i = 0; i < num_vars; ++i) {
      Binding var = readBinding(stream, definitionMap);
      subst.add(var, readLevel(stream, definitionMap));
    }

    return subst;
  }

  public Expression readExpression(DataInputStream stream, Map<Integer, Definition> definitionMap) throws IOException {
    int code = stream.read();
    switch (code) {
      case 1: {
        Expression function = readExpression(stream, definitionMap);
        int size = stream.readInt();
        List<Expression> arguments = new ArrayList<>(size);
        List<EnumSet<AppExpression.Flag>> flags = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
          arguments.add(readExpression(stream, definitionMap));
          EnumSet<AppExpression.Flag> flag = EnumSet.noneOf(AppExpression.Flag.class);
          if (stream.readBoolean()) {
            flag.add(AppExpression.Flag.EXPLICIT);
          }
          if (stream.readBoolean()) {
            flag.add(AppExpression.Flag.VISIBLE);
          }
          flags.add(flag);
        }
        return new AppExpression(function, arguments, flags);
      }
      case 2: {
        Definition definition = definitionMap.get(stream.readInt());
        LevelSubstitution polySubst = readSubstitution(stream, definitionMap);
        return definition.getDefCall(polySubst);
      }
      case 3: {
        Definition definition = definitionMap.get(stream.readInt());
        LevelSubstitution polySubst = readSubstitution(stream, definitionMap);
        int size = stream.readInt();
        if (!(definition instanceof Constructor)) {
          throw new IncorrectFormat();
        }

        List<Expression> parameters = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
          parameters.add(readExpression(stream, definitionMap));
        }
        return ConCall((Constructor) definition, parameters).applyLevelSubst(polySubst);
      }
      case 4: {
        Definition definition = definitionMap.get(stream.readInt());
        LevelSubstitution polySubst = readSubstitution(stream, definitionMap);
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
        return ClassCall((ClassDefinition) definition, statements).applyLevelSubst(polySubst);
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
        return new UniverseExpression(readSort(stream, definitionMap));
      }
      case 9: {
        return Error(stream.readBoolean() ? readExpression(stream, definitionMap) : null, new TypeCheckingError(myModuleID + " deserialization error"));  // FIXME[error] bad error
      }
      case 10: {
        int size = stream.readInt();
        List<Expression> fields = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
          fields.add(readExpression(stream, definitionMap));
        }
        SigmaExpression sigma = readExpression(stream, definitionMap).toSigma();
        if (sigma == null) {
          throw new IncorrectFormat();
        }
        return Tuple(fields, sigma);
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
      case 16: {
        Expression expr = readExpression(stream, definitionMap);
        Expression type = readExpression(stream, definitionMap);
        return new OfTypeExpression(expr, type);
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
