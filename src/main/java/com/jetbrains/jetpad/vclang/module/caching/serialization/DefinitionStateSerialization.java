package com.jetbrains.jetpad.vclang.module.caching.serialization;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.*;
import com.jetbrains.jetpad.vclang.core.elimtree.*;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.module.caching.LocalizedTypecheckerState;
import com.jetbrains.jetpad.vclang.module.caching.PersistenceProvider;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class DefinitionStateSerialization {
  private final PersistenceProvider<? extends SourceId> myPersistenceProvider;
  private final CalltargetIndexProvider myCalltargetIndexProvider;

  public DefinitionStateSerialization(PersistenceProvider<? extends SourceId> persistenceProvider, CalltargetIndexProvider calltargetIndexProvider) {
    myPersistenceProvider = persistenceProvider;
    myCalltargetIndexProvider = calltargetIndexProvider;
  }

  public ModuleProtos.Module.DefinitionState writeDefinitionState(LocalizedTypecheckerState<? extends SourceId>.LocalTypecheckerState state) {
    ModuleProtos.Module.DefinitionState.Builder builder = ModuleProtos.Module.DefinitionState.newBuilder();
    for (Abstract.Definition definition : state.getTypecheckedDefinitions()) {
      Definition typechecked = state.getTypechecked(definition);
      if (typechecked instanceof Constructor || typechecked instanceof ClassField) continue;
      builder.putDefinition(myPersistenceProvider.getIdFor(definition), writeDefinitionStub(typechecked, state));
    }
    return builder.build();
  }

  private DefinitionProtos.DefinitionStub writeDefinitionStub(Definition definition, LocalizedTypecheckerState<? extends SourceId>.LocalTypecheckerState state) {
    DefinitionProtos.DefinitionStub.Builder out = DefinitionProtos.DefinitionStub.newBuilder();
    if (definition.status() == Definition.TypeCheckingStatus.NO_ERRORS) {
      out.setDefinition(writeDefinition(definition, state));
    }
    return out.build();
  }

  // TODO: HACK. Second parameter should not be needed
  private DefinitionProtos.Definition writeDefinition(Definition definition, LocalizedTypecheckerState<? extends SourceId>.LocalTypecheckerState state) {
    final DefinitionProtos.Definition.Builder out = DefinitionProtos.Definition.newBuilder();

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

    builder.setFieldSet(defSerializer.writeFieldSet(definition.getFieldSet()));
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

  private DefinitionProtos.Definition.DataData.Constructor.Pattern writePattern(DefinitionSerialization defSerializer, Pattern pattern) {
    DefinitionProtos.Definition.DataData.Constructor.Pattern.Builder builder = DefinitionProtos.Definition.DataData.Constructor.Pattern.newBuilder();
    if (pattern instanceof BindingPattern) {
      builder.setBinding(DefinitionProtos.Definition.DataData.Constructor.Pattern.Binding.newBuilder()
        .setVar(defSerializer.writeParameter(((BindingPattern) pattern).getBinding())));
    } else if (pattern instanceof EmptyPattern) {
      builder.setEmpty(DefinitionProtos.Definition.DataData.Constructor.Pattern.Empty.newBuilder());
    } else if (pattern instanceof ConstructorPattern) {
      DefinitionProtos.Definition.DataData.Constructor.Pattern.ConstructorRef.Builder pBuilder = DefinitionProtos.Definition.DataData.Constructor.Pattern.ConstructorRef.newBuilder();
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
        intervalBuilder.addCase(DefinitionProtos.Body.ExpressionPair.newBuilder()
          .setLeft(defSerializer.writeExpr(pair.proj1))
          .setRight(defSerializer.writeExpr(pair.proj2)));
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
