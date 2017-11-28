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
import com.jetbrains.jetpad.vclang.module.caching.LocalizedTypecheckerState;
import com.jetbrains.jetpad.vclang.module.caching.PersistenceProvider;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.util.Pair;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DefinitionStateDeserialization<SourceIdT extends SourceId> {
  private final PersistenceProvider<SourceIdT> myPersistenceProvider;
  private final SourceIdT mySourceId;

  public DefinitionStateDeserialization(SourceIdT sourceId, PersistenceProvider<SourceIdT> persistenceProvider) {
    mySourceId = sourceId;
    myPersistenceProvider = persistenceProvider;
  }

  public void readStubs(ModuleProtos.Module.DefinitionState in, LocalizedTypecheckerState<SourceIdT>.LocalTypecheckerState state) throws DeserializationError {
    for (Map.Entry<String, DefinitionProtos.Definition> entry : in.getDefinitionMap().entrySet()) {
      String id = entry.getKey();
      DefinitionProtos.Definition defProto = entry.getValue();

      myPersistenceProvider.registerCachedDefinition(mySourceId, id, null);
      final GlobalReferable abstractDef = getAbstract(id);
      final Definition def;
      switch (defProto.getDefinitionDataCase()) {
        case CLASS:
          ClassDefinition classDef = new ClassDefinition(abstractDef);
          for (DefinitionProtos.Definition.ClassData.Field fieldProto : defProto.getClass_().getPersonalFieldList()) {
            myPersistenceProvider.registerCachedDefinition(mySourceId, fieldProto.getName(), abstractDef);
            GlobalReferable absField = getAbstract(fieldProto.getName());
            ClassField res = new ClassField(absField, classDef);
            res.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
            state.record(absField, res);
          }
          def = classDef;
          break;
        case DATA:
          DataDefinition dataDef = new DataDefinition(abstractDef);
          for (String constructorId : defProto.getData().getConstructorsMap().keySet()) {
            myPersistenceProvider.registerCachedDefinition(mySourceId, constructorId, abstractDef);
            GlobalReferable absConstructor = getAbstract(constructorId);
            Constructor res = new Constructor(absConstructor, dataDef);
            res.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
            state.record(absConstructor, res);
          }
          def = dataDef;
          break;
        case FUNCTION:
          def = new FunctionDefinition(getAbstract(id));
          break;
        default:
          throw new DeserializationError("Unknown Definition kind: " + defProto.getDefinitionDataCase());
      }

      def.setStatus(readTcStatus(defProto));
      state.record(abstractDef, def);
    }
  }

  private @Nonnull Definition.TypeCheckingStatus readTcStatus(DefinitionProtos.Definition defProto) {
    switch (defProto.getStatus()) {
      case HEADER_HAS_ERRORS:
        return Definition.TypeCheckingStatus.HEADER_HAS_ERRORS;
      case BODY_HAS_ERRORS:
        return Definition.TypeCheckingStatus.BODY_HAS_ERRORS;
      case HEADER_NEEDS_TYPE_CHECKING:
        return Definition.TypeCheckingStatus.HEADER_NEEDS_TYPE_CHECKING;
      case BODY_NEEDS_TYPE_CHECKING:
        return Definition.TypeCheckingStatus.BODY_NEEDS_TYPE_CHECKING;
      case HAS_ERRORS:
        return Definition.TypeCheckingStatus.HAS_ERRORS;
      case NO_ERRORS:
        return Definition.TypeCheckingStatus.NO_ERRORS;
      default:
        throw new IllegalStateException("Unknown typechecking state");
    }
  }

  public void fillInDefinitions(ModuleProtos.Module.DefinitionState in, LocalizedTypecheckerState<SourceIdT>.LocalTypecheckerState state, CalltargetProvider calltargetProvider) throws DeserializationError {
    CalltargetProvider.Typed typedCalltargetProvider = new CalltargetProvider.Typed(calltargetProvider);

    for (Map.Entry<String, DefinitionProtos.Definition> entry : in.getDefinitionMap().entrySet()) {
      String id = entry.getKey();
      DefinitionProtos.Definition defProto = entry.getValue();

      final Definition def = getTypechecked(state, id);

      final DefinitionDeserialization defDeserializer = new DefinitionDeserialization(typedCalltargetProvider);

      if (defProto.getThisClassRef() != 0) {
        def.setThisClass(typedCalltargetProvider.getCalltarget(defProto.getThisClassRef(), ClassDefinition.class));
      }

      switch (defProto.getDefinitionDataCase()) {
        case CLASS:
          ClassDefinition classDef = (ClassDefinition) def;
          fillInClassDefinition(defDeserializer, typedCalltargetProvider, defProto.getClass_(), classDef, state);
          break;
        case DATA:
          DataDefinition dataDef = (DataDefinition) def;
          fillInDataDefinition(defDeserializer, typedCalltargetProvider, defProto.getData(), dataDef, state);
          break;
        case FUNCTION:
          FunctionDefinition functionDef = (FunctionDefinition) def;
          fillInFunctionDefinition(defDeserializer, defProto.getFunction(), functionDef);
          break;
        default:
          throw new DeserializationError("Unknown Definition kind: " + defProto.getDefinitionDataCase());
      }
    }
  }

  private void fillInClassDefinition(DefinitionDeserialization defDeserializer, CalltargetProvider.Typed calltargetProvider, DefinitionProtos.Definition.ClassData classProto, ClassDefinition classDef, LocalizedTypecheckerState<SourceIdT>.LocalTypecheckerState state) throws DeserializationError {
    for (int classFieldRef : classProto.getFieldRefList()) {
      classDef.addField(calltargetProvider.getCalltarget(classFieldRef, ClassField.class));
    }
    for (Map.Entry<Integer, DefinitionProtos.Definition.ClassData.Implementation> entry : classProto.getImplementationsMap().entrySet()) {
      TypedDependentLink thisParam = (TypedDependentLink) defDeserializer.readParameter(entry.getValue().getThisParam());
      ClassDefinition.Implementation impl = new ClassDefinition.Implementation(thisParam, defDeserializer.readExpr(entry.getValue().getTerm()));
      classDef.implementField(calltargetProvider.getCalltarget(entry.getKey(), ClassField.class), impl);
    }
    classDef.setSort(defDeserializer.readSort(classProto.getSort()));

    for (int superClassRef : classProto.getSuperClassRefList()) {
      ClassDefinition superClass = calltargetProvider.getCalltarget(superClassRef, ClassDefinition.class);
      classDef.addSuperClass(superClass);
    }
    if (classProto.getEnclosingThisFieldRef() != 0) {
      classDef.setEnclosingThisField(calltargetProvider.getCalltarget(classProto.getEnclosingThisFieldRef(), ClassField.class));
    }

    for (DefinitionProtos.Definition.ClassData.Field fieldProto : classProto.getPersonalFieldList()) {
      ClassField field = getTypechecked(state, fieldProto.getName());
      field.setThisParameter((TypedDependentLink) defDeserializer.readParameter(fieldProto.getThisParam()));
      if (fieldProto.hasType()) {
        field.setBaseType(defDeserializer.readExpr(fieldProto.getType()));
      }
      classDef.addPersonalField(field);
    }
  }

  private void fillInDataDefinition(DefinitionDeserialization defDeserializer, CalltargetProvider.Typed calltargetProvider, DefinitionProtos.Definition.DataData dataProto, DataDefinition dataDef, LocalizedTypecheckerState<SourceIdT>.LocalTypecheckerState state) throws DeserializationError {
    dataDef.setParameters(defDeserializer.readParameters(dataProto.getParamList()));
    dataDef.setSort(defDeserializer.readSort(dataProto.getSort()));

    for (Map.Entry<String, DefinitionProtos.Definition.DataData.Constructor> entry : dataProto.getConstructorsMap().entrySet()) {
      DefinitionProtos.Definition.DataData.Constructor constructorProto = entry.getValue();
      Constructor constructor = getTypechecked(state, entry.getKey());
      if (constructorProto.getPatternCount() > 0) {
        constructor.setPatterns(readPatterns(defDeserializer, calltargetProvider, constructorProto.getPatternList(), new LinkList()));
      }
      if (constructorProto.getClauseCount() > 0) {
        List<ClauseBase> clauses = new ArrayList<>(constructorProto.getClauseCount());
        for (DefinitionProtos.Definition.Clause clause : constructorProto.getClauseList()) {
          clauses.add(readClause(defDeserializer, calltargetProvider, clause));
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

  private ClauseBase readClause(DefinitionDeserialization defDeserializer, CalltargetProvider.Typed calltargetProvider, DefinitionProtos.Definition.Clause clause) throws DeserializationError {
    return new ClauseBase(readPatterns(defDeserializer, calltargetProvider, clause.getPatternList(), new LinkList()).getPatternList(), defDeserializer.readExpr(clause.getExpression()));
  }

  private Body readBody(DefinitionDeserialization defDeserializer, DefinitionProtos.Body proto) throws DeserializationError {
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
        throw new DeserializationError("Unknown body kind: " + proto.getKindCase());
    }
  }

  private Patterns readPatterns(DefinitionDeserialization defDeserializer, CalltargetProvider.Typed calltargetProvider, List<DefinitionProtos.Definition.Pattern> protos, LinkList list) throws DeserializationError {
    List<Pattern> patterns = new ArrayList<>(protos.size());
    for (DefinitionProtos.Definition.Pattern proto : protos) {
      patterns.add(readPattern(defDeserializer, calltargetProvider, proto, list));
    }
    return new Patterns(patterns);
  }

  private Pattern readPattern(DefinitionDeserialization defDeserializer, CalltargetProvider.Typed calltargetProvider, DefinitionProtos.Definition.Pattern proto, LinkList list) throws DeserializationError {
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
            calltargetProvider.getCalltarget(proto.getConstructor().getConstructorRef(), Constructor.class),
            defDeserializer.readSort(proto.getConstructor().getSortArgument()),
            defDeserializer.readExprList(proto.getConstructor().getDataTypeArgumentList()),
            Collections.emptyList()
          ), readPatterns(defDeserializer, calltargetProvider, proto.getConstructor().getPatternList(), list));
      default:
        throw new DeserializationError("Unknown Pattern kind: " + proto.getKindCase());
    }
  }

  private void fillInFunctionDefinition(DefinitionDeserialization defDeserializer, DefinitionProtos.Definition.FunctionData functionProto, FunctionDefinition functionDef) throws DeserializationError {
    functionDef.setParameters(defDeserializer.readParameters(functionProto.getParamList()));
    if (functionProto.hasType()) {
      functionDef.setResultType(defDeserializer.readExpr(functionProto.getType()));
    }
    if (functionProto.hasBody()) {
      functionDef.setBody(readBody(defDeserializer, functionProto.getBody()));
    }
  }


  private GlobalReferable getAbstract(String id) {
    return myPersistenceProvider.getFromId(mySourceId, id);
  }

  private <DefinitionT extends Definition> DefinitionT getTypechecked(LocalizedTypecheckerState<SourceIdT>.LocalTypecheckerState state, String id) throws DeserializationError {
    try {
      //noinspection unchecked
      return (DefinitionT) state.getTypechecked(getAbstract(id));
    } catch (ClassCastException ignored) {
      throw new DeserializationError("Stored Definition data does not match its kind");
    }
  }
}
