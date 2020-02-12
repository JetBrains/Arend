package org.arend.module.serialization;

import org.arend.core.context.LinkList;
import org.arend.core.context.binding.Binding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.*;
import org.arend.core.elimtree.Body;
import org.arend.core.elimtree.ClauseBase;
import org.arend.core.elimtree.ElimTree;
import org.arend.core.elimtree.IntervalElim;
import org.arend.core.expr.*;
import org.arend.core.pattern.*;
import org.arend.core.sort.Sort;
import org.arend.ext.core.definition.CoreFunctionDefinition;
import org.arend.naming.reference.ClassReferableImpl;
import org.arend.naming.reference.DataLocatedReferableImpl;
import org.arend.naming.reference.TCClassReferable;
import org.arend.naming.reference.TCReferable;
import org.arend.prelude.Prelude;
import org.arend.typechecking.order.dependency.DependencyListener;

import java.util.*;

public class DefinitionDeserialization {
  private final CallTargetProvider myCallTargetProvider;
  private final DependencyListener myDependencyListener;

  public DefinitionDeserialization(CallTargetProvider callTargetProvider, DependencyListener dependencyListener) {
    myCallTargetProvider = callTargetProvider;
    myDependencyListener = dependencyListener;
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
    def.setPLevelKind(defDeserializer.readUniverseKind(defProto.getPLevelKind()));
    def.setHLevelKind(defDeserializer.readUniverseKind(defProto.getHLevelKind()));
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
      field.setPLevelKind(defDeserializer.readUniverseKind(fieldProto.getPLevelKind()));
      field.setHLevelKind(defDeserializer.readUniverseKind(fieldProto.getHLevelKind()));
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
      TCClassReferable classRef = classDef.getReferable();
      if (classRef instanceof ClassReferableImpl) {
        ((ClassReferableImpl) classRef).getSuperClassReferences().add(superClass.getReferable());
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
      classDef.addParametersLevel(new ClassDefinition.ParametersLevel(parametersLevelProto.getHasParameters() ? defDeserializer.readParameters(parametersLevelProto.getParameterList()) : null, parametersLevelProto.getLevel(), fields));
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
    dataDef.setSort(defDeserializer.readSort(dataProto.getSort()));

    for (DefinitionProtos.Definition.DataData.Constructor constructorProto : dataProto.getConstructorList()) {
      Constructor constructor = myCallTargetProvider.getCallTarget(constructorProto.getReferable().getIndex(), Constructor.class);
      if (constructorProto.getPatternCount() > 0) {
        constructor.setPatterns(readPatterns(defDeserializer, constructorProto.getPatternList(), new LinkList()));
      }
      if (constructorProto.getClauseCount() > 0) {
        List<ClauseBase> clauses = new ArrayList<>(constructorProto.getClauseCount());
        for (DefinitionProtos.Definition.Clause clause : constructorProto.getClauseList()) {
          clauses.add(readClause(defDeserializer, clause));
        }
        constructor.setClauses(clauses);
      }
      constructor.setParameters(defDeserializer.readParameters(constructorProto.getParamList()));
      List<Integer> constructorParametersTypecheckingOrder = constructorProto.getParametersTypecheckingOrderList();
      if (!parametersTypecheckingOrder.isEmpty()) {
        constructor.setParametersTypecheckingOrder(constructorParametersTypecheckingOrder);
      }
      List<Boolean> cGoodThisParameters = constructorProto.getGoodThisParametersList();
      if (!cGoodThisParameters.isEmpty()) {
        constructor.setGoodThisParameters(cGoodThisParameters);
      }
      constructor.setTypeClassParameters(readTypeClassParametersKind(constructorProto.getTypeClassParametersList()));
      if (constructorProto.hasConditions()) {
        constructor.setBody(readBody(defDeserializer, constructorProto.getConditions(), DependentLink.Helper.size(constructor.getParameters())));
      }
    }

    dataDef.setTruncated(dataProto.getIsTruncated());
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

  private ClauseBase readClause(ExpressionDeserialization defDeserializer, DefinitionProtos.Definition.Clause clause) throws DeserializationException {
    return new ClauseBase(readPatterns(defDeserializer, clause.getPatternList(), new LinkList()).getPatternList(), defDeserializer.readExpr(clause.getExpression()));
  }

  private Body readBody(ExpressionDeserialization defDeserializer, DefinitionProtos.Body proto, int numberOfParameters) throws DeserializationException {
    switch (proto.getKindCase()) {
      case ELIM_TREE:
        return defDeserializer.readElimTree(proto.getElimTree());
      case INTERVAL_ELIM:
        List<IntervalElim.CasePair> cases = new ArrayList<>(proto.getIntervalElim().getCaseCount());
        for (DefinitionProtos.Body.ExpressionPair pairProto : proto.getIntervalElim().getCaseList()) {
          cases.add(new IntervalElim.CasePair(pairProto.hasLeft() ? defDeserializer.readExpr(pairProto.getLeft()) : null, pairProto.hasRight() ? defDeserializer.readExpr(pairProto.getRight()) : null));
        }
        ElimTree elimTree = null;
        if (proto.getIntervalElim().hasOtherwise()) {
          elimTree = defDeserializer.readElimTree(proto.getIntervalElim().getOtherwise());
        }
        return new IntervalElim(numberOfParameters, cases, elimTree);
      case EXPRESSION:
        return defDeserializer.readExpr(proto.getExpression());
      default:
        throw new DeserializationException("Unknown body kind: " + proto.getKindCase());
    }
  }

  private Patterns readPatterns(ExpressionDeserialization defDeserializer, List<DefinitionProtos.Definition.Pattern> protos, LinkList list) throws DeserializationException {
    List<Pattern> patterns = new ArrayList<>(protos.size());
    for (DefinitionProtos.Definition.Pattern proto : protos) {
      patterns.add(readPattern(defDeserializer, proto, list));
    }
    return new Patterns(patterns);
  }

  private Pattern readPattern(ExpressionDeserialization defDeserializer, DefinitionProtos.Definition.Pattern proto, LinkList list) throws DeserializationException {
    switch (proto.getKindCase()) {
      case BINDING:
        DependentLink param = defDeserializer.readParameter(proto.getBinding().getVar());
        list.append(param);
        return new BindingPattern(param);
      case EMPTY:
        return EmptyPattern.INSTANCE;
      case CONSTRUCTOR:
        Expression expression = defDeserializer.readExpr(proto.getConstructor().getExpression());
        Patterns patterns = readPatterns(defDeserializer, proto.getConstructor().getPatternList(), list);
        if (expression instanceof SmallIntegerExpression && ((SmallIntegerExpression) expression).getInteger() == 0) {
          return new ConstructorPattern(new ConCallExpression(Prelude.ZERO, Sort.PROP, Collections.emptyList(), Collections.emptyList()), patterns);
        }
        if (expression instanceof ConCallExpression) {
          return new ConstructorPattern((ConCallExpression) expression, patterns);
        }
        if (expression instanceof ClassCallExpression) {
          return new ConstructorPattern((ClassCallExpression) expression, patterns);
        }
        if (expression instanceof SigmaExpression) {
          return new ConstructorPattern((SigmaExpression) expression, patterns);
        }
        if (expression instanceof FunCallExpression && ((FunCallExpression) expression).getDefinition() instanceof DConstructor) {
          return new ConstructorPattern((FunCallExpression) expression, patterns);
        }
        throw new DeserializationException("Wrong pattern expression");
      default:
        throw new DeserializationException("Unknown Pattern kind: " + proto.getKindCase());
    }
  }

  private ParametersLevel readParametersLevel(ExpressionDeserialization defDeserializer, DefinitionProtos.Definition.ParametersLevel proto) throws DeserializationException {
    return new ParametersLevel(proto.getHasParameters() ? defDeserializer.readParameters(proto.getParameterList()) : null, proto.getLevel());
  }

  private void fillInFunctionDefinition(ExpressionDeserialization defDeserializer, DefinitionProtos.Definition.FunctionData functionProto, FunctionDefinition functionDef) throws DeserializationException {
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

  private Pattern readDPattern(ExpressionDeserialization defDeserializer, DefinitionProtos.Definition.DPattern proto) throws DeserializationException {
    switch (proto.getKindCase()) {
      case BINDING:
        Binding param = defDeserializer.readBindingRef(proto.getBinding());
        if (!(param instanceof DependentLink)) {
          throw new IllegalStateException();
        }
        return new BindingPattern((DependentLink) param);
      case CONSTRUCTOR:
        Expression expression = defDeserializer.readExpr(proto.getConstructor().getExpression());
        List<Pattern> patternList = new ArrayList<>();
        for (DefinitionProtos.Definition.DPattern pattern : proto.getConstructor().getPatternList()) {
          patternList.add(readDPattern(defDeserializer, pattern));
        }
        Patterns patterns = new Patterns(patternList);
        if (expression instanceof SmallIntegerExpression && ((SmallIntegerExpression) expression).getInteger() == 0) {
          return new ConstructorPattern(new ConCallExpression(Prelude.ZERO, Sort.PROP, Collections.emptyList(), Collections.emptyList()), patterns);
        }
        if (expression instanceof ConCallExpression) {
          return new ConstructorPattern((ConCallExpression) expression, patterns);
        }
        if (expression instanceof ClassCallExpression) {
          return new ConstructorPattern((ClassCallExpression) expression, patterns);
        }
        if (expression instanceof SigmaExpression) {
          return new ConstructorPattern((SigmaExpression) expression, patterns);
        }
        if (expression instanceof FunCallExpression && ((FunCallExpression) expression).getDefinition() instanceof DConstructor) {
          return new ConstructorPattern((FunCallExpression) expression, patterns);
        }
        throw new DeserializationException("Wrong pattern expression");
      default:
        throw new DeserializationException("Unknown Pattern kind: " + proto.getKindCase());
    }
  }

  // To implement this function properly, we need to serialize references to class synonyms
  private void setTypeClassReference(TCReferable referable, DependentLink parameters, Expression type) {
    if (!(referable instanceof DataLocatedReferableImpl)) {
      return;
    }

    for (; parameters.hasNext(); parameters = parameters.getNext()) {
      parameters = parameters.getNextTyped(null);
      if (parameters.isExplicit()) {
        return;
      }
    }

    while (type instanceof PiExpression) {
      for (DependentLink link = ((PiExpression) type).getParameters(); link.hasNext(); link = link.getNext()) {
        link = link.getNextTyped(null);
        if (link.isExplicit()) {
          return;
        }
      }
      type = ((PiExpression) type).getCodomain();
    }

    if (type instanceof ClassCallExpression) {
      ((DataLocatedReferableImpl) referable).setTypeClassReference(((ClassCallExpression) type).getDefinition().getReferable());
    }
  }
}
