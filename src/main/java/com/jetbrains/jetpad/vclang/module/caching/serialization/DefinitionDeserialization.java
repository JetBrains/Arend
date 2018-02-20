package com.jetbrains.jetpad.vclang.module.caching.serialization;

import com.jetbrains.jetpad.vclang.core.context.LinkList;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.TypedDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.*;
import com.jetbrains.jetpad.vclang.core.elimtree.Body;
import com.jetbrains.jetpad.vclang.core.elimtree.ClauseBase;
import com.jetbrains.jetpad.vclang.core.elimtree.ElimTree;
import com.jetbrains.jetpad.vclang.core.elimtree.IntervalElim;
import com.jetbrains.jetpad.vclang.core.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.pattern.*;
import com.jetbrains.jetpad.vclang.naming.reference.ClassReferable;
import com.jetbrains.jetpad.vclang.naming.reference.SimpleClassReferable;
import com.jetbrains.jetpad.vclang.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DefinitionDeserialization {
  private final CallTargetProvider myCallTargetProvider;

  public DefinitionDeserialization(CallTargetProvider callTargetProvider) {
    myCallTargetProvider = callTargetProvider;
  }

  public void fillInDefinition(DefinitionProtos.Definition defProto, Definition def) throws DeserializationException {
    final ExpressionDeserialization defDeserializer = new ExpressionDeserialization(myCallTargetProvider);

    if (defProto.getThisClassRef() != 0) {
      def.setThisClass(myCallTargetProvider.getCallTarget(defProto.getThisClassRef(), ClassDefinition.class));
    }

    switch (defProto.getDefinitionDataCase()) {
      case CLASS:
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
  }

  private void fillInClassDefinition(ExpressionDeserialization defDeserializer, DefinitionProtos.Definition.ClassData classProto, ClassDefinition classDef) throws DeserializationException {
    for (int classFieldRef : classProto.getFieldRefList()) {
      classDef.addField(myCallTargetProvider.getCallTarget(classFieldRef, ClassField.class));
    }
    for (Map.Entry<Integer, DefinitionProtos.Definition.ClassData.Implementation> entry : classProto.getImplementationsMap().entrySet()) {
      TypedDependentLink thisParam = (TypedDependentLink) defDeserializer.readParameter(entry.getValue().getThisParam());
      ClassDefinition.Implementation impl = new ClassDefinition.Implementation(thisParam, defDeserializer.readExpr(entry.getValue().getTerm()));
      classDef.implementField(myCallTargetProvider.getCallTarget(entry.getKey(), ClassField.class), impl);
    }
    classDef.setSort(defDeserializer.readSort(classProto.getSort()));
    if (classProto.getCoercingFieldRef() != -1) {
      classDef.setCoercingField(myCallTargetProvider.getCallTarget(classProto.getCoercingFieldRef(), ClassField.class));
    }

    for (int superClassRef : classProto.getSuperClassRefList()) {
      ClassDefinition superClass = myCallTargetProvider.getCallTarget(superClassRef, ClassDefinition.class);
      classDef.addSuperClass(superClass);
      ((SimpleClassReferable) classDef.getReferable()).getSuperClassReferences().add((ClassReferable) superClass.getReferable());
    }
    if (classProto.getEnclosingThisFieldRef() != 0) {
      classDef.setEnclosingThisField(myCallTargetProvider.getCallTarget(classProto.getEnclosingThisFieldRef(), ClassField.class));
    }

    for (DefinitionProtos.Definition.ClassData.Field fieldProto : classProto.getPersonalFieldList()) {
      ClassField field = myCallTargetProvider.getCallTarget(fieldProto.getReferable().getIndex(), ClassField.class);
      field.setThisParameter((TypedDependentLink) defDeserializer.readParameter(fieldProto.getThisParam()));
      if (fieldProto.hasType()) {
        field.setBaseType(defDeserializer.readExpr(fieldProto.getType()));
      }
      classDef.addPersonalField(field);
    }
  }

  private void fillInDataDefinition(ExpressionDeserialization defDeserializer, DefinitionProtos.Definition.DataData dataProto, DataDefinition dataDef) throws DeserializationException {
    dataDef.setParameters(defDeserializer.readParameters(dataProto.getParamList()));
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
    if (functionProto.hasBody()) {
      functionDef.setBody(readBody(defDeserializer, functionProto.getBody()));
    }
  }
}
