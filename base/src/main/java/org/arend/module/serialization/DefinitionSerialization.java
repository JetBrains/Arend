package org.arend.module.serialization;

import com.google.protobuf.ByteString;
import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.binding.ParamLevelVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.*;
import org.arend.core.elimtree.Body;
import org.arend.core.elimtree.ElimBody;
import org.arend.core.elimtree.IntervalElim;
import org.arend.core.expr.AbsExpression;
import org.arend.core.expr.Expression;
import org.arend.core.expr.PiExpression;
import org.arend.core.pattern.BindingPattern;
import org.arend.core.pattern.ConstructorExpressionPattern;
import org.arend.core.pattern.EmptyPattern;
import org.arend.core.pattern.ExpressionPattern;
import org.arend.core.subst.Levels;
import org.arend.ext.core.definition.CoreDefinition;
import org.arend.ext.reference.Precedence;
import org.arend.ext.serialization.ArendSerializer;
import org.arend.ext.userData.Key;
import org.arend.ext.serialization.SerializableKey;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.MetaReferable;
import org.arend.naming.reference.TCDefReferable;
import org.arend.naming.reference.TCReferable;
import org.arend.typechecking.order.dependency.DependencyListener;
import org.arend.ext.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class DefinitionSerialization implements ArendSerializer {
  private final CallTargetIndexProvider myCallTargetIndexProvider;
  private final DependencyListener myDependencyListener;

  public DefinitionSerialization(CallTargetIndexProvider callTargetIndexProvider, DependencyListener dependencyListener) {
    myCallTargetIndexProvider = callTargetIndexProvider;
    myDependencyListener = dependencyListener;
  }

  DefinitionProtos.Definition writeDefinition(Definition definition) {
    final ExpressionSerialization defSerializer = new ExpressionSerialization(myCallTargetIndexProvider);

    final DefinitionProtos.Definition.Builder out = DefinitionProtos.Definition.newBuilder();
    out.setUniverseKind(defSerializer.writeUniverseKind(definition.getUniverseKind()));
    out.putAllUserData(writeUserData(definition));
    if (definition.getPLevelsParent() != null) {
      out.setPLevelsParent(myCallTargetIndexProvider.getDefIndex(definition.getPLevelsParent()) + 1);
    }
    if (definition.getHLevelsParent() != null) {
      out.setHLevelsParent(myCallTargetIndexProvider.getDefIndex(definition.getHLevelsParent()) + 1);
    }
    out.setPLevelsDerived(definition.arePLevelsDerived());
    out.setHLevelsDerived(definition.areHLevelsDerived());
    out.setIsStdLevels(definition.getLevelParameters() == null);
    if (definition.getLevelParameters() != null) {
      out.addAllLevelParam(writeLevelParameters(definition.getLevelParameters()));
    }
    for (Pair<TCDefReferable, Integer> pair : definition.getParametersOriginalDefinitions()) {
      out.addParameterOriginalDef(writeParameterOriginalDef(pair));
    }
    for (FunctionDefinition axiom : definition.getAxioms()) {
      out.addAxiom(myCallTargetIndexProvider.getDefIndex(axiom));
    }

    for (TCReferable dependency : myDependencyListener.getDependencies(definition.getRef())) {
      if (dependency instanceof MetaReferable) {
        out.addMetaRef(myCallTargetIndexProvider.getDefIndex(dependency));
      }
    }

    if (definition instanceof ClassDefinition) {
      // type cannot possibly have errors
      out.setClass_(writeClassDefinition(defSerializer, (ClassDefinition) definition));
    } else if (definition instanceof DataDefinition) {
      out.setData(writeDataDefinition(defSerializer, (DataDefinition) definition));
    } else if (definition instanceof DConstructor) {
      out.setConstructor(writeDConstructor(defSerializer, (DConstructor) definition));
    } else if (definition instanceof FunctionDefinition) {
      out.setFunction(writeFunctionDefinition(defSerializer, (FunctionDefinition) definition));
    } else {
      throw new IllegalStateException();
    }

    return out.build();
  }

  private Map<String, ByteString> writeUserData(Definition definition) {
    Map<String, ByteString> result = new HashMap<>();
    for (Map.Entry<Key<?>, Object> entry : definition.getUserDataMap().entrySet()) {
      if (entry.getKey() instanceof SerializableKey) {
        //noinspection unchecked
        SerializableKey<Object> key = (SerializableKey<Object>) entry.getKey();
        result.put(key.getName(), ByteString.copyFrom(key.serialize(this, entry.getValue())));
      }
    }
    return result;
  }

  private DefinitionProtos.Definition.LevelParameter writeLevelParameter(LevelVariable parameter) {
    DefinitionProtos.Definition.LevelParameter.Builder builder = DefinitionProtos.Definition.LevelParameter.newBuilder();
    builder.setIsPlevel(parameter.getType() == LevelVariable.LvlType.PLVL);
    if (parameter instanceof ParamLevelVariable) {
      builder.setName(parameter.getName());
      builder.setIndex(((ParamLevelVariable) parameter).getIndex());
      builder.setSize(((ParamLevelVariable) parameter).getSize());
    } else {
      builder.setSize(-1);
    }
    return builder.build();
  }

  private List<DefinitionProtos.Definition.LevelParameter> writeLevelParameters(List<? extends LevelVariable> parameters) {
    List<DefinitionProtos.Definition.LevelParameter> result = new ArrayList<>(parameters.size());
    for (LevelVariable parameter : parameters) {
      result.add(writeLevelParameter(parameter));
    }
    return result;
  }

  private DefinitionProtos.Definition.ClassData writeClassDefinition(ExpressionSerialization defSerializer, ClassDefinition definition) {
    DefinitionProtos.Definition.ClassData.Builder builder = DefinitionProtos.Definition.ClassData.newBuilder();

    builder.setBaseUniverseKind(defSerializer.writeUniverseKind(definition.getBaseUniverseKind()));

    for (Map.Entry<ClassDefinition, Levels> entry : definition.getSuperLevels().entrySet()) {
      builder.putSuperLevels(myCallTargetIndexProvider.getDefIndex(entry.getKey()), defSerializer.writeLevels(entry.getValue(), entry.getKey()));
    }

    for (ClassField field : definition.getPersonalFields()) {
      DefinitionProtos.Definition.ClassData.Field.Builder fBuilder = DefinitionProtos.Definition.ClassData.Field.newBuilder();
      fBuilder.setReferable(writeReferable(field));
      fBuilder.setType(defSerializer.visitPi(field.getType()));
      if (field.getTypeLevel() != null) {
        fBuilder.setTypeLevel(defSerializer.writeExpr(field.getTypeLevel()));
      }
      fBuilder.setResultTypeLevel(field.getResultTypeLevel());
      fBuilder.setNumberOfParameters(field.getNumberOfParameters());
      fBuilder.setIsExplicit(field.getReferable().isExplicitField());
      fBuilder.setIsParameter(field.getReferable().isParameterField());
      fBuilder.setIsRealParameter(field.getReferable().isRealParameterField());
      fBuilder.setIsProperty(field.isProperty());
      fBuilder.setIsHideable(field.isHideable());
      fBuilder.setUniverseKind(defSerializer.writeUniverseKind(field.getUniverseKind()));
      fBuilder.putAllUserData(writeUserData(field));
      builder.addPersonalField(fBuilder.build());
    }

    for (ClassField classField : definition.getFields()) {
      builder.addFieldRef(myCallTargetIndexProvider.getDefIndex(classField));
    }
    for (Map.Entry<ClassField, AbsExpression> impl : definition.getImplemented()) {
      builder.putImplementations(myCallTargetIndexProvider.getDefIndex(impl.getKey()), defSerializer.writeAbsExpr(impl.getValue()));
    }
    for (Map.Entry<ClassField, Pair<AbsExpression,Boolean>> defaultImpl : definition.getDefaults()) {
      builder.putDefaults(myCallTargetIndexProvider.getDefIndex(defaultImpl.getKey()), DefinitionProtos.Definition.DefaultData.newBuilder()
          .setExpr(defSerializer.writeAbsExpr(defaultImpl.getValue().proj1))
          .setIsFunc(defaultImpl.getValue().proj2).build());
    }
    for (Map.Entry<ClassField, Set<ClassField>> entry : definition.getDefaultDependencies().entrySet()) {
      builder.putDefaultDependencies(myCallTargetIndexProvider.getDefIndex(entry.getKey()), writeRefList(entry.getValue()));
    }
    for (Map.Entry<ClassField, Set<ClassField>> entry : definition.getDefaultImplDependencies().entrySet()) {
      builder.putDefaultImplDependencies(myCallTargetIndexProvider.getDefIndex(entry.getKey()), writeRefList(entry.getValue()));
    }
    for (Map.Entry<ClassField, PiExpression> entry : definition.getOverriddenFields()) {
      builder.putOverriddenField(myCallTargetIndexProvider.getDefIndex(entry.getKey()), defSerializer.visitPi(entry.getValue()));
    }
    for (ClassField field : definition.getCovariantFields()) {
      builder.addCovariantField(myCallTargetIndexProvider.getDefIndex(field));
    }
    for (ClassField field : definition.getOmegaFields()) {
      builder.addOmegaField(myCallTargetIndexProvider.getDefIndex(field));
    }
    builder.setSort(defSerializer.writeSort(definition.getSort()));

    for (ClassDefinition classDefinition : definition.getSuperClasses()) {
      builder.addSuperClassRef(myCallTargetIndexProvider.getDefIndex(classDefinition));
    }

    if (definition.getClassifyingField() != null) {
      builder.setCoercingFieldRef(myCallTargetIndexProvider.getDefIndex(definition.getClassifyingField()));
    } else {
      builder.setCoercingFieldRef(-1);
    }
    builder.setIsRecord(definition.isRecord());

    if (!definition.getCoerceData().isEmpty()) {
      builder.setCoerceData(writeCoerceData(definition.getCoerceData()));
    }
    if (definition.getSquasher() != null) {
      builder.setSquasher(myCallTargetIndexProvider.getDefIndex(definition.getSquasher()));
    }

    for (ClassDefinition.ParametersLevel parametersLevel : definition.getParametersLevels()) {
      DefinitionProtos.Definition.ClassParametersLevel.Builder parametersLevelBuilder = DefinitionProtos.Definition.ClassParametersLevel.newBuilder();
      parametersLevelBuilder.setParametersLevel(writeParametersLevel(defSerializer, parametersLevel));
      for (ClassField field : parametersLevel.fields) {
        parametersLevelBuilder.addField(myCallTargetIndexProvider.getDefIndex(field));
      }
      if (parametersLevel.strictList != null) {
        parametersLevelBuilder.setIsStrict(true);
        for (Pair<ClassDefinition, Set<ClassField>> pair : parametersLevel.strictList) {
          DefinitionProtos.Definition.ClassParametersLevel.ClassExtSig.Builder sig = DefinitionProtos.Definition.ClassParametersLevel.ClassExtSig.newBuilder();
          if (pair != null) {
            sig.setClassDef(myCallTargetIndexProvider.getDefIndex(pair.proj1));
            for (ClassField field : pair.proj2) {
              sig.addField(myCallTargetIndexProvider.getDefIndex(field));
            }
          }
          parametersLevelBuilder.addClassExtSig(sig.build());
        }
      }
      builder.addParametersLevel(parametersLevelBuilder.build());
    }

    for (ClassField goodThisField : definition.getGoodThisFields()) {
      builder.addGoodField(myCallTargetIndexProvider.getDefIndex(goodThisField));
    }

    for (ClassField typeClassField : definition.getTypeClassFields()) {
      builder.addTypeClassField(myCallTargetIndexProvider.getDefIndex(typeClassField));
    }

    return builder.build();
  }

  private DefinitionProtos.Definition.RefList writeRefList(Collection<? extends Definition> definitions) {
    DefinitionProtos.Definition.RefList.Builder builder = DefinitionProtos.Definition.RefList.newBuilder();
    for (Definition definition : definitions) {
      builder.addRef(getDefIndex(definition));
    }
    return builder.build();
  }

  private DefinitionProtos.Definition.TypeClassParameterKind writeTypeClassParameterKind(Definition.TypeClassParameterKind kind) {
    switch (kind) {
      case YES:
        return DefinitionProtos.Definition.TypeClassParameterKind.YES;
      case NO:
        return DefinitionProtos.Definition.TypeClassParameterKind.NO;
      case ONLY_LOCAL:
        return DefinitionProtos.Definition.TypeClassParameterKind.ONLY_LOCAL;
    }
    throw new IllegalStateException();
  }

  private DefinitionProtos.Definition.DataData writeDataDefinition(ExpressionSerialization defSerializer, DataDefinition definition) {
    DefinitionProtos.Definition.DataData.Builder builder = DefinitionProtos.Definition.DataData.newBuilder();

    builder.addAllOmegaParameter(definition.getOmegaParameters());

    builder.setHasEnclosingClass(definition.getEnclosingClass() != null);
    builder.addAllParam(defSerializer.writeParameters(definition.getParameters()));
    if (definition.getParametersTypecheckingOrder() != null) {
      builder.addAllParametersTypecheckingOrder(definition.getParametersTypecheckingOrder());
    }
    builder.addAllGoodThisParameters(definition.getGoodThisParameters());
    for (Definition.TypeClassParameterKind kind : definition.getTypeClassParameters()) {
      builder.addTypeClassParameters(writeTypeClassParameterKind(kind));
    }
    builder.addAllParametersLevels(writeParametersLevels(defSerializer, definition.getParametersLevels()));
    for (Definition recursiveDefinition : definition.getRecursiveDefinitions()) {
      builder.addRecursiveDefinition(myCallTargetIndexProvider.getDefIndex(recursiveDefinition));
    }
    if (definition.getSort() != null) {
      builder.setSort(defSerializer.writeSort(definition.getSort()));
    }

    for (Constructor constructor : definition.getConstructors()) {
      DefinitionProtos.Definition.DataData.Constructor.Builder cBuilder = DefinitionProtos.Definition.DataData.Constructor.newBuilder();
      cBuilder.setReferable(writeReferable(constructor));
      if (constructor.getPatterns() != null) {
        for (ExpressionPattern pattern : constructor.getPatterns()) {
          cBuilder.addPattern(defSerializer.writePattern(pattern));
        }
      }
      cBuilder.addAllParam(defSerializer.writeParameters(constructor.getParameters()));
      if (constructor.getParametersTypecheckingOrder() != null) {
        cBuilder.addAllParametersTypecheckingOrder(constructor.getParametersTypecheckingOrder());
      }
      cBuilder.addAllStrictParameters(constructor.getStrictParameters());
      cBuilder.addAllGoodThisParameters(constructor.getGoodThisParameters());
      for (Definition.TypeClassParameterKind kind : constructor.getTypeClassParameters()) {
        cBuilder.addTypeClassParameters(writeTypeClassParameterKind(kind));
      }
      if (constructor.getBody() != null) {
        cBuilder.setConditions(writeBody(defSerializer, constructor.getBody()));
      }
      cBuilder.putAllUserData(writeUserData(constructor));
      cBuilder.setRecursiveParameter(constructor.getRecursiveParameter() + 1);

      builder.addConstructor(cBuilder.build());
    }

    builder.setTruncatedLevel(definition.getTruncatedLevel());
    builder.setIsSquashed(definition.isSquashed());
    if (definition.getSquasher() != null) {
      builder.setSquasher(myCallTargetIndexProvider.getDefIndex(definition.getSquasher()));
    }
    int i = 0;
    for (DependentLink link = definition.getParameters(); link.hasNext(); link = link.getNext()) {
      builder.addCovariantParameter(definition.isCovariant(i++));
    }

    if (!definition.getCoerceData().isEmpty()) {
      builder.setCoerceData(writeCoerceData(definition.getCoerceData()));
    }

    return builder.build();
  }

  private DefinitionProtos.Definition.CoerceData writeCoerceData(CoerceData coerceData) {
    DefinitionProtos.Definition.CoerceData.Builder builder = DefinitionProtos.Definition.CoerceData.newBuilder();
    for (Map.Entry<CoerceData.Key, List<Definition>> entry : coerceData.getMapFrom()) {
      builder.addCoerceFrom(writeCoerceDataElement(entry));
    }
    for (Map.Entry<CoerceData.Key, List<Definition>> entry : coerceData.getMapTo()) {
      builder.addCoerceTo(writeCoerceDataElement(entry));
    }
    return builder.build();
  }

  private DefinitionProtos.Definition.CoerceData.Element writeCoerceDataElement(Map.Entry<CoerceData.Key, List<Definition>> entry) {
    DefinitionProtos.Definition.CoerceData.Element.Builder elementBuilder = DefinitionProtos.Definition.CoerceData.Element.newBuilder();
    if (entry.getKey() instanceof CoerceData.DefinitionKey) {
      elementBuilder.setDefinitionKey(DefinitionProtos.Definition.CoerceData.DefinitionKey.newBuilder().setClassifyingDef(myCallTargetIndexProvider.getDefIndex(((CoerceData.DefinitionKey) entry.getKey()).definition)));
    } else if (entry.getKey() instanceof CoerceData.PiKey) {
      elementBuilder.setConstantKey(DefinitionProtos.Definition.CoerceData.ConstantKey.PI);
    } else if (entry.getKey() instanceof CoerceData.SigmaKey) {
      elementBuilder.setConstantKey(DefinitionProtos.Definition.CoerceData.ConstantKey.SIGMA);
    } else if (entry.getKey() instanceof CoerceData.UniverseKey) {
      elementBuilder.setConstantKey(DefinitionProtos.Definition.CoerceData.ConstantKey.UNIVERSE);
    } else if (entry.getKey() instanceof CoerceData.AnyKey) {
      elementBuilder.setConstantKey(DefinitionProtos.Definition.CoerceData.ConstantKey.ANY);
    } else {
      throw new IllegalStateException();
    }
    for (Definition def : entry.getValue()) {
      elementBuilder.addCoercingDef(myCallTargetIndexProvider.getDefIndex(def));
    }
    return elementBuilder.build();
  }

  private DefinitionProtos.Definition.ParametersLevel writeParametersLevel(ExpressionSerialization defSerializer, ParametersLevel parametersLevel) {
    DefinitionProtos.Definition.ParametersLevel.Builder builder = DefinitionProtos.Definition.ParametersLevel.newBuilder();
    builder.setHasParameters(parametersLevel.parameters != null);
    if (parametersLevel.parameters != null) {
      builder.addAllParameter(defSerializer.writeParameters(parametersLevel.parameters));
    }
    builder.setLevel(parametersLevel.level);
    return builder.build();
  }

  private List<DefinitionProtos.Definition.ParametersLevel> writeParametersLevels(ExpressionSerialization defSerializer, List<? extends ParametersLevel> parametersLevels) {
    List<DefinitionProtos.Definition.ParametersLevel> result = new ArrayList<>();
    for (ParametersLevel parametersLevel : parametersLevels) {
      result.add(writeParametersLevel(defSerializer, parametersLevel));
    }
    return result;
  }

  private DefinitionProtos.Definition.ParameterOriginalDef writeParameterOriginalDef(Pair<TCDefReferable, Integer> pair) {
    DefinitionProtos.Definition.ParameterOriginalDef.Builder builder = DefinitionProtos.Definition.ParameterOriginalDef.newBuilder();
    builder.setIndex(pair.proj2);
    builder.setDefinition(myCallTargetIndexProvider.getDefIndex(pair.proj1));
    return builder.build();
  }

  private DefinitionProtos.Definition.FunctionData writeFunctionDefinition(ExpressionSerialization defSerializer, FunctionDefinition definition) {
    DefinitionProtos.Definition.FunctionData.Builder builder = DefinitionProtos.Definition.FunctionData.newBuilder();

    builder.addAllOmegaParameter(definition.getOmegaParameters());
    builder.addAllStrictParameters(definition.getStrictParameters());
    builder.setHasEnclosingClass(definition.getEnclosingClass() != null);
    builder.addAllParam(defSerializer.writeParameters(definition.getParameters()));
    if (definition.getParametersTypecheckingOrder() != null) {
      builder.addAllParametersTypecheckingOrder(definition.getParametersTypecheckingOrder());
    }
    builder.addAllGoodThisParameters(definition.getGoodThisParameters());
    for (Definition.TypeClassParameterKind kind : definition.getTypeClassParameters()) {
      builder.addTypeClassParameters(writeTypeClassParameterKind(kind));
    }
    builder.addAllParametersLevels(writeParametersLevels(defSerializer, definition.getParametersLevels()));
    for (Definition recursiveDefinition : definition.getRecursiveDefinitions()) {
      builder.addRecursiveDefinition(myCallTargetIndexProvider.getDefIndex(recursiveDefinition));
    }
    if (definition.getResultType() != null) {
      builder.setType(defSerializer.writeExpr(definition.getResultType()));
    }
    if (definition.getResultTypeLevel() != null) {
      builder.setTypeLevel(defSerializer.writeExpr(definition.getResultTypeLevel()));
    }
    switch (definition.getBodyHiddenStatus()) {
      case NOT_HIDDEN:
        builder.setBodyHiddenStatus(DefinitionProtos.Definition.FunctionData.HiddenStatus.NOT_HIDDEN);
        break;
      case HIDDEN:
        builder.setBodyHiddenStatus(DefinitionProtos.Definition.FunctionData.HiddenStatus.HIDDEN);
        break;
      case REALLY_HIDDEN:
        builder.setBodyHiddenStatus(DefinitionProtos.Definition.FunctionData.HiddenStatus.REALLY_HIDDEN);
        break;
    }
    DefinitionProtos.Definition.FunctionKind kind;
    switch (definition.getKind()) {
      case LEMMA:
        kind = definition.getReferable().getKind() == GlobalReferable.Kind.COCLAUSE_FUNCTION ? DefinitionProtos.Definition.FunctionKind.COCLAUSE_LEMMA : DefinitionProtos.Definition.FunctionKind.LEMMA;
        break;
      case SFUNC:
        kind = DefinitionProtos.Definition.FunctionKind.SFUNC;
        break;
      case TYPE:
        kind = DefinitionProtos.Definition.FunctionKind.TYPE;
        break;
      case INSTANCE:
        kind = DefinitionProtos.Definition.FunctionKind.INSTANCE;
        break;
      default:
        kind = definition.getReferable().getKind() == GlobalReferable.Kind.COCLAUSE_FUNCTION ? DefinitionProtos.Definition.FunctionKind.COCLAUSE : DefinitionProtos.Definition.FunctionKind.FUNC;
    }
    builder.setKind(kind);
    builder.setVisibleParameter(definition.getVisibleParameter());
    if (definition.getReallyActualBody() != null) {
      builder.setBody(writeBody(defSerializer, definition.getReallyActualBody()));
    }

    return builder.build();
  }

  private DefinitionProtos.Definition.DConstructorData writeDConstructor(ExpressionSerialization defSerializer, DConstructor definition) {
    DefinitionProtos.Definition.DConstructorData.Builder builder = DefinitionProtos.Definition.DConstructorData.newBuilder();
    builder.setFunction(writeFunctionDefinition(defSerializer, definition));
    builder.setNumberOfParameters(definition.getNumberOfParameters());
    if (definition.getPattern() != null) {
      builder.setPattern(writeDPattern(defSerializer, definition.getPattern()));
    }
    return builder.build();
  }

  private DefinitionProtos.Definition.DPattern writeDPattern(ExpressionSerialization defSerializer, ExpressionPattern pattern) {
    DefinitionProtos.Definition.DPattern.Builder builder = DefinitionProtos.Definition.DPattern.newBuilder();
    if (pattern instanceof BindingPattern) {
      builder.setBinding(defSerializer.writeBindingRef(((BindingPattern) pattern).getBinding()));
    } else if (pattern instanceof EmptyPattern) {
      throw new IllegalStateException("Empty pattern in defined constructor");
    } else if (pattern instanceof ConstructorExpressionPattern) {
      DefinitionProtos.Definition.DPattern.Constructor.Builder pBuilder = DefinitionProtos.Definition.DPattern.Constructor.newBuilder();
      pBuilder.setExpression(defSerializer.writeExpr(((ConstructorExpressionPattern) pattern).getDataExpression()));
      for (ExpressionPattern patternArgument : pattern.getSubPatterns()) {
        pBuilder.addPattern(writeDPattern(defSerializer, patternArgument));
      }
      builder.setConstructor(pBuilder.build());
    } else {
      throw new IllegalArgumentException();
    }
    return builder.build();
  }

  private DefinitionProtos.Body writeBody(ExpressionSerialization defSerializer, @NotNull Body body) {
    DefinitionProtos.Body.Builder bodyBuilder = DefinitionProtos.Body.newBuilder();
    if (body instanceof IntervalElim) {
      IntervalElim intervalElim = (IntervalElim) body;
      DefinitionProtos.Body.IntervalElim.Builder intervalBuilder = DefinitionProtos.Body.IntervalElim.newBuilder();
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
        intervalBuilder.setOtherwise(defSerializer.writeElimBody(intervalElim.getOtherwise()));
      }
      bodyBuilder.setIntervalElim(intervalBuilder);
    } else if (body instanceof ElimBody) {
      bodyBuilder.setElimBody(defSerializer.writeElimBody((ElimBody) body));
    } else if (body instanceof Expression) {
      bodyBuilder.setExpression(defSerializer.writeExpr((Expression) body));
    } else {
      throw new IllegalStateException();
    }
    return bodyBuilder.build();
  }

  private DefinitionProtos.Referable writeReferable(Definition definition) {
    DefinitionProtos.Referable.Builder builder = DefinitionProtos.Referable.newBuilder();
    GlobalReferable referable = definition.getReferable();
    builder.setName(referable.textRepresentation());
    builder.setPrecedence(writePrecedence(referable.getPrecedence()));
    builder.setIndex(myCallTargetIndexProvider.getDefIndex(definition));
    return builder.build();
  }

  static DefinitionProtos.Precedence writePrecedence(Precedence precedence) {
    DefinitionProtos.Precedence.Builder builder = DefinitionProtos.Precedence.newBuilder();
    switch (precedence.associativity) {
      case LEFT_ASSOC:
        builder.setAssoc(DefinitionProtos.Precedence.Assoc.LEFT);
        break;
      case RIGHT_ASSOC:
        builder.setAssoc(DefinitionProtos.Precedence.Assoc.RIGHT);
        break;
      case NON_ASSOC:
        builder.setAssoc(DefinitionProtos.Precedence.Assoc.NON_ASSOC);
        break;
    }
    builder.setPriority(precedence.priority);
    builder.setInfix(precedence.isInfix);
    return builder.build();
  }

  @Override
  public int getDefIndex(@NotNull CoreDefinition definition) {
    if (!(definition instanceof Definition)) {
      throw new IllegalArgumentException();
    }
    return myCallTargetIndexProvider.getDefIndex((Definition) definition);
  }
}
