package com.jetbrains.jetpad.vclang.module.caching.serialization;

import com.jetbrains.jetpad.vclang.core.context.LinkList;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.TypedDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.*;
import com.jetbrains.jetpad.vclang.core.elimtree.*;
import com.jetbrains.jetpad.vclang.core.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.module.caching.LocalizedTypecheckerState;
import com.jetbrains.jetpad.vclang.module.caching.PersistenceProvider;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.util.Pair;

import java.util.*;

public class DefinitionStateDeserialization<SourceIdT extends SourceId> {
  private final PersistenceProvider<SourceIdT> myPersistenceProvider;
  private final SourceIdT mySourceId;

  public DefinitionStateDeserialization(SourceIdT sourceId, PersistenceProvider<SourceIdT> persistenceProvider) {
    mySourceId = sourceId;
    myPersistenceProvider = persistenceProvider;
  }

  public void readStubs(ModuleProtos.Module.DefinitionState in, LocalizedTypecheckerState<SourceIdT>.LocalTypecheckerState state) throws DeserializationError {
    for (Map.Entry<String, DefinitionProtos.DefinitionStub> entry : in.getDefinitionMap().entrySet()) {
      String id = entry.getKey();
      DefinitionProtos.DefinitionStub defStubProto = entry.getValue();
      final Definition def;
      final Abstract.Definition abstractDef = getAbstract(id);

      if (defStubProto.hasDefinition()) {
        DefinitionProtos.Definition defProto = defStubProto.getDefinition();
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
            def = new FunctionDefinition(abstractDef);
            break;
          default:
            throw new DeserializationError("Unknown Definition kind: " + defProto.getDefinitionDataCase());
        }
      } else {
        def = Definition.newDefinition(abstractDef);
        if (def == null) {
          throw new DeserializationError("Unknown Definition kind: " + abstractDef);
        }
        def.setStatus(Definition.TypeCheckingStatus.HEADER_NEEDS_TYPE_CHECKING);
      }

      state.record(abstractDef, def);
    }
  }

  public void fillInDefinitions(ModuleProtos.Module.DefinitionState in, LocalizedTypecheckerState<SourceIdT>.LocalTypecheckerState state, CalltargetProvider calltargetProvider) throws DeserializationError {
    CalltargetProvider.Typed typedCalltargetProvider = new CalltargetProvider.Typed(calltargetProvider);

    for (Map.Entry<String, DefinitionProtos.DefinitionStub> entry : in.getDefinitionMap().entrySet()) {
      String id = entry.getKey();
      DefinitionProtos.DefinitionStub defStubProto = entry.getValue();

      if (defStubProto.hasDefinition()) {
        final DefinitionProtos.Definition defProto = defStubProto.getDefinition();
        final Definition def = getTypechecked(state, id);
        final DefinitionDeserialization defDeserializer = new DefinitionDeserialization(typedCalltargetProvider);

        readClassifyingFields(def, typedCalltargetProvider, defProto.getClassifyingFieldList());

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

        def.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
      }
    }
  }

  private void fillInClassDefinition(DefinitionDeserialization defDeserializer, CalltargetProvider.Typed calltargetProvider, DefinitionProtos.Definition.ClassData classProto, ClassDefinition classDef, LocalizedTypecheckerState<SourceIdT>.LocalTypecheckerState state) throws DeserializationError {
    classDef.setFieldSet(defDeserializer.readFieldSet(classProto.getFieldSet()));

    Set<ClassDefinition> superClasses = new HashSet<>();
    for (int superClassRef : classProto.getSuperClassRefList()) {
      ClassDefinition superClass = calltargetProvider.getCalltarget(superClassRef, ClassDefinition.class);
      superClasses.add(superClass);
    }
    classDef.setSuperClasses(superClasses);
    if (classProto.getEnclosingThisFieldRef() != 0) {
      classDef.setEnclosingThisField(calltargetProvider.getCalltarget(classProto.getEnclosingThisFieldRef(), ClassField.class));
    }

    for (Map.Entry<String, DefinitionProtos.Definition.ClassData.Field> entry : classProto.getFieldsMap().entrySet()) {
      DefinitionProtos.Definition.ClassData.Field fieldProto = entry.getValue();
      ClassField field = getTypechecked(state, entry.getKey());
      field.setThisParameter((TypedDependentLink) defDeserializer.readParameter(fieldProto.getThisParam()));
      field.setBaseType(defDeserializer.readExpr(fieldProto.getType()));
      field.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
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
      constructor.setParameters(defDeserializer.readParameters(constructorProto.getParamList()));
      if (constructorProto.hasConditions()) {
        constructor.setBody(readBody(defDeserializer, constructorProto.getConditions()));
      }
      constructor.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
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

  private Body readBody(DefinitionDeserialization defDeserializer, DefinitionProtos.Body proto) throws DeserializationError {
    switch (proto.getKindCase()) {
      case ELIM_TREE:
        return defDeserializer.readElimTree(proto.getElimTree());
      case INTERVAL_ELIM:
        List<Pair<Expression, Expression>> cases = new ArrayList<>(proto.getIntervalElim().getCaseCount());
        for (DefinitionProtos.Body.ExpressionPair pairProto : proto.getIntervalElim().getCaseList()) {
          cases.add(new Pair<>(defDeserializer.readExpr(pairProto.getLeft()), defDeserializer.readExpr(pairProto.getRight())));
        }
        ElimTree elimTree = null;
        if (proto.getIntervalElim().hasOtherwise()) {
          elimTree = defDeserializer.readElimTree(proto.getIntervalElim().getOtherwise());
        }
        return new IntervalElim(defDeserializer.readParameters(proto.getIntervalElim().getParamList()), cases, elimTree);
      default:
        throw new DeserializationError("Unknown body kind: " + proto.getKindCase());
    }
  }

  private Patterns readPatterns(DefinitionDeserialization defDeserializer, CalltargetProvider.Typed calltargetProvider, List<DefinitionProtos.Definition.DataData.Constructor.Pattern> protos, LinkList list) throws DeserializationError {
    List<Pattern> patterns = new ArrayList<>(protos.size());
    for (DefinitionProtos.Definition.DataData.Constructor.Pattern proto : protos) {
      patterns.add(readPattern(defDeserializer, calltargetProvider, proto, list));
    }
    return new Patterns(patterns);
  }

  private Pattern readPattern(DefinitionDeserialization defDeserializer, CalltargetProvider.Typed calltargetProvider, DefinitionProtos.Definition.DataData.Constructor.Pattern proto, LinkList list) throws DeserializationError {
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
    functionDef.setResultType(defDeserializer.readExpr(functionProto.getType()));
    if (functionProto.hasBody()) {
      functionDef.setBody(readBody(defDeserializer, functionProto.getBody()));
    }
  }

  private void readClassifyingFields(Definition definition, CalltargetProvider.Typed calltargetProvider, List<DefinitionProtos.Definition.ClassifyingFields> classifyingFields) throws DeserializationError {
    int index = 0;
    for (DefinitionProtos.Definition.ClassifyingFields ref : classifyingFields) {
      if (ref.getFieldRefCount() > 0) {
        definition.setClassifyingFieldOfParameter(index, calltargetProvider.getCalltarget(ref.getFieldRef(0), ClassField.class));
      }
      index++;
    }
  }


  private Abstract.Definition getAbstract(String id) {
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
