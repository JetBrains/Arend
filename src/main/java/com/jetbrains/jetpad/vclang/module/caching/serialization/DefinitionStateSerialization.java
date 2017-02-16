package com.jetbrains.jetpad.vclang.module.caching.serialization;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelBinding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.*;
import com.jetbrains.jetpad.vclang.module.caching.LocalizedTypecheckerState;
import com.jetbrains.jetpad.vclang.module.caching.PersistenceProvider;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.term.Abstract;

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
    for (LevelBinding polyVar : definition.getPolyParams()) {
      out.addPolyParam(defSerializer.createLevelBinding(polyVar));
    }

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
      fBuilder.setType(defSerializer.writeExpr(field.getBaseType()));
      builder.putFields(myPersistenceProvider.getIdFor(abstractField), fBuilder.build());
    }

    builder.setFieldSet(defSerializer.writeFieldSet(definition.getFieldSet()));

    for (ClassDefinition classDefinition : definition.getSuperClasses()) {
      builder.addSuperClassRef(myCalltargetIndexProvider.getDefIndex(classDefinition));
    }

    return builder.build();
  }

  private DefinitionProtos.Definition.DataData writeDataDefinition(DefinitionSerialization defSerializer, DataDefinition definition) {
    DefinitionProtos.Definition.DataData.Builder builder = DefinitionProtos.Definition.DataData.newBuilder();

    builder.addAllParam(defSerializer.writeParameters(definition.getParameters()));
    builder.setSorts(defSerializer.writeSortMax(definition.getSorts()));

    for (Constructor constructor : definition.getConstructors()) {
      DefinitionProtos.Definition.DataData.Constructor.Builder cBuilder = DefinitionProtos.Definition.DataData.Constructor.newBuilder();
      if (constructor.getPatterns() != null) {
        cBuilder.setPatterns(defSerializer.writePatterns(constructor.getPatterns()));
      }
      cBuilder.addAllParam(defSerializer.writeParameters(constructor.getParameters()));

      builder.putConstructors(myPersistenceProvider.getIdFor(constructor.getAbstractDefinition()), cBuilder.build());
    }

    for (Condition condition : definition.getConditions()) {
      builder.putConditions(myCalltargetIndexProvider.getDefIndex(condition.getConstructor()), defSerializer.writeElimTree(condition.getElimTree()));
    }

    builder.setMatchesOnInterval(definition.matchesOnInterval());

    return builder.build();
  }

  private DefinitionProtos.Definition.FunctionData writeFunctionDefinition(DefinitionSerialization defSerializer, FunctionDefinition definition) {
    DefinitionProtos.Definition.FunctionData.Builder builder = DefinitionProtos.Definition.FunctionData.newBuilder();

    builder.addAllParam(defSerializer.writeParameters(definition.getParameters()));
    builder.setType(defSerializer.writeType(definition.getResultType()));
    if (definition.getElimTree() != null) {
      builder.setElimTree(defSerializer.writeElimTree(definition.getElimTree()));
    }

    return builder.build();
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
