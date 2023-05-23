package org.arend.module.serialization;

import com.google.protobuf.ByteString;
import org.arend.core.context.LinkList;
import org.arend.core.context.binding.Binding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.*;
import org.arend.core.elimtree.*;
import org.arend.core.expr.*;
import org.arend.core.pattern.*;
import org.arend.core.subst.Levels;
import org.arend.ext.core.definition.CoreDefinition;
import org.arend.ext.core.definition.CoreFunctionDefinition;
import org.arend.ext.serialization.ArendDeserializer;
import org.arend.ext.serialization.DeserializationException;
import org.arend.ext.serialization.SerializableKey;
import org.arend.ext.typechecking.DefinitionListener;
import org.arend.extImpl.SerializableKeyRegistryImpl;
import org.arend.naming.reference.*;
import org.arend.prelude.Prelude;
import org.arend.typechecking.order.dependency.DependencyListener;
import org.arend.ext.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class DefinitionDeserialization implements ArendDeserializer {
  private final CallTargetProvider myCallTargetProvider;
  private final DependencyListener myDependencyListener;
  private final SerializableKeyRegistryImpl myKeyRegistry;
  private final DefinitionListener myDefinitionListener;

  public DefinitionDeserialization(CallTargetProvider callTargetProvider, DependencyListener dependencyListener, SerializableKeyRegistryImpl keyRegistry, DefinitionListener definitionListener) {
    myCallTargetProvider = callTargetProvider;
    myDependencyListener = dependencyListener;
    myKeyRegistry = keyRegistry;
    myDefinitionListener = definitionListener;
  }

  public void fillInDefinition(DefinitionProtos.Definition defProto, Definition def) throws DeserializationException {
    final ExpressionDeserialization defDeserializer = new ExpressionDeserialization(myCallTargetProvider, myDependencyListener, def);

    switch (defProto.getDefinitionDataCase()) {
      case CLASS -> fillInClassDefinition(defDeserializer, defProto.getClass_(), (ClassDefinition) def);
      case DATA -> fillInDataDefinition(defDeserializer, defProto.getData(), (DataDefinition) def);
      case CONSTRUCTOR -> fillInDConstructor(defDeserializer, defProto.getConstructor(), (DConstructor) def);
      case FUNCTION -> fillInFunctionDefinition(defDeserializer, defProto.getFunction(), (FunctionDefinition) def);
      case META -> fillInMetaDefinition(defDeserializer, defProto.getMeta(), (MetaTopDefinition) def);
      default -> throw new DeserializationException("Unknown Definition kind: " + defProto.getDefinitionDataCase());
    }

    def.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
    List<Pair<TCDefReferable, Integer>> parametersOriginalDefinitions = readParametersOriginalDefinitions(defProto.getParameterOriginalDefList());
    for (Pair<TCDefReferable, Integer> pair : parametersOriginalDefinitions) {
      myDependencyListener.dependsOn(def.getRef(), pair.proj1);
    }

    if (def instanceof TopLevelDefinition topDef) {
      topDef.setUniverseKind(defDeserializer.readUniverseKind(defProto.getUniverseKind()));
      int pLevelsParent = defProto.getPLevelsParent();
      if (pLevelsParent != 0) {
        topDef.setPLevelsParent(myCallTargetProvider.getRef(pLevelsParent - 1));
      }
      int hLevelsParent = defProto.getHLevelsParent();
      if (hLevelsParent != 0) {
        topDef.setHLevelsParent(myCallTargetProvider.getRef(hLevelsParent - 1));
      }
      topDef.setPLevelsDerived(defProto.getPLevelsDerived());
      topDef.setHLevelsDerived(defProto.getHLevelsDerived());
      topDef.setAxioms(readDefinitions(defProto.getAxiomList(), FunctionDefinition.class));

      topDef.setParametersOriginalDefinitions(parametersOriginalDefinitions);
    }

    for (Integer index : defProto.getMetaRefList()) {
      myDependencyListener.dependsOn(def.getRef(), myCallTargetProvider.getMetaCallTarget(index));
    }

    loadKeys(defProto.getUserDataMap(), def);

    if (myDefinitionListener != null) {
      myDefinitionListener.loaded(def);
    }
  }

  private void loadKeys(Map<String, ByteString> proto, Definition def) throws DeserializationException {
    if (myKeyRegistry != null) {
      for (Map.Entry<String, ByteString> entry : proto.entrySet()) {
        //noinspection unchecked
        SerializableKey<Object> key = (SerializableKey<Object>) myKeyRegistry.getKey(entry.getKey());
        if (key == null) {
          throw new DeserializationException("Key '" + entry.getKey() + "' is not registered");
        }
        def.putUserData(key, key.deserialize(this, entry.getValue().toByteArray()));
      }
    }
  }

  private PiExpression checkFieldType(PiExpression expr, ClassDefinition classDef) throws DeserializationException {
    if (!expr.getParameters().getNext().hasNext()) {
      Expression type = expr.getParameters().getTypeExpr();
      if (type instanceof ClassCallExpression && ((ClassCallExpression) type).getDefinition().equals(classDef)) {
        return expr;
      }
    }
    throw new DeserializationException("Incorrect class field type");
  }

  private void fillInClassDefinition(ExpressionDeserialization defDeserializer, DefinitionProtos.Definition.ClassData classProto, ClassDefinition classDef) throws DeserializationException {
    classDef.setBaseUniverseKind(defDeserializer.readUniverseKind(classProto.getBaseUniverseKind()));

    Map<Integer, LevelProtos.Levels> superLevelsProto = classProto.getSuperLevelsMap();
    if (!superLevelsProto.isEmpty()) {
      Map<ClassDefinition, Levels> superLevels = new HashMap<>();
      for (Map.Entry<Integer, LevelProtos.Levels> entry : superLevelsProto.entrySet()) {
        ClassDefinition superClass = myCallTargetProvider.getCallTarget(entry.getKey(), ClassDefinition.class);
        superLevels.put(superClass, defDeserializer.readLevels(entry.getValue()));
      }
      classDef.setSuperLevels(superLevels);
    }

    for (DefinitionProtos.Definition.ClassData.Field fieldProto : classProto.getPersonalFieldList()) {
      ClassField field = myCallTargetProvider.getCallTarget(fieldProto.getReferable().getIndex(), ClassField.class);
      if (!fieldProto.hasType()) {
        throw new DeserializationException("Missing class field type");
      }
      PiExpression fieldType = checkFieldType(defDeserializer.readPi(fieldProto.getType()), classDef);
      if (fieldProto.getIsProperty()) {
        field.setIsProperty();
      }
      field.setType(fieldType);
      if (fieldProto.hasTypeLevel()) {
        field.setTypeLevel(defDeserializer.readExpr(fieldProto.getTypeLevel()), fieldProto.getResultTypeLevel());
      }
      field.setNumberOfParameters(fieldProto.getNumberOfParameters());
      // setTypeClassReference(field.getReferable(), EmptyDependentLink.getInstance(), fieldType.getCodomain());
      field.setHideable(fieldProto.getIsHideable());
      field.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
      field.setUniverseKind(defDeserializer.readUniverseKind(fieldProto.getUniverseKind()));
      loadKeys(fieldProto.getUserDataMap(), field);
    }

    for (int classFieldRef : classProto.getFieldRefList()) {
      classDef.addField(myCallTargetProvider.getCallTarget(classFieldRef, ClassField.class));
    }
    for (int classFieldRef : classProto.getFieldRefList()) {
      classDef.addField(myCallTargetProvider.getCallTarget(classFieldRef, ClassField.class));
    }
    for (Map.Entry<Integer, ExpressionProtos.Expression.Abs> entry : classProto.getImplementationsMap().entrySet()) {
      classDef.implementField(myCallTargetProvider.getCallTarget(entry.getKey(), ClassField.class), defDeserializer.readAbsExpr(entry.getValue()));
    }
    for (Map.Entry<Integer, DefinitionProtos.Definition.DefaultData> entry : classProto.getDefaultsMap().entrySet()) {
      classDef.addDefault(myCallTargetProvider.getCallTarget(entry.getKey(), ClassField.class), defDeserializer.readAbsExpr(entry.getValue().getExpr()), entry.getValue().getIsFunc());
    }
    for (Map.Entry<Integer, DefinitionProtos.Definition.RefList> entry : classProto.getDefaultDependenciesMap().entrySet()) {
      classDef.addDefaultDependencies(myCallTargetProvider.getCallTarget(entry.getKey(), ClassField.class), readDefinitions(entry.getValue().getRefList(), ClassField.class));
    }
    for (Map.Entry<Integer, DefinitionProtos.Definition.RefList> entry : classProto.getDefaultImplDependenciesMap().entrySet()) {
      classDef.addDefaultImplDependencies(myCallTargetProvider.getCallTarget(entry.getKey(), ClassField.class), readDefinitions(entry.getValue().getRefList(), ClassField.class));
    }
    for (Map.Entry<Integer, ExpressionProtos.Expression.Pi> entry : classProto.getOverriddenFieldMap().entrySet()) {
      classDef.overrideField(myCallTargetProvider.getCallTarget(entry.getKey(), ClassField.class), checkFieldType(defDeserializer.readPi(entry.getValue()), classDef));
    }
    for (Integer fieldRef : classProto.getCovariantFieldList()) {
      classDef.addCovariantField(myCallTargetProvider.getCallTarget(fieldRef, ClassField.class));
    }
    for (Integer fieldRef : classProto.getOmegaFieldList()) {
      classDef.addOmegaField(myCallTargetProvider.getCallTarget(fieldRef, ClassField.class));
    }
    classDef.setSort(defDeserializer.readSort(classProto.getSort()));

    for (int superClassRef : classProto.getSuperClassRefList()) {
      ClassDefinition superClass = myCallTargetProvider.getCallTarget(superClassRef, ClassDefinition.class);
      classDef.addSuperClass(superClass);
      myDependencyListener.dependsOn(classDef.getReferable(), superClass.getReferable());

      for (Map.Entry<ClassField, AbsExpression> entry : superClass.getImplemented()) {
        classDef.implementField(entry.getKey(), entry.getValue());
      }
    }

    if (classDef.getReferable() instanceof ClassReferableImpl classRef) {
      for (ClassDefinition superClass : classDef.getSuperClasses()) {
        Referable superRef = superClass.getReferable().getUnderlyingReferable();
        if (superRef instanceof ClassReferable) {
          classRef.getSuperClassReferences().add((ClassReferable) superRef);
        }
        if (!classDef.getSuperLevels().isEmpty()) {
          classRef.addSuperLevels(classDef.getSuperLevels().get(superClass) != null);
        }
      }
    }

    if (classProto.getCoercingFieldRef() != -1) {
      classDef.setClassifyingField(myCallTargetProvider.getCallTarget(classProto.getCoercingFieldRef(), ClassField.class));
    }
    if (classProto.getIsRecord()) {
      classDef.setRecord();
    }

    int squasher = classProto.getSquasher();
    if (squasher != 0) {
      classDef.setSquasher(myCallTargetProvider.getCallTarget(squasher, FunctionDefinition.class));
    }

    readCoerceData(classProto.getCoerceData(), classDef.getCoerceData());

    for (DefinitionProtos.Definition.ClassParametersLevel classParametersLevelProto : classProto.getParametersLevelList()) {
      List<ClassField> fields = new ArrayList<>();
      for (Integer fieldRef : classParametersLevelProto.getFieldList()) {
        fields.add(myCallTargetProvider.getCallTarget(fieldRef, ClassField.class));
      }
      DefinitionProtos.Definition.ParametersLevel parametersLevelProto = classParametersLevelProto.getParametersLevel();
      List<Pair<ClassDefinition, Set<ClassField>>> strictList;
      if (classParametersLevelProto.getIsStrict()) {
        strictList = new ArrayList<>();
        for (DefinitionProtos.Definition.ClassParametersLevel.ClassExtSig sig : classParametersLevelProto.getClassExtSigList()) {
          if (sig.getClassDef() == 0) {
            strictList.add(null);
          } else {
            Set<ClassField> sigFields = new HashSet<>();
            for (Integer fieldRef : sig.getFieldList()) {
              sigFields.add(myCallTargetProvider.getCallTarget(fieldRef, ClassField.class));
            }
            strictList.add(new Pair<>(myCallTargetProvider.getCallTarget(sig.getClassDef(), ClassDefinition.class), sigFields));
          }
        }
      } else {
        strictList = null;
      }
      classDef.addParametersLevel(new ClassDefinition.ParametersLevel(parametersLevelProto.getHasParameters() ? defDeserializer.readParameters(parametersLevelProto.getParameterList()) : null, parametersLevelProto.getLevel(), fields, strictList));
    }

    List<Integer> goodFieldIndices = classProto.getGoodFieldList();
    if (!goodFieldIndices.isEmpty()) {
      Set<ClassField> goodFields = new HashSet<>();
      for (Integer goodFieldIndex : goodFieldIndices) {
        goodFields.add(myCallTargetProvider.getCallTarget(goodFieldIndex, ClassField.class));
      }
      classDef.setGoodThisFields(goodFields);
    }

    List<Integer> typeClassFieldIndices = classProto.getTypeClassFieldList();
    if (!typeClassFieldIndices.isEmpty()) {
      Set<ClassField> typeClassFields = new HashSet<>();
      for (Integer typeClassFieldIndex : typeClassFieldIndices) {
        typeClassFields.add(myCallTargetProvider.getCallTarget(typeClassFieldIndex, ClassField.class));
      }
      classDef.setTypeClassFields(typeClassFields);
    }
  }

  private <T extends Definition> Set<T> readDefinitions(List<Integer> protos, Class<T> clazz) throws DeserializationException {
    Set<T> result = new HashSet<>();
    for (Integer index : protos) {
      result.add(getDefFromIndex(index, clazz));
    }
    return result;
  }

  private List<Definition.TypeClassParameterKind> readTypeClassParametersKind(List<DefinitionProtos.Definition.TypeClassParameterKind> kinds) {
    List<Definition.TypeClassParameterKind> result = new ArrayList<>(kinds.size());
    for (DefinitionProtos.Definition.TypeClassParameterKind kind : kinds) {
      switch (kind) {
        case YES -> result.add(Definition.TypeClassParameterKind.YES);
        case ONLY_LOCAL -> result.add(Definition.TypeClassParameterKind.ONLY_LOCAL);
        default -> result.add(Definition.TypeClassParameterKind.NO);
      }
    }
    return result;
  }

  private List<Pair<TCDefReferable,Integer>> readParametersOriginalDefinitions(List<DefinitionProtos.Definition.ParameterOriginalDef> protos) throws DeserializationException {
    if (protos.isEmpty()) return Collections.emptyList();
    List<Pair<TCDefReferable,Integer>> result = new ArrayList<>();
    for (DefinitionProtos.Definition.ParameterOriginalDef proto : protos) {
      result.add(new Pair<>((TCDefReferable) myCallTargetProvider.getRef(proto.getDefinition()), proto.getIndex()));
    }
    return result;
  }

  private void fillInDataDefinition(ExpressionDeserialization defDeserializer, DefinitionProtos.Definition.DataData dataProto, DataDefinition dataDef) throws DeserializationException {
    dataDef.setOmegaParameters(dataProto.getOmegaParameterList());
    if (dataProto.getHasEnclosingClass()) {
      dataDef.setHasEnclosingClass(true);
    }
    dataDef.setParameters(defDeserializer.readParameters(dataProto.getParamList()));
    List<Integer> parametersTypecheckingOrder = dataProto.getParametersTypecheckingOrderList();
    if (!parametersTypecheckingOrder.isEmpty()) {
      dataDef.setParametersTypecheckingOrder(parametersTypecheckingOrder);
    }
    List<Boolean> goodThisParameters = dataProto.getGoodThisParametersList();
    if (!goodThisParameters.isEmpty()) {
      dataDef.setGoodThisParameters(goodThisParameters);
    }
    dataDef.setTypeClassParameters(readTypeClassParametersKind(dataProto.getTypeClassParametersList()));
    for (DefinitionProtos.Definition.ParametersLevel levelParametersProto : dataProto.getParametersLevelsList()) {
      dataDef.addParametersLevel(readParametersLevel(defDeserializer, levelParametersProto));
    }
    List<Integer> recursiveDefIndices = dataProto.getRecursiveDefinitionList();
    if (!recursiveDefIndices.isEmpty()) {
      Set<TopLevelDefinition> recursiveDefs = new HashSet<>();
      for (Integer index : recursiveDefIndices) {
        recursiveDefs.add(myCallTargetProvider.getCallTarget(index, TopLevelDefinition.class));
      }
      dataDef.setRecursiveDefinitions(recursiveDefs);
    }
    dataDef.setSort(defDeserializer.readSort(dataProto.getSort()));

    for (DefinitionProtos.Definition.DataData.Constructor constructorProto : dataProto.getConstructorList()) {
      Constructor constructor = myCallTargetProvider.getCallTarget(constructorProto.getReferable().getIndex(), Constructor.class);
      if (constructorProto.getPatternCount() > 0) {
        constructor.setPatterns(defDeserializer.readExpressionPatterns(constructorProto.getPatternList(), new LinkList()));
      }
      constructor.setParameters(defDeserializer.readParameters(constructorProto.getParamList()));
      List<Integer> constructorParametersTypecheckingOrder = constructorProto.getParametersTypecheckingOrderList();
      if (!constructorParametersTypecheckingOrder.isEmpty()) {
        constructor.setParametersTypecheckingOrder(constructorParametersTypecheckingOrder);
      }
      List<Boolean> strictParameters = constructorProto.getStrictParametersList();
      if (!strictParameters.isEmpty()) {
        constructor.setStrictParameters(strictParameters);
      }
      List<Boolean> cGoodThisParameters = constructorProto.getGoodThisParametersList();
      if (!cGoodThisParameters.isEmpty()) {
        constructor.setGoodThisParameters(cGoodThisParameters);
      }
      constructor.setTypeClassParameters(readTypeClassParametersKind(constructorProto.getTypeClassParametersList()));
      if (constructorProto.hasConditions()) {
        constructor.setBody(readBody(defDeserializer, constructorProto.getConditions(), DependentLink.Helper.size(constructor.getParameters())));
      }
      constructor.setRecursiveParameter(constructorProto.getRecursiveParameter() - 1);
      loadKeys(constructorProto.getUserDataMap(), constructor);
    }

    int truncatedLevel = dataProto.getTruncatedLevel();
    if (truncatedLevel >= -1) {
      dataDef.setTruncatedLevel(truncatedLevel);
    }
    dataDef.setSquashed(dataProto.getIsSquashed());
    int squasher = dataProto.getSquasher();
    if (squasher != 0) {
      dataDef.setSquasher(myCallTargetProvider.getCallTarget(squasher, FunctionDefinition.class));
    }

    int index = 0;
    for (Boolean isCovariant : dataProto.getCovariantParameterList()) {
      if (isCovariant) {
        dataDef.setCovariant(index, true);
      }
      index++;
    }

    for (Constructor constructor : dataDef.getConstructors()) {
      constructor.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
    }

    readCoerceData(dataProto.getCoerceData(), dataDef.getCoerceData());
  }

  private CoerceData.Key readCoerceDataKey(DefinitionProtos.Definition.CoerceData.Element element) throws DeserializationException {
    return switch (element.getKeyCase()) {
      case DEFINITION_KEY ->
        new CoerceData.DefinitionKey(myCallTargetProvider.getCallTarget(element.getDefinitionKey().getClassifyingDef()));
      case CONSTANT_KEY -> switch (element.getConstantKey()) {
        case PI -> new CoerceData.PiKey();
        case SIGMA -> new CoerceData.SigmaKey();
        case UNIVERSE -> new CoerceData.UniverseKey();
        default -> new CoerceData.AnyKey();
      };
      default -> new CoerceData.AnyKey();
    };
  }

  private void readCoerceData(DefinitionProtos.Definition.CoerceData coerceDataProto, CoerceData coerceData) throws DeserializationException {
    for (DefinitionProtos.Definition.CoerceData.Element elementProto : coerceDataProto.getCoerceFromList()) {
      List<Definition> coercingDefs = new ArrayList<>();
      for (Integer defIndex : elementProto.getCoercingDefList()) {
        coercingDefs.add(myCallTargetProvider.getCallTarget(defIndex));
      }
      coerceData.putCoerceFrom(readCoerceDataKey(elementProto), coercingDefs);
    }

    for (DefinitionProtos.Definition.CoerceData.Element elementProto : coerceDataProto.getCoerceToList()) {
      List<Definition> coercingDefs = new ArrayList<>();
      for (Integer defIndex : elementProto.getCoercingDefList()) {
        coercingDefs.add(myCallTargetProvider.getCallTarget(defIndex));
      }
      coerceData.putCoerceTo(readCoerceDataKey(elementProto), coercingDefs);
    }
  }

  private Body readBody(ExpressionDeserialization defDeserializer, DefinitionProtos.Body proto, int numberOfParameters) throws DeserializationException {
    switch (proto.getKindCase()) {
      case ELIM_BODY -> {
        return defDeserializer.readElimBody(proto.getElimBody());
      }
      case INTERVAL_ELIM -> {
        List<IntervalElim.CasePair> cases = new ArrayList<>(proto.getIntervalElim().getCaseCount());
        for (DefinitionProtos.Body.ExpressionPair pairProto : proto.getIntervalElim().getCaseList()) {
          cases.add(new IntervalElim.CasePair(pairProto.hasLeft() ? defDeserializer.readExpr(pairProto.getLeft()) : null, pairProto.hasRight() ? defDeserializer.readExpr(pairProto.getRight()) : null));
        }
        ElimBody elimBody = null;
        if (proto.getIntervalElim().hasOtherwise()) {
          elimBody = defDeserializer.readElimBody(proto.getIntervalElim().getOtherwise());
        }
        return new IntervalElim(numberOfParameters, cases, elimBody);
      }
      case EXPRESSION -> {
        return defDeserializer.readExpr(proto.getExpression());
      }
      default -> throw new DeserializationException("Unknown body kind: " + proto.getKindCase());
    }
  }

  private ParametersLevel readParametersLevel(ExpressionDeserialization defDeserializer, DefinitionProtos.Definition.ParametersLevel proto) throws DeserializationException {
    return new ParametersLevel(proto.getHasParameters() ? defDeserializer.readParameters(proto.getParameterList()) : null, proto.getLevel());
  }

  private void fillInFunctionDefinition(ExpressionDeserialization defDeserializer, DefinitionProtos.Definition.FunctionData functionProto, FunctionDefinition functionDef) throws DeserializationException {
    functionDef.setOmegaParameters(functionProto.getOmegaParameterList());
    if (functionProto.getHasEnclosingClass()) {
      functionDef.setHasEnclosingClass(true);
    }
    List<Boolean> strictParameters = functionProto.getStrictParametersList();
    if (!strictParameters.isEmpty()) {
      functionDef.setStrictParameters(strictParameters);
    }
    functionDef.setParameters(defDeserializer.readParameters(functionProto.getParamList()));
    List<Integer> parametersTypecheckingOrder = functionProto.getParametersTypecheckingOrderList();
    if (!parametersTypecheckingOrder.isEmpty()) {
      functionDef.setParametersTypecheckingOrder(parametersTypecheckingOrder);
    }
    List<Boolean> goodThisParameters = functionProto.getGoodThisParametersList();
    if (!goodThisParameters.isEmpty()) {
      functionDef.setGoodThisParameters(goodThisParameters);
    }
    functionDef.setTypeClassParameters(readTypeClassParametersKind(functionProto.getTypeClassParametersList()));
    for (DefinitionProtos.Definition.ParametersLevel parametersLevelProto : functionProto.getParametersLevelsList()) {
      functionDef.addParametersLevel(readParametersLevel(defDeserializer, parametersLevelProto));
    }
    List<Integer> recursiveDefIndices = functionProto.getRecursiveDefinitionList();
    if (!recursiveDefIndices.isEmpty()) {
      Set<TopLevelDefinition> recursiveDefs = new HashSet<>();
      for (Integer index : recursiveDefIndices) {
        recursiveDefs.add(myCallTargetProvider.getCallTarget(index, TopLevelDefinition.class));
      }
      functionDef.setRecursiveDefinitions(recursiveDefs);
    }
    if (functionProto.hasType()) {
      functionDef.setResultType(defDeserializer.readExpr(functionProto.getType()));
    }
    if (functionProto.hasTypeLevel()) {
      functionDef.setResultTypeLevel(defDeserializer.readExpr(functionProto.getTypeLevel()));
    }
    switch (functionProto.getBodyHiddenStatus()) {
      case HIDDEN -> functionDef.hideBody();
      case REALLY_HIDDEN -> functionDef.reallyHideBody();
    }
    FunctionDefinition.Kind kind = switch (functionProto.getKind()) {
      case LEMMA, COCLAUSE_LEMMA -> CoreFunctionDefinition.Kind.LEMMA;
      case SFUNC -> CoreFunctionDefinition.Kind.SFUNC;
      case TYPE -> CoreFunctionDefinition.Kind.TYPE;
      case INSTANCE -> CoreFunctionDefinition.Kind.INSTANCE;
      default -> CoreFunctionDefinition.Kind.FUNC;
    };
    functionDef.setKind(kind);
    functionDef.setVisibleParameter(functionProto.getVisibleParameter());
    if (functionProto.hasBody()) {
      functionDef.setBody(readBody(defDeserializer, functionProto.getBody(), DependentLink.Helper.size(functionDef.getParameters())));
    }
    // setTypeClassReference(functionDef.getReferable(), functionDef.getParameters(), functionDef.getResultType());
  }

  private void fillInMetaDefinition(ExpressionDeserialization defDeserializer, DefinitionProtos.Definition.MetaData metaProto, MetaTopDefinition metaDef) throws DeserializationException {
    metaDef.setParameters(defDeserializer.readParameters(metaProto.getParamList()), new ArrayList<>(metaProto.getTypedParamList()));
  }

  private void fillInDConstructor(ExpressionDeserialization defDeserializer, DefinitionProtos.Definition.DConstructorData constructorProto, DConstructor constructorDef) throws DeserializationException {
    fillInFunctionDefinition(defDeserializer, constructorProto.getFunction(), constructorDef);
    constructorDef.setNumberOfParameters(constructorProto.getNumberOfParameters());
    if (constructorProto.hasPattern()) {
      constructorDef.setPattern(readDPattern(defDeserializer, constructorProto.getPattern()));
    }
  }

  private ExpressionPattern readDPattern(ExpressionDeserialization defDeserializer, DefinitionProtos.Definition.DPattern proto) throws DeserializationException {
    switch (proto.getKindCase()) {
      case BINDING -> {
        Binding param = defDeserializer.readBindingRef(proto.getBinding());
        if (!(param instanceof DependentLink)) {
          throw new IllegalStateException();
        }
        return new BindingPattern((DependentLink) param);
      }
      case CONSTRUCTOR -> {
        Expression expression = defDeserializer.readExpr(proto.getConstructor().getExpression());
        List<ExpressionPattern> patterns = new ArrayList<>();
        for (DefinitionProtos.Definition.DPattern pattern : proto.getConstructor().getPatternList()) {
          patterns.add(readDPattern(defDeserializer, pattern));
        }
        if (expression instanceof SmallIntegerExpression && ((SmallIntegerExpression) expression).getInteger() == 0) {
          return new ConstructorExpressionPattern(new ConCallExpression(Prelude.ZERO, Levels.EMPTY, Collections.emptyList(), Collections.emptyList()), patterns);
        }
        if (expression instanceof ConCallExpression) {
          return new ConstructorExpressionPattern((ConCallExpression) expression, patterns);
        }
        if (expression instanceof ClassCallExpression) {
          return new ConstructorExpressionPattern((ClassCallExpression) expression, patterns);
        }
        if (expression instanceof SigmaExpression) {
          return new ConstructorExpressionPattern((SigmaExpression) expression, patterns);
        }
        if (expression instanceof FunCallExpression && ((FunCallExpression) expression).getDefinition() instanceof DConstructor) {
          return new ConstructorExpressionPattern((FunCallExpression) expression, patterns);
        }
        throw new DeserializationException("Wrong pattern expression");
      }
      default -> throw new DeserializationException("Unknown Pattern kind: " + proto.getKindCase());
    }
  }

  @Override
  public @NotNull CoreDefinition getDefFromIndex(int index) throws DeserializationException {
    return myCallTargetProvider.getCallTarget(index);
  }

  @Override
  public <D extends CoreDefinition> @NotNull D getDefFromIndex(int index, Class<D> clazz) throws DeserializationException {
    CoreDefinition def = getDefFromIndex(index);
    if (!clazz.isInstance(def)) {
      throw new DeserializationException("Class mismatch\nExpected class: " + clazz.getName() + "\nActual class: " + def.getClass().getName());
    }
    return clazz.cast(def);
  }
}
