package org.arend.module.serialization;

import org.arend.core.context.LinkList;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.*;
import org.arend.core.elimtree.Body;
import org.arend.core.elimtree.ClauseBase;
import org.arend.core.elimtree.ElimTree;
import org.arend.core.elimtree.IntervalElim;
import org.arend.core.expr.*;
import org.arend.core.pattern.*;
import org.arend.core.sort.Sort;
import org.arend.naming.reference.ClassReferableImpl;
import org.arend.naming.reference.DataLocatedReferableImpl;
import org.arend.naming.reference.TCClassReferable;
import org.arend.naming.reference.TCReferable;
import org.arend.prelude.Prelude;
import org.arend.typechecking.order.dependency.DependencyListener;
import org.arend.util.Pair;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DefinitionDeserialization {
  private final CallTargetProvider myCallTargetProvider;
  private final DependencyListener myDependencyListener;

  public DefinitionDeserialization(CallTargetProvider callTargetProvider, DependencyListener dependencyListener) {
    myCallTargetProvider = callTargetProvider;
    myDependencyListener = dependencyListener;
  }

  public void fillInDefinition(DefinitionProtos.Definition defProto, Definition def, boolean typecheckDefinitionsWithErrors) throws DeserializationException {
    final ExpressionDeserialization defDeserializer = new ExpressionDeserialization(myCallTargetProvider, myDependencyListener, def.getReferable());

    switch (defProto.getDefinitionDataCase()) {
      case CLASS:
        defDeserializer.setIsHeader(false);
        ClassDefinition classDef = (ClassDefinition) def;
        fillInClassDefinition(defDeserializer, defProto.getClass_(), classDef);
        break;
      case DATA:
        DataDefinition dataDef = (DataDefinition) def;
        fillInDataDefinition(defDeserializer, defProto.getData(), dataDef);
        break;
      case FUNCTION:
        FunctionDefinition functionDef = (FunctionDefinition) def;
        fillInFunctionDefinition(defDeserializer, defProto.getFunction(), functionDef);
        break;
      default:
        throw new DeserializationException("Unknown Definition kind: " + defProto.getDefinitionDataCase());
    }

    def.setStatus(readTcStatus(defProto, typecheckDefinitionsWithErrors));
    if (defProto.getHasUniverses()) {
      def.setHasUniverses();
    }
  }

  private @Nonnull Definition.TypeCheckingStatus readTcStatus(DefinitionProtos.Definition defProto, boolean typecheckDefinitionsWithErrors) {
    if (typecheckDefinitionsWithErrors) {
      switch (defProto.getStatus()) {
        case HEADER_HAS_ERRORS:
        case BODY_HAS_ERRORS:
        case HAS_ERRORS:
          return Definition.TypeCheckingStatus.MAY_BE_TYPE_CHECKED_WITH_ERRORS;
        case HAS_WARNINGS:
          return Definition.TypeCheckingStatus.MAY_BE_TYPE_CHECKED_WITH_WARNINGS;
        case NO_ERRORS:
          return Definition.TypeCheckingStatus.NO_ERRORS;
      }
    } else {
      switch (defProto.getStatus()) {
        case HEADER_HAS_ERRORS:
          return Definition.TypeCheckingStatus.HEADER_HAS_ERRORS;
        case BODY_HAS_ERRORS:
          return Definition.TypeCheckingStatus.BODY_HAS_ERRORS;
        case HAS_ERRORS:
          return Definition.TypeCheckingStatus.HAS_ERRORS;
        case HAS_WARNINGS:
          return Definition.TypeCheckingStatus.HAS_WARNINGS;
        case NO_ERRORS:
          return Definition.TypeCheckingStatus.NO_ERRORS;
      }
    }
    throw new IllegalStateException("Unknown typechecking state");
  }

  private LamExpression checkImplementation(Expression expr, ClassDefinition classDef) throws DeserializationException {
    if (expr instanceof LamExpression) {
      LamExpression lamExpr = (LamExpression) expr;
      if (!lamExpr.getParameters().getNext().hasNext()) {
        Expression type = lamExpr.getParameters().getTypeExpr();
        if (type instanceof ClassCallExpression) {
          return lamExpr;
        }
      }
    }
    throw new DeserializationException("Incorrect class field implementation");
  }

  private PiExpression checkFieldType(Expression expr, ClassDefinition classDef) throws DeserializationException {
    if (expr instanceof PiExpression) {
      PiExpression piExpr = (PiExpression) expr;
      if (!piExpr.getParameters().getNext().hasNext()) {
        Expression type = piExpr.getParameters().getTypeExpr();
        if (type instanceof ClassCallExpression && ((ClassCallExpression) type).getDefinition().equals(classDef)) {
          return piExpr;
        }
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
      PiExpression fieldType = checkFieldType(defDeserializer.readExpr(fieldProto.getType()), classDef);
      if (fieldProto.getIsProperty()) {
        field.setIsProperty();
      }
      field.setType(fieldType);
      // setTypeClassReference(field.getReferable(), EmptyDependentLink.getInstance(), fieldType.getCodomain());
      field.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
    }

    for (int classFieldRef : classProto.getFieldRefList()) {
      classDef.addField(myCallTargetProvider.getCallTarget(classFieldRef, ClassField.class));
    }
    for (Map.Entry<Integer, ExpressionProtos.Expression> entry : classProto.getImplementationsMap().entrySet()) {
      classDef.implementField(myCallTargetProvider.getCallTarget(entry.getKey(), ClassField.class), checkImplementation(defDeserializer.readExpr(entry.getValue()), classDef));
    }
    classDef.setSort(defDeserializer.readSort(classProto.getSort()));

    for (int superClassRef : classProto.getSuperClassRefList()) {
      ClassDefinition superClass = myCallTargetProvider.getCallTarget(superClassRef, ClassDefinition.class);
      classDef.addSuperClass(superClass);
      myDependencyListener.dependsOn(classDef.getReferable(), true, superClass.getReferable());
      TCClassReferable classRef = classDef.getReferable();
      if (classRef instanceof ClassReferableImpl) {
        ((ClassReferableImpl) classRef).getSuperClassReferences().add(superClass.getReferable());
      }

      for (Map.Entry<ClassField, LamExpression> entry : superClass.getImplemented()) {
        classDef.implementField(entry.getKey(), entry.getValue());
      }
    }

    if (classProto.getCoercingFieldRef() != -1) {
      classDef.setClassifyingField(myCallTargetProvider.getCallTarget(classProto.getCoercingFieldRef(), ClassField.class));
    }
    if (classProto.getIsRecord()) {
      classDef.setRecord();
    }

    readCoerceData(classProto.getCoerceData(), classDef.getCoerceData(), classDef);
  }

  private void fillInDataDefinition(ExpressionDeserialization defDeserializer, DefinitionProtos.Definition.DataData dataProto, DataDefinition dataDef) throws DeserializationException {
    dataDef.setParameters(defDeserializer.readParameters(dataProto.getParamList()));
    List<Integer> parametersTypecheckingOrder = dataProto.getParametersTypecheckingOrderList();
    if (!parametersTypecheckingOrder.isEmpty()) {
      dataDef.setParametersTypecheckingOrder(parametersTypecheckingOrder);
    }
    dataDef.setSort(defDeserializer.readSort(dataProto.getSort()));
    defDeserializer.setIsHeader(false);

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
      if (constructorProto.hasConditions()) {
        constructor.setBody(readBody(defDeserializer, constructorProto.getConditions()));
      }
      constructor.setNumberOfIntervalParameters(constructorProto.getNumberOfIntervalParameters());
    }

    if (dataProto.getMatchesOnInterval()) {
      dataDef.setMatchesOnInterval();
    }
    dataDef.setIsTruncated(dataProto.getIsTruncated());

    int index = 0;
    for (Boolean isCovariant : dataProto.getCovariantParameterList()) {
      if (isCovariant) {
        dataDef.setCovariant(index);
      }
      index++;
    }

    for (Constructor constructor : dataDef.getConstructors()) {
      constructor.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
    }

    readCoerceData(dataProto.getCoerceData(), dataDef.getCoerceData(), dataDef);
  }

  private void readCoerceData(DefinitionProtos.Definition.CoerceData coerceDataProto, CoerceData coerceData, Definition definition) throws DeserializationException {
    for (DefinitionProtos.Definition.CoerceData.Element elementProto : coerceDataProto.getCoerceFromList()) {
      List<FunctionDefinition> coercingDefs = new ArrayList<>();
      for (Integer defIndex : elementProto.getCoercingDefList()) {
        FunctionDefinition coercingDef = myCallTargetProvider.getCallTarget(defIndex, FunctionDefinition.class);
        myDependencyListener.dependsOn(definition.getReferable(), true, coercingDef.getReferable());
        coercingDefs.add(coercingDef);
      }
      int classifyingDefIndex = elementProto.getClassifyingDef();
      coerceData.putCoerceFrom(classifyingDefIndex == 0 ? null : myCallTargetProvider.getCallTarget(classifyingDefIndex - 1), coercingDefs);
    }

    for (DefinitionProtos.Definition.CoerceData.Element elementProto : coerceDataProto.getCoerceToList()) {
      List<Definition> coercingDefs = new ArrayList<>();
      for (Integer defIndex : elementProto.getCoercingDefList()) {
        Definition coercingDef = myCallTargetProvider.getCallTarget(defIndex);
        myDependencyListener.dependsOn(definition.getReferable(), true, coercingDef.getReferable());
        coercingDefs.add(coercingDef);
      }
      int classifyingDefIndex = elementProto.getClassifyingDef();
      coerceData.putCoerceTo(classifyingDefIndex == 0 ? null : myCallTargetProvider.getCallTarget(classifyingDefIndex - 1), coercingDefs);
    }
  }

  private ClauseBase readClause(ExpressionDeserialization defDeserializer, DefinitionProtos.Definition.Clause clause) throws DeserializationException {
    return new ClauseBase(readPatterns(defDeserializer, clause.getPatternList(), new LinkList()).getPatternList(), defDeserializer.readExpr(clause.getExpression()));
  }

  private Body readBody(ExpressionDeserialization defDeserializer, DefinitionProtos.Body proto) throws DeserializationException {
    switch (proto.getKindCase()) {
      case ELIM_TREE:
        return defDeserializer.readElimTree(proto.getElimTree());
      case INTERVAL_ELIM:
        DependentLink parameters = defDeserializer.readParameters(proto.getIntervalElim().getParamList());
        List<Pair<Expression, Expression>> cases = new ArrayList<>(proto.getIntervalElim().getCaseCount());
        for (DefinitionProtos.Body.ExpressionPair pairProto : proto.getIntervalElim().getCaseList()) {
          cases.add(new Pair<>(pairProto.hasLeft() ? defDeserializer.readExpr(pairProto.getLeft()) : null, pairProto.hasRight() ? defDeserializer.readExpr(pairProto.getRight()) : null));
        }
        ElimTree elimTree = null;
        if (proto.getIntervalElim().hasOtherwise()) {
          elimTree = defDeserializer.readElimTree(proto.getIntervalElim().getOtherwise());
        }
        return new IntervalElim(parameters, cases, elimTree);
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
        throw new DeserializationException("Wrong pattern expression");
      default:
        throw new DeserializationException("Unknown Pattern kind: " + proto.getKindCase());
    }
  }

  private void fillInFunctionDefinition(ExpressionDeserialization defDeserializer, DefinitionProtos.Definition.FunctionData functionProto, FunctionDefinition functionDef) throws DeserializationException {
    functionDef.setParameters(defDeserializer.readParameters(functionProto.getParamList()));
    List<Integer> parametersTypecheckingOrder = functionProto.getParametersTypecheckingOrderList();
    if (!parametersTypecheckingOrder.isEmpty()) {
      functionDef.setParametersTypecheckingOrder(parametersTypecheckingOrder);
    }
    if (functionProto.hasType()) {
      functionDef.setResultType(defDeserializer.readExpr(functionProto.getType()));
    }
    defDeserializer.setIsHeader(false);
    if (functionProto.hasBody()) {
      functionDef.setBody(readBody(defDeserializer, functionProto.getBody()));
    }
    // setTypeClassReference(functionDef.getReferable(), functionDef.getParameters(), functionDef.getResultType());
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
