package com.jetbrains.jetpad.vclang.module.caching.serialization;

import com.jetbrains.jetpad.vclang.module.caching.LocalizedTypecheckerState;
import com.jetbrains.jetpad.vclang.module.caching.PersistenceProvider;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.term.definition.*;

import java.util.*;

public class DefinitionStateDeserialization<SourceIdT extends SourceId> {
  private final PersistenceProvider<SourceIdT> myPersistenceProvider;
  private final SourceIdT mySourceId;

  public DefinitionStateDeserialization(SourceIdT sourceId, PersistenceProvider<SourceIdT> persistenceProvider) {
    mySourceId = sourceId;
    myPersistenceProvider = persistenceProvider;
  }

  public void readStubs(ModuleProtos.Module.DefinitionState in, LocalizedTypecheckerState<SourceIdT>.LocalTypecheckerState state) {
    for (Map.Entry<String, DefinitionProtos.Definition> entry : in.getDefinitionMap().entrySet()) {
      String id = entry.getKey();
      DefinitionProtos.Definition defProto = entry.getValue();
      final Definition def;
      final Abstract.Definition abstractDef = getAbstract(id);
      switch (defProto.getDefinitionDataCase()) {
        case CLASS:
          ClassDefinition classDef = new ClassDefinition((Abstract.ClassDefinition) abstractDef);
          for (String constructorId : defProto.getClass_().getFieldsMap().keySet()) {
            Abstract.ClassField absField = (Abstract.ClassField) getAbstract(constructorId);
            state.record(absField, new ClassField(absField, classDef));
          }
          def = classDef;
          break;
        case DATA:
          DataDefinition dataDef = new DataDefinition((Abstract.DataDefinition) abstractDef);
          for (String constructorId : defProto.getData().getConstructorsMap().keySet()) {
            Abstract.Constructor absConstructor = (Abstract.Constructor) getAbstract(constructorId);
            state.record(absConstructor, new Constructor(absConstructor, dataDef));
          }
          def = dataDef;
          break;
        case FUNCTION:
          def = new FunctionDefinition(getAbstract(id));
          break;
        default:
          throw new IllegalStateException();  // TODO[serial]: report properly
      }
      state.record(abstractDef, def);
    }
  }

  public void fillInDefinitions(ModuleProtos.Module.DefinitionState in, LocalizedTypecheckerState<SourceIdT>.LocalTypecheckerState state, CalltargetProvider calltargetProvider) {
    for (Map.Entry<String, DefinitionProtos.Definition> entry : in.getDefinitionMap().entrySet()) {
      String id = entry.getKey();
      DefinitionProtos.Definition defProto = entry.getValue();

      final Definition def = getTypechecked(state, id);

      final DefinitionDeserialization defDeserializer = new DefinitionDeserialization(calltargetProvider);

      List<TypedBinding> polyParams = new ArrayList<>();
      for (ExpressionProtos.Binding.TypedBinding typedBinding : defProto.getPolyParamList()) {
        polyParams.add(defDeserializer.readTypedBinding(typedBinding));
      }

      def.setPolyParams(polyParams);

      switch (defProto.getDefinitionDataCase()) {
        case CLASS:
          ClassDefinition classDef = (ClassDefinition) def;
          fillInClassDefinition(defDeserializer, calltargetProvider, defProto.getClass_(), classDef, state);
          break;
        case DATA:
          DataDefinition dataDef = (DataDefinition) def;
          dataDef.typeHasErrors(defProto.getTypeHasErrors());
          fillInDataDefinition(defDeserializer, calltargetProvider, defProto.getData(), dataDef, state);
          break;
        case FUNCTION:
          FunctionDefinition functionDef = (FunctionDefinition) def;
          functionDef.typeHasErrors(defProto.getTypeHasErrors());
          fillInFunctionDefinition(defDeserializer, defProto.getFunction(), functionDef);
          break;
        default:
          throw new IllegalStateException();  // TODO[serial]: report properly
      }

      def.hasErrors(defProto.getHasErrors() ? Definition.TypeCheckingStatus.HAS_ERRORS : Definition.TypeCheckingStatus.NO_ERRORS);

      if (defProto.getThisClassRef() != 0) {
        ClassDefinition thisClass = calltargetProvider.getCalltarget(defProto.getThisClassRef());
        def.setThisClass(thisClass);
      }
    }
  }

  private void fillInClassDefinition(DefinitionDeserialization defDeserializer, CalltargetProvider calltargetProvider, DefinitionProtos.Definition.ClassData classProto, ClassDefinition classDef, LocalizedTypecheckerState<SourceIdT>.LocalTypecheckerState state) {
    classDef.setFieldSet(defDeserializer.readFieldSet(classProto.getFieldSet()));

    Set<ClassDefinition> superClasses = new HashSet<>();
    for (int superClassRef : classProto.getSuperClassRefList()) {
      ClassDefinition superClass = calltargetProvider.getCalltarget(superClassRef);
      superClasses.add(superClass);
    }
    classDef.setSuperClasses(superClasses);

    for (Map.Entry<String, DefinitionProtos.Definition.ClassData.Field> entry : classProto.getFieldsMap().entrySet()) {
      DefinitionProtos.Definition.ClassData.Field fieldProto = entry.getValue();
      ClassField field = getTypechecked(state, entry.getKey());
      field.setThisParameter(defDeserializer.readParameter(fieldProto.getThisParam()));
      field.setBaseType(defDeserializer.readExpr(fieldProto.getType()));
    }
  }

  private void fillInDataDefinition(DefinitionDeserialization defDeserializer, CalltargetProvider calltargetProvider, DefinitionProtos.Definition.DataData dataProto, DataDefinition dataDef, LocalizedTypecheckerState<SourceIdT>.LocalTypecheckerState state) {
    dataDef.setParameters(defDeserializer.readParameters(dataProto.getParamList()));

    for (Map.Entry<String, DefinitionProtos.Definition.DataData.Constructor> entry : dataProto.getConstructorsMap().entrySet()) {
      DefinitionProtos.Definition.DataData.Constructor constructorProto = entry.getValue();
      Constructor constructor = getTypechecked(state, entry.getKey());
      if (constructorProto.hasPatterns()) {
        constructor.setPatterns(defDeserializer.readPatterns(constructorProto.getPatterns()));
      }
      constructor.setParameters(defDeserializer.readParameters(constructorProto.getParamList()));
      constructor.typeHasErrors(constructorProto.getTypeHasErrors());
      constructor.hasErrors(constructorProto.getHasErrors() ? Definition.TypeCheckingStatus.HAS_ERRORS : Definition.TypeCheckingStatus.NO_ERRORS);
      dataDef.addConstructor(constructor);
    }

    for (Map.Entry<Integer, ExpressionProtos.ElimTreeNode> entry : dataProto.getConditionsMap().entrySet()) {
      Constructor targetConstructor = calltargetProvider.getCalltarget(entry.getKey());
      dataDef.addCondition(new Condition(targetConstructor, defDeserializer.readElimTree(entry.getValue())));
    }

    if (dataProto.getMatchesOnInterval()) {
      dataDef.setMatchesOnInterval();
    }
  }

  private void fillInFunctionDefinition(DefinitionDeserialization defDeserializer, DefinitionProtos.Definition.FunctionData functionProto, FunctionDefinition functionDef) {
    functionDef.setParameters(defDeserializer.readParameters(functionProto.getParamList()));
    functionDef.setResultType(defDeserializer.readTypeMax(functionProto.getType()));
    if (functionProto.hasElimTree()) {
      functionDef.setElimTree(defDeserializer.readElimTree(functionProto.getElimTree()));
    }
  }


  private Abstract.Definition getAbstract(String id) {
    return myPersistenceProvider.getFromId(mySourceId, id);
  }

  private <DefinitionT extends Definition> DefinitionT getTypechecked(LocalizedTypecheckerState<SourceIdT>.LocalTypecheckerState state, String id) {
    try {
      //noinspection unchecked
      return (DefinitionT) state.getTypechecked(getAbstract(id));
    } catch (ClassCastException ignored) {
      throw new IllegalStateException();  // TODO[serial]: report properly
    }
  }
}
