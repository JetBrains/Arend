package com.jetbrains.jetpad.vclang.module.serialization;

import com.jetbrains.jetpad.vclang.core.context.LinkList;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.*;
import com.jetbrains.jetpad.vclang.core.elimtree.Body;
import com.jetbrains.jetpad.vclang.core.elimtree.ClauseBase;
import com.jetbrains.jetpad.vclang.core.elimtree.ElimTree;
import com.jetbrains.jetpad.vclang.core.elimtree.IntervalElim;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.pattern.*;
import com.jetbrains.jetpad.vclang.naming.reference.ClassReferableImpl;
import com.jetbrains.jetpad.vclang.naming.reference.DataLocatedReferableImpl;
import com.jetbrains.jetpad.vclang.naming.reference.TCClassReferable;
import com.jetbrains.jetpad.vclang.naming.reference.TCReferable;
import com.jetbrains.jetpad.vclang.typechecking.order.dependency.DependencyListener;
import com.jetbrains.jetpad.vclang.util.Pair;

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
  }

  private @Nonnull Definition.TypeCheckingStatus readTcStatus(DefinitionProtos.Definition defProto, boolean typecheckDefinitionsWithErrors) {
    if (typecheckDefinitionsWithErrors) {
      switch (defProto.getStatus()) {
        case BODY_HAS_ERRORS:
          return Definition.TypeCheckingStatus.BODY_NEEDS_TYPE_CHECKING;
        case NO_ERRORS:
          return Definition.TypeCheckingStatus.NO_ERRORS;
        default:
          return Definition.TypeCheckingStatus.HEADER_NEEDS_TYPE_CHECKING;
      }
    } else {
      switch (defProto.getStatus()) {
        case HEADER_HAS_ERRORS:
          return Definition.TypeCheckingStatus.HEADER_HAS_ERRORS;
        case BODY_HAS_ERRORS:
          return Definition.TypeCheckingStatus.BODY_HAS_ERRORS;
        case HAS_ERRORS:
          return Definition.TypeCheckingStatus.HAS_ERRORS;
        case NO_ERRORS:
          return Definition.TypeCheckingStatus.NO_ERRORS;
        default:
          throw new IllegalStateException("Unknown typechecking state");
      }
    }
  }

  private LamExpression checkImplementation(Expression expr, ClassDefinition classDef) throws DeserializationException {
    if (expr instanceof LamExpression) {
      LamExpression lamExpr = (LamExpression) expr;
      if (!lamExpr.getParameters().getNext().hasNext()) {
        Expression type = lamExpr.getParameters().getTypeExpr();
        if (type instanceof ClassCallExpression && ((ClassCallExpression) type).getDefinition().equals(classDef)) {
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
      field.setType(fieldType);
      classDef.addPersonalField(field);
      setTypeClassReference(field.getReferable(), EmptyDependentLink.getInstance(), fieldType.getCodomain());
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
      TCClassReferable classRef = classDef.getReferable();
      if (classRef instanceof ClassReferableImpl) {
        ((ClassReferableImpl) classRef).getSuperClassReferences().add(superClass.getReferable());
      }

      for (Map.Entry<ClassField, LamExpression> entry : superClass.getImplemented()) {
        classDef.implementField(entry.getKey(), entry.getValue());
      }
    }

    if (classProto.getCoercingFieldRef() != -1) {
      classDef.setCoercingField(myCallTargetProvider.getCallTarget(classProto.getCoercingFieldRef(), ClassField.class));
    }
  }

  private void fillInDataDefinition(ExpressionDeserialization defDeserializer, DefinitionProtos.Definition.DataData dataProto, DataDefinition dataDef) throws DeserializationException {
    dataDef.setParameters(defDeserializer.readParameters(dataProto.getParamList()));
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
      if (constructorProto.hasConditions()) {
        constructor.setBody(readBody(defDeserializer, constructorProto.getConditions()));
      }
      dataDef.addConstructor(constructor);
    }

    if (dataProto.getMatchesOnInterval()) {
      dataDef.setMatchesOnInterval();
    }

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
        return new ConstructorPattern(
          new ConCallExpression(
            myCallTargetProvider.getCallTarget(proto.getConstructor().getConstructorRef(), Constructor.class),
            defDeserializer.readSort(proto.getConstructor().getSortArgument()),
            defDeserializer.readExprList(proto.getConstructor().getDataTypeArgumentList()),
            Collections.emptyList()
          ), readPatterns(defDeserializer, proto.getConstructor().getPatternList(), list));
      default:
        throw new DeserializationException("Unknown Pattern kind: " + proto.getKindCase());
    }
  }

  private void fillInFunctionDefinition(ExpressionDeserialization defDeserializer, DefinitionProtos.Definition.FunctionData functionProto, FunctionDefinition functionDef) throws DeserializationException {
    functionDef.setParameters(defDeserializer.readParameters(functionProto.getParamList()));
    if (functionProto.hasType()) {
      functionDef.setResultType(defDeserializer.readExpr(functionProto.getType()));
    }
    defDeserializer.setIsHeader(false);
    if (functionProto.hasBody()) {
      functionDef.setBody(readBody(defDeserializer, functionProto.getBody()));
    }
    setTypeClassReference(functionDef.getReferable(), functionDef.getParameters(), functionDef.getResultType());
  }

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
