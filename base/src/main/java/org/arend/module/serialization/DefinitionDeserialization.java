package org.arend.module.serialization;

import com.google.protobuf.ByteString;
import org.arend.core.context.LinkList;
import org.arend.core.context.binding.Binding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.*;
import org.arend.core.elimtree.*;
import org.arend.core.expr.*;
import org.arend.core.pattern.*;
import org.arend.core.sort.Sort;
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
import org.arend.util.Pair;
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
    final ExpressionDeserialization defDeserializer = new ExpressionDeserialization(myCallTargetProvider, myDependencyListener, def.getReferable());

    switch (defProto.getDefinitionDataCase()) {
      case CLASS:
        fillInClassDefinition(defDeserializer, defProto.getClass_(), (ClassDefinition) def);
        break;
      case DATA:
        fillInDataDefinition(defDeserializer, defProto.getData(), (DataDefinition) def);
        break;
      case CONSTRUCTOR:
        fillInDConstructor(defDeserializer, defProto.getConstructor(), (DConstructor) def);
        break;
      case FUNCTION:
        fillInFunctionDefinition(defDeserializer, defProto.getFunction(), (FunctionDefinition) def);
        break;
      default:
        throw new DeserializationException("Unknown Definition kind: " + defProto.getDefinitionDataCase());
    }

    def.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
    def.setUniverseKind(defDeserializer.readUniverseKind(defProto.getUniverseKind()));

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
        field.setTypeLevel(defDeserializer.readExpr(fieldProto.getTypeLevel()));
      }
      field.setNumberOfParameters(fieldProto.getNumberOfParameters());
      // setTypeClassReference(field.getReferable(), EmptyDependentLink.getInstance(), fieldType.getCodomain());
      field.setHideable(fieldProto.getIsHideable());
      field.setCovariant(fieldProto.getIsCovariant());
      field.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
      field.setUniverseKind(defDeserializer.readUniverseKind(fieldProto.getUniverseKind()));
      loadKeys(fieldProto.getUserDataMap(), field);
    }

    for (int classFieldRef : classProto.getFieldRefList()) {
      classDef.addField(myCallTargetProvider.getCallTarget(classFieldRef, ClassField.class));
    }
    for (Map.Entry<Integer, ExpressionProtos.Expression.Abs> entry : classProto.getImplementationsMap().entrySet()) {
      classDef.implementField(myCallTargetProvider.getCallTarget(entry.getKey(), ClassField.class), defDeserializer.readAbsExpr(entry.getValue()));
    }
    for (Map.Entry<Integer, ExpressionProtos.Expression.Pi> entry : classProto.getOverriddenFieldMap().entrySet()) {
      classDef.overrideField(myCallTargetProvider.getCallTarget(entry.getKey(), ClassField.class), checkFieldType(defDeserializer.readPi(entry.getValue()), classDef));
    }
    classDef.setSort(defDeserializer.readSort(classProto.getSort()));

    for (int superClassRef : classProto.getSuperClassRefList()) {
      ClassDefinition superClass = myCallTargetProvider.getCallTarget(superClassRef, ClassDefinition.class);
      classDef.addSuperClass(superClass);
      myDependencyListener.dependsOn(classDef.getReferable(), superClass.getReferable());
      TCReferable classRef = classDef.getReferable();
      if (classRef instanceof ClassReferableImpl) {
        Referable superRef = superClass.getReferable().getUnderlyingReferable();
        if (superRef instanceof ClassReferable) {
          ((ClassReferableImpl) classRef).getSuperClassReferences().add((ClassReferable) superRef);
        }
      }

      for (Map.Entry<ClassField, AbsExpression> entry : superClass.getImplemented()) {
        classDef.implementField(entry.getKey(), entry.getValue());
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

  private List<Definition.TypeClassParameterKind> readTypeClassParametersKind(List<DefinitionProtos.Definition.TypeClassParameterKind> kinds) {
    List<Definition.TypeClassParameterKind> result = new ArrayList<>(kinds.size());
    for (DefinitionProtos.Definition.TypeClassParameterKind kind : kinds) {
      switch (kind) {
        case YES:
          result.add(Definition.TypeClassParameterKind.YES);
          break;
        case ONLY_LOCAL:
          result.add(Definition.TypeClassParameterKind.ONLY_LOCAL);
          break;
        default:
          result.add(Definition.TypeClassParameterKind.NO);
          break;
      }
    }
    return result;
  }

  private void fillInDataDefinition(ExpressionDeserialization defDeserializer, DefinitionProtos.Definition.DataData dataProto, DataDefinition dataDef) throws DeserializationException {
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
      Set<Definition> recursiveDefs = new HashSet<>();
      for (Integer index : recursiveDefIndices) {
        recursiveDefs.add(myCallTargetProvider.getCallTarget(index));
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

  private void readCoerceData(DefinitionProtos.Definition.CoerceData coerceDataProto, CoerceData coerceData) throws DeserializationException {
    for (DefinitionProtos.Definition.CoerceData.Element elementProto : coerceDataProto.getCoerceFromList()) {
      List<FunctionDefinition> coercingDefs = new ArrayList<>();
      for (Integer defIndex : elementProto.getCoercingDefList()) {
        coercingDefs.add(myCallTargetProvider.getCallTarget(defIndex, FunctionDefinition.class));
      }
      int classifyingDefIndex = elementProto.getClassifyingDef();
      coerceData.putCoerceFrom(classifyingDefIndex == 0 ? null : myCallTargetProvider.getCallTarget(classifyingDefIndex - 1), coercingDefs);
    }

    for (DefinitionProtos.Definition.CoerceData.Element elementProto : coerceDataProto.getCoerceToList()) {
      List<Definition> coercingDefs = new ArrayList<>();
      for (Integer defIndex : elementProto.getCoercingDefList()) {
        coercingDefs.add(myCallTargetProvider.getCallTarget(defIndex));
      }
      int classifyingDefIndex = elementProto.getClassifyingDef();
      coerceData.putCoerceTo(classifyingDefIndex == 0 ? null : myCallTargetProvider.getCallTarget(classifyingDefIndex - 1), coercingDefs);
    }
  }

  private Body readBody(ExpressionDeserialization defDeserializer, DefinitionProtos.Body proto, int numberOfParameters) throws DeserializationException {
    switch (proto.getKindCase()) {
      case ELIM_BODY:
        return defDeserializer.readElimBody(proto.getElimBody());
      case INTERVAL_ELIM:
        List<IntervalElim.CasePair> cases = new ArrayList<>(proto.getIntervalElim().getCaseCount());
        for (DefinitionProtos.Body.ExpressionPair pairProto : proto.getIntervalElim().getCaseList()) {
          cases.add(new IntervalElim.CasePair(pairProto.hasLeft() ? defDeserializer.readExpr(pairProto.getLeft()) : null, pairProto.hasRight() ? defDeserializer.readExpr(pairProto.getRight()) : null));
        }
        ElimBody elimBody = null;
        if (proto.getIntervalElim().hasOtherwise()) {
          elimBody = defDeserializer.readElimBody(proto.getIntervalElim().getOtherwise());
        }
        return new IntervalElim(numberOfParameters, cases, elimBody);
      case EXPRESSION:
        return defDeserializer.readExpr(proto.getExpression());
      default:
        throw new DeserializationException("Unknown body kind: " + proto.getKindCase());
    }
  }

  private ParametersLevel readParametersLevel(ExpressionDeserialization defDeserializer, DefinitionProtos.Definition.ParametersLevel proto) throws DeserializationException {
    return new ParametersLevel(proto.getHasParameters() ? defDeserializer.readParameters(proto.getParameterList()) : null, proto.getLevel());
  }

  private void fillInFunctionDefinition(ExpressionDeserialization defDeserializer, DefinitionProtos.Definition.FunctionData functionProto, FunctionDefinition functionDef) throws DeserializationException {
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
      Set<Definition> recursiveDefs = new HashSet<>();
      for (Integer index : recursiveDefIndices) {
        recursiveDefs.add(myCallTargetProvider.getCallTarget(index));
      }
      functionDef.setRecursiveDefinitions(recursiveDefs);
    }
    if (functionProto.hasType()) {
      functionDef.setResultType(defDeserializer.readExpr(functionProto.getType()));
    }
    if (functionProto.hasTypeLevel()) {
      functionDef.setResultTypeLevel(defDeserializer.readExpr(functionProto.getTypeLevel()));
    }
    if (functionProto.getBodyIsHidden()) {
      functionDef.hideBody();
    }
    FunctionDefinition.Kind kind;
    switch (functionProto.getKind()) {
      case LEMMA:
        kind = CoreFunctionDefinition.Kind.LEMMA;
        break;
      case SFUNC:
        kind = CoreFunctionDefinition.Kind.SFUNC;
        break;
      case INSTANCE:
        kind = CoreFunctionDefinition.Kind.INSTANCE;
        break;
      default:
        kind = CoreFunctionDefinition.Kind.FUNC;
    }
    functionDef.setKind(kind);
    functionDef.setVisibleParameter(functionProto.getVisibleParameter());
    if (functionProto.hasBody()) {
      functionDef.setBody(readBody(defDeserializer, functionProto.getBody(), DependentLink.Helper.size(functionDef.getParameters())));
    }
    // setTypeClassReference(functionDef.getReferable(), functionDef.getParameters(), functionDef.getResultType());
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
      case BINDING:
        Binding param = defDeserializer.readBindingRef(proto.getBinding());
        if (!(param instanceof DependentLink)) {
          throw new IllegalStateException();
        }
        return new BindingPattern((DependentLink) param);
      case CONSTRUCTOR:
        Expression expression = defDeserializer.readExpr(proto.getConstructor().getExpression());
        List<ExpressionPattern> patterns = new ArrayList<>();
        for (DefinitionProtos.Definition.DPattern pattern : proto.getConstructor().getPatternList()) {
          patterns.add(readDPattern(defDeserializer, pattern));
        }
        if (expression instanceof SmallIntegerExpression && ((SmallIntegerExpression) expression).getInteger() == 0) {
          return new ConstructorExpressionPattern(new ConCallExpression(Prelude.ZERO, Sort.PROP, Collections.emptyList(), Collections.emptyList()), patterns);
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
      default:
        throw new DeserializationException("Unknown Pattern kind: " + proto.getKindCase());
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
