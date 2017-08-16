package com.jetbrains.jetpad.vclang.module.caching.serialization;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.*;
import com.jetbrains.jetpad.vclang.core.elimtree.Body;
import com.jetbrains.jetpad.vclang.core.elimtree.ClauseBase;
import com.jetbrains.jetpad.vclang.core.elimtree.ElimTree;
import com.jetbrains.jetpad.vclang.core.elimtree.IntervalElim;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.pattern.BindingPattern;
import com.jetbrains.jetpad.vclang.core.pattern.ConstructorPattern;
import com.jetbrains.jetpad.vclang.core.pattern.EmptyPattern;
import com.jetbrains.jetpad.vclang.core.pattern.Pattern;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.module.caching.LocalizedTypecheckerState;
import com.jetbrains.jetpad.vclang.module.caching.PersistenceProvider;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DefinitionStateSerialization {
  private final PersistenceProvider<? extends SourceId> myPersistenceProvider;
  private final CalltargetIndexProvider myCalltargetIndexProvider;

  public DefinitionStateSerialization(PersistenceProvider<? extends SourceId> persistenceProvider, CalltargetIndexProvider calltargetIndexProvider) {
    myPersistenceProvider = persistenceProvider;
    myCalltargetIndexProvider = calltargetIndexProvider;
  }

  public ModuleProtos.Module.DefinitionState writeDefinitionState(LocalizedTypecheckerState<? extends SourceId>.LocalTypecheckerState state) {
    ModuleProtos.Module.DefinitionState.Builder builder = ModuleProtos.Module.DefinitionState.newBuilder();
    for (Abstract.GlobalReferableSourceNode definition : state.getTypecheckedDefinitions()) {
      Definition typechecked = state.getTypechecked(definition);
      if (typechecked instanceof Constructor || typechecked instanceof ClassField) continue;

      if (canBeReferred(typechecked)) {
        builder.putDefinition(myPersistenceProvider.getIdFor(definition), writeDefinition(typechecked, state));
      }
    }
    return builder.build();
  }

  private boolean canBeReferred(Definition typechecked) {
    return typechecked.status().headerIsOK();
  }

  // TODO: HACK. Second parameter should not be needed
  private DefinitionProtos.Definition writeDefinition(Definition definition, LocalizedTypecheckerState<? extends SourceId>.LocalTypecheckerState state) {
    final DefinitionProtos.Definition.Builder out = DefinitionProtos.Definition.newBuilder();

    switch (definition.status()) {
      case HEADER_HAS_ERRORS:
        out.setStatus(DefinitionProtos.Definition.Status.HEADER_HAS_ERRORS);
        break;
      case BODY_HAS_ERRORS:
        out.setStatus(DefinitionProtos.Definition.Status.BODY_HAS_ERRORS);
        break;
      case HEADER_NEEDS_TYPE_CHECKING:
        out.setStatus(DefinitionProtos.Definition.Status.HEADER_NEEDS_TYPE_CHECKING);
        break;
      case BODY_NEEDS_TYPE_CHECKING:
        out.setStatus(DefinitionProtos.Definition.Status.BODY_NEEDS_TYPE_CHECKING);
        break;
      case HAS_ERRORS:
        out.setStatus(DefinitionProtos.Definition.Status.HAS_ERRORS);
        break;
      case NO_ERRORS:
        out.setStatus(DefinitionProtos.Definition.Status.NO_ERRORS);
        break;
      default:
        throw new IllegalStateException("Unknown typechecking status");
    }

    if (definition.getThisClass() != null) {
      out.setThisClassRef(myCalltargetIndexProvider.getDefIndex(definition.getThisClass()));
    }

    final DefinitionSerialization defSerializer = new DefinitionSerialization(myCalltargetIndexProvider);

    out.addAllClassifyingField(writeClassifyingFields(definition));

    if (definition instanceof ClassDefinition) {
      // type cannot possibly have errors
      out.setClass_(writeClassDefinition(defSerializer, (ClassDefinition) definition, state));
    } else if (definition instanceof DataDefinition) {
      out.setData(writeDataDefinition(defSerializer, (DataDefinition) definition));
    } else if (definition instanceof FunctionDefinition) {
      out.setFunction(writeFunctionDefinition(defSerializer, (FunctionDefinition) definition));
    } else {
      throw new IllegalStateException();
    }

    return out.build();
  }

  // TODO: HACK. State should not be needed as class fields are not individually typecheckable
  private DefinitionProtos.Definition.ClassData writeClassDefinition(DefinitionSerialization defSerializer, ClassDefinition definition, LocalizedTypecheckerState<? extends SourceId>.LocalTypecheckerState state) {
    DefinitionProtos.Definition.ClassData.Builder builder = DefinitionProtos.Definition.ClassData.newBuilder();

    for (Abstract.ClassField abstractField : definition.getAbstractDefinition().getFields()) {
      ClassField field = (ClassField) state.getTypechecked(abstractField);
      DefinitionProtos.Definition.ClassData.Field.Builder fBuilder = DefinitionProtos.Definition.ClassData.Field.newBuilder();
      fBuilder.setThisParam(defSerializer.writeParameter(field.getThisParameter()));
      fBuilder.setType(defSerializer.writeExpr(field.getBaseType(Sort.STD)));
      builder.putFields(myPersistenceProvider.getIdFor(abstractField), fBuilder.build());
    }

    for (ClassField classField : definition.getFields()) {
      builder.addClassFieldRef(myCalltargetIndexProvider.getDefIndex(classField));
    }
    for (Map.Entry<ClassField, ClassDefinition.Implementation> impl : definition.getImplemented()) {
      DefinitionProtos.Definition.ClassData.Implementation.Builder iBuilder = DefinitionProtos.Definition.ClassData.Implementation.newBuilder();
      iBuilder.setThisParam(defSerializer.writeParameter(impl.getValue().thisParam));
      iBuilder.setTerm(defSerializer.writeExpr(impl.getValue().term));
      builder.putImplementations(myCalltargetIndexProvider.getDefIndex(impl.getKey()), iBuilder.build());
    }
    builder.setSort(defSerializer.writeSort(definition.getSort()));
    if (definition.getEnclosingThisField() != null) {
      builder.setEnclosingThisFieldRef(myCalltargetIndexProvider.getDefIndex(definition.getEnclosingThisField()));
    }

    for (ClassDefinition classDefinition : definition.getSuperClasses()) {
      builder.addSuperClassRef(myCalltargetIndexProvider.getDefIndex(classDefinition));
    }

    return builder.build();
  }

  private DefinitionProtos.Definition.DataData writeDataDefinition(DefinitionSerialization defSerializer, DataDefinition definition) {
    DefinitionProtos.Definition.DataData.Builder builder = DefinitionProtos.Definition.DataData.newBuilder();

    builder.addAllParam(defSerializer.writeParameters(definition.getParameters()));
    builder.setSort(defSerializer.writeSort(definition.getSort()));

    for (Constructor constructor : definition.getConstructors()) {
      DefinitionProtos.Definition.DataData.Constructor.Builder cBuilder = DefinitionProtos.Definition.DataData.Constructor.newBuilder();
      if (constructor.getPatterns() != null) {
        for (Pattern pattern : constructor.getPatterns().getPatternList()) {
          cBuilder.addPattern(writePattern(defSerializer, pattern));
        }
      }
      for (ClauseBase clause : constructor.getClauses()) {
        cBuilder.addClause(writeClause(defSerializer, clause));
      }
      cBuilder.addAllParam(defSerializer.writeParameters(constructor.getParameters()));
      if (constructor.getBody() != null) {
        cBuilder.setConditions(writeBody(defSerializer, constructor.getBody()));
      }

      builder.putConstructors(myPersistenceProvider.getIdFor(constructor.getAbstractDefinition()), cBuilder.build());
    }

    builder.setMatchesOnInterval(definition.matchesOnInterval());
    int i = 0;
    for (DependentLink link = definition.getParameters(); link.hasNext(); link = link.getNext()) {
      builder.addCovariantParameter(definition.isCovariant(i++));
    }

    return builder.build();
  }

  private DefinitionProtos.Definition.Clause writeClause(DefinitionSerialization defSerializer, ClauseBase clause) {
    DefinitionProtos.Definition.Clause.Builder builder = DefinitionProtos.Definition.Clause.newBuilder();
    for (Pattern pattern : clause.patterns) {
      builder.addPattern(writePattern(defSerializer, pattern));
    }
    builder.setExpression(defSerializer.writeExpr(clause.expression));
    return builder.build();
  }

  private DefinitionProtos.Definition.Pattern writePattern(DefinitionSerialization defSerializer, Pattern pattern) {
    DefinitionProtos.Definition.Pattern.Builder builder = DefinitionProtos.Definition.Pattern.newBuilder();
    if (pattern instanceof BindingPattern) {
      builder.setBinding(DefinitionProtos.Definition.Pattern.Binding.newBuilder()
        .setVar(defSerializer.writeParameter(((BindingPattern) pattern).getBinding())));
    } else if (pattern instanceof EmptyPattern) {
      builder.setEmpty(DefinitionProtos.Definition.Pattern.Empty.newBuilder());
    } else if (pattern instanceof ConstructorPattern) {
      DefinitionProtos.Definition.Pattern.ConstructorRef.Builder pBuilder = DefinitionProtos.Definition.Pattern.ConstructorRef.newBuilder();
      pBuilder.setConstructorRef(myCalltargetIndexProvider.getDefIndex(((ConstructorPattern) pattern).getConstructor()));
      for (Pattern patternArgument : ((ConstructorPattern) pattern).getArguments()) {
        pBuilder.addPattern(writePattern(defSerializer, patternArgument));
      }
      builder.setConstructor(pBuilder.build());
    } else {
      throw new IllegalArgumentException();
    }
    return builder.build();
  }

  private DefinitionProtos.Definition.FunctionData writeFunctionDefinition(DefinitionSerialization defSerializer, FunctionDefinition definition) {
    DefinitionProtos.Definition.FunctionData.Builder builder = DefinitionProtos.Definition.FunctionData.newBuilder();

    builder.addAllParam(defSerializer.writeParameters(definition.getParameters()));
    builder.setType(defSerializer.writeExpr(definition.getResultType()));
    if (definition.getBody() != null) {
      builder.setBody(writeBody(defSerializer, definition.getBody()));
    }

    return builder.build();
  }

  private DefinitionProtos.Body writeBody(DefinitionSerialization defSerializer, Body body) {
    DefinitionProtos.Body.Builder bodyBuilder = DefinitionProtos.Body.newBuilder();
    if (body instanceof IntervalElim) {
      IntervalElim intervalElim = (IntervalElim) body;
      DefinitionProtos.Body.IntervalElim.Builder intervalBuilder = DefinitionProtos.Body.IntervalElim.newBuilder();
      intervalBuilder.addAllParam(defSerializer.writeParameters(intervalElim.getParameters()));
      for (Pair<Expression, Expression> pair : intervalElim.getCases()) {
        DefinitionProtos.Body.ExpressionPair.Builder pairBuilder = DefinitionProtos.Body.ExpressionPair.newBuilder();
        if (pair.proj1 != null) {
          pairBuilder.setLeft(defSerializer.writeExpr(pair.proj1));
        }
        if (pair.proj2 != null) {
          pairBuilder.setRight(defSerializer.writeExpr(pair.proj2));
        }
        intervalBuilder.addCase(pairBuilder);
      }
      if (intervalElim.getOtherwise() != null) {
        intervalBuilder.setOtherwise(defSerializer.writeElimTree(intervalElim.getOtherwise()));
      }
      bodyBuilder.setIntervalElim(intervalBuilder);
    } else if (body instanceof ElimTree) {
      bodyBuilder.setElimTree(defSerializer.writeElimTree((ElimTree) body));
    } else {
      throw new IllegalStateException();
    }
    return bodyBuilder.build();
  }

  private List<DefinitionProtos.Definition.ClassifyingFields> writeClassifyingFields(Definition definition) {
    List<DefinitionProtos.Definition.ClassifyingFields> refs = new ArrayList<>();
    int index = 0;
    for (DependentLink link = definition.getParameters(); link.hasNext(); link = link.getNext()) {
      DefinitionProtos.Definition.ClassifyingFields.Builder refBuilder = DefinitionProtos.Definition.ClassifyingFields.newBuilder();
      ClassField field = definition.getClassifyingFieldOfParameter(index++);
      if (field != null) {
        refBuilder.addFieldRef(myCalltargetIndexProvider.getDefIndex(field));
      }
      refs.add(refBuilder.build());
    }
    return refs;
  }
}
