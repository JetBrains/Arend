package org.arend.module.serialization;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.*;
import org.arend.core.elimtree.Body;
import org.arend.core.elimtree.ClauseBase;
import org.arend.core.elimtree.ElimTree;
import org.arend.core.elimtree.IntervalElim;
import org.arend.core.expr.AbsExpression;
import org.arend.core.expr.Expression;
import org.arend.core.expr.PiExpression;
import org.arend.core.pattern.BindingPattern;
import org.arend.core.pattern.ConstructorPattern;
import org.arend.core.pattern.EmptyPattern;
import org.arend.core.pattern.Pattern;
import org.arend.core.sort.Sort;
import org.arend.ext.reference.Precedence;
import org.arend.naming.reference.GlobalReferable;
import org.arend.util.Pair;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DefinitionSerialization {
  private final CallTargetIndexProvider myCallTargetIndexProvider;

  public DefinitionSerialization(CallTargetIndexProvider callTargetIndexProvider) {
    myCallTargetIndexProvider = callTargetIndexProvider;
  }

  DefinitionProtos.Definition writeDefinition(Definition definition) {
    final ExpressionSerialization defSerializer = new ExpressionSerialization(myCallTargetIndexProvider);

    final DefinitionProtos.Definition.Builder out = DefinitionProtos.Definition.newBuilder();
    out.setHasTypeClassReference(definition.getReferable().getTypeClassReference() != null);
    out.setUniverseKind(defSerializer.writeUniverseKind(definition.getUniverseKind()));

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

  private DefinitionProtos.Definition.ClassData writeClassDefinition(ExpressionSerialization defSerializer, ClassDefinition definition) {
    DefinitionProtos.Definition.ClassData.Builder builder = DefinitionProtos.Definition.ClassData.newBuilder();

    for (ClassField field : definition.getPersonalFields()) {
      DefinitionProtos.Definition.ClassData.Field.Builder fBuilder = DefinitionProtos.Definition.ClassData.Field.newBuilder();
      fBuilder.setReferable(writeReferable(field));
      fBuilder.setHasTypeClassReference(field.getReferable().getTypeClassReference() != null);
      fBuilder.setType(defSerializer.visitPi(field.getType(Sort.STD)));
      if (field.getTypeLevel() != null) {
        fBuilder.setTypeLevel(defSerializer.writeExpr(field.getTypeLevel()));
      }
      fBuilder.setNumberOfParameters(field.getNumberOfParameters());
      fBuilder.setIsExplicit(field.getReferable().isExplicitField());
      fBuilder.setIsParameter(field.getReferable().isParameterField());
      fBuilder.setIsProperty(field.isProperty());
      fBuilder.setIsHideable(field.isHideable());
      fBuilder.setIsCovariant(field.isCovariant());
      fBuilder.setUniverseKind(defSerializer.writeUniverseKind(field.getUniverseKind()));
      builder.addPersonalField(fBuilder.build());
    }

    for (ClassField classField : definition.getFields()) {
      builder.addFieldRef(myCallTargetIndexProvider.getDefIndex(classField));
    }
    for (Map.Entry<ClassField, AbsExpression> impl : definition.getImplemented()) {
      builder.putImplementations(myCallTargetIndexProvider.getDefIndex(impl.getKey()), defSerializer.writeAbsExpr(impl.getValue()));
    }
    for (Map.Entry<ClassField, PiExpression> entry : definition.getOverriddenFields()) {
      builder.putOverriddenField(myCallTargetIndexProvider.getDefIndex(entry.getKey()), defSerializer.visitPi(entry.getValue()));
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

    builder.addAllParam(defSerializer.writeParameters(definition.getParameters()));
    if (definition.getParametersTypecheckingOrder() != null) {
      builder.addAllParametersTypecheckingOrder(definition.getParametersTypecheckingOrder());
    }
    builder.addAllGoodThisParameters(definition.getGoodThisParameters());
    for (Definition.TypeClassParameterKind kind : definition.getTypeClassParameters()) {
      builder.addTypeClassParameters(writeTypeClassParameterKind(kind));
    }
    builder.addAllParametersLevels(writeParametersLevels(defSerializer, definition.getParametersLevels()));
    if (definition.getSort() != null) {
      builder.setSort(defSerializer.writeSort(definition.getSort()));
    }

    for (Constructor constructor : definition.getConstructors()) {
      DefinitionProtos.Definition.DataData.Constructor.Builder cBuilder = DefinitionProtos.Definition.DataData.Constructor.newBuilder();
      cBuilder.setReferable(writeReferable(constructor));
      if (constructor.getPatterns() != null) {
        for (Pattern pattern : constructor.getPatterns().getPatternList()) {
          cBuilder.addPattern(writePattern(defSerializer, pattern));
        }
      }
      for (ClauseBase clause : constructor.getClauses()) {
        cBuilder.addClause(writeClause(defSerializer, clause));
      }
      cBuilder.addAllParam(defSerializer.writeParameters(constructor.getParameters()));
      if (constructor.getParametersTypecheckingOrder() != null) {
        cBuilder.addAllParametersTypecheckingOrder(constructor.getParametersTypecheckingOrder());
      }
      cBuilder.addAllGoodThisParameters(constructor.getGoodThisParameters());
      for (Definition.TypeClassParameterKind kind : constructor.getTypeClassParameters()) {
        cBuilder.addTypeClassParameters(writeTypeClassParameterKind(kind));
      }
      if (constructor.getBody() != null) {
        cBuilder.setConditions(writeBody(defSerializer, constructor.getBody()));
      }

      builder.addConstructor(cBuilder.build());
    }

    builder.setIsTruncated(definition.isTruncated());
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
    for (Map.Entry<Definition, List<FunctionDefinition>> entry : coerceData.getMapFrom()) {
      DefinitionProtos.Definition.CoerceData.Element.Builder elementBuilder = DefinitionProtos.Definition.CoerceData.Element.newBuilder();
      elementBuilder.setClassifyingDef(entry.getKey() == null ? 0 : myCallTargetIndexProvider.getDefIndex(entry.getKey()) + 1);
      for (FunctionDefinition def : entry.getValue()) {
        elementBuilder.addCoercingDef(myCallTargetIndexProvider.getDefIndex(def));
      }
      builder.addCoerceFrom(elementBuilder.build());
    }
    for (Map.Entry<Definition, List<Definition>> entry : coerceData.getMapTo()) {
      DefinitionProtos.Definition.CoerceData.Element.Builder elementBuilder = DefinitionProtos.Definition.CoerceData.Element.newBuilder();
      elementBuilder.setClassifyingDef(entry.getKey() == null ? 0 : myCallTargetIndexProvider.getDefIndex(entry.getKey()) + 1);
      for (Definition def : entry.getValue()) {
        elementBuilder.addCoercingDef(myCallTargetIndexProvider.getDefIndex(def));
      }
      builder.addCoerceTo(elementBuilder.build());
    }
    return builder.build();
  }

  private DefinitionProtos.Definition.Clause writeClause(ExpressionSerialization defSerializer, ClauseBase clause) {
    DefinitionProtos.Definition.Clause.Builder builder = DefinitionProtos.Definition.Clause.newBuilder();
    for (Pattern pattern : clause.patterns) {
      builder.addPattern(writePattern(defSerializer, pattern));
    }
    builder.setExpression(defSerializer.writeExpr(clause.expression));
    return builder.build();
  }

  private DefinitionProtos.Definition.Pattern writePattern(ExpressionSerialization defSerializer, Pattern pattern) {
    DefinitionProtos.Definition.Pattern.Builder builder = DefinitionProtos.Definition.Pattern.newBuilder();
    if (pattern instanceof BindingPattern) {
      builder.setBinding(DefinitionProtos.Definition.Pattern.Binding.newBuilder()
        .setVar(defSerializer.writeParameter(((BindingPattern) pattern).getBinding())));
    } else if (pattern instanceof EmptyPattern) {
      builder.setEmpty(DefinitionProtos.Definition.Pattern.Empty.newBuilder());
    } else if (pattern instanceof ConstructorPattern) {
      DefinitionProtos.Definition.Pattern.Constructor.Builder pBuilder = DefinitionProtos.Definition.Pattern.Constructor.newBuilder();
      pBuilder.setExpression(defSerializer.writeExpr(((ConstructorPattern) pattern).getDataExpression()));
      for (Pattern patternArgument : ((ConstructorPattern) pattern).getArguments()) {
        pBuilder.addPattern(writePattern(defSerializer, patternArgument));
      }
      builder.setConstructor(pBuilder.build());
    } else {
      throw new IllegalArgumentException();
    }
    return builder.build();
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

  private DefinitionProtos.Definition.FunctionData writeFunctionDefinition(ExpressionSerialization defSerializer, FunctionDefinition definition) {
    DefinitionProtos.Definition.FunctionData.Builder builder = DefinitionProtos.Definition.FunctionData.newBuilder();

    builder.addAllParam(defSerializer.writeParameters(definition.getParameters()));
    if (definition.getParametersTypecheckingOrder() != null) {
      builder.addAllParametersTypecheckingOrder(definition.getParametersTypecheckingOrder());
    }
    builder.addAllGoodThisParameters(definition.getGoodThisParameters());
    for (Definition.TypeClassParameterKind kind : definition.getTypeClassParameters()) {
      builder.addTypeClassParameters(writeTypeClassParameterKind(kind));
    }
    builder.addAllParametersLevels(writeParametersLevels(defSerializer, definition.getParametersLevels()));
    if (definition.getResultType() != null) {
      builder.setType(defSerializer.writeExpr(definition.getResultType()));
    }
    if (definition.getResultTypeLevel() != null) {
      builder.setTypeLevel(defSerializer.writeExpr(definition.getResultTypeLevel()));
    }
    builder.setBodyIsHidden(definition.isBodyHidden());
    DefinitionProtos.Definition.FunctionKind kind;
    switch (definition.getKind()) {
      case LEMMA:
        kind = DefinitionProtos.Definition.FunctionKind.LEMMA;
        break;
      case SFUNC:
        kind = DefinitionProtos.Definition.FunctionKind.SFUNC;
        break;
      default:
        kind = DefinitionProtos.Definition.FunctionKind.FUNC;
    }
    builder.setKind(kind);
    builder.setVisibleParameter(definition.getVisibleParameter());
    if (definition.getActualBody() != null) {
      builder.setBody(writeBody(defSerializer, definition.getActualBody()));
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

  private DefinitionProtos.Definition.DPattern writeDPattern(ExpressionSerialization defSerializer, Pattern pattern) {
    DefinitionProtos.Definition.DPattern.Builder builder = DefinitionProtos.Definition.DPattern.newBuilder();
    if (pattern instanceof BindingPattern) {
      builder.setBinding(defSerializer.writeBindingRef(((BindingPattern) pattern).getBinding()));
    } else if (pattern instanceof EmptyPattern) {
      throw new IllegalStateException("Empty pattern in defined constructor");
    } else if (pattern instanceof ConstructorPattern) {
      DefinitionProtos.Definition.DPattern.Constructor.Builder pBuilder = DefinitionProtos.Definition.DPattern.Constructor.newBuilder();
      pBuilder.setExpression(defSerializer.writeExpr(((ConstructorPattern) pattern).getDataExpression()));
      for (Pattern patternArgument : ((ConstructorPattern) pattern).getArguments()) {
        pBuilder.addPattern(writeDPattern(defSerializer, patternArgument));
      }
      builder.setConstructor(pBuilder.build());
    } else {
      throw new IllegalArgumentException();
    }
    return builder.build();
  }

  private DefinitionProtos.Body writeBody(ExpressionSerialization defSerializer, @Nonnull Body body) {
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
        intervalBuilder.setOtherwise(defSerializer.writeElimTree(intervalElim.getOtherwise()));
      }
      bodyBuilder.setIntervalElim(intervalBuilder);
    } else if (body instanceof ElimTree) {
      bodyBuilder.setElimTree(defSerializer.writeElimTree((ElimTree) body));
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
}
