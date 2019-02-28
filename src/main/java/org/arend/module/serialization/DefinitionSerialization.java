package org.arend.module.serialization;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.*;
import org.arend.core.elimtree.Body;
import org.arend.core.elimtree.ClauseBase;
import org.arend.core.elimtree.ElimTree;
import org.arend.core.elimtree.IntervalElim;
import org.arend.core.expr.Expression;
import org.arend.core.expr.LamExpression;
import org.arend.core.pattern.BindingPattern;
import org.arend.core.pattern.ConstructorPattern;
import org.arend.core.pattern.EmptyPattern;
import org.arend.core.pattern.Pattern;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.naming.reference.GlobalReferable;
import org.arend.term.Precedence;
import org.arend.util.Pair;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefinitionSerialization {
  private final CallTargetIndexProvider myCallTargetIndexProvider;

  public DefinitionSerialization(CallTargetIndexProvider callTargetIndexProvider) {
    myCallTargetIndexProvider = callTargetIndexProvider;
  }

  DefinitionProtos.Definition writeDefinition(Definition definition) {
    final DefinitionProtos.Definition.Builder out = DefinitionProtos.Definition.newBuilder();

    switch (definition.status()) {
      case HEADER_HAS_ERRORS:
        out.setStatus(DefinitionProtos.Definition.Status.HEADER_HAS_ERRORS);
        break;
      case BODY_HAS_ERRORS:
      case MAY_BE_TYPE_CHECKED_WITH_ERRORS:
      case BODY_NEEDS_TYPE_CHECKING:
        out.setStatus(DefinitionProtos.Definition.Status.BODY_HAS_ERRORS);
        break;
      case HEADER_NEEDS_TYPE_CHECKING:
      case HAS_ERRORS:
        out.setStatus(DefinitionProtos.Definition.Status.HAS_ERRORS);
        break;
      case MAY_BE_TYPE_CHECKED_WITH_WARNINGS:
      case HAS_WARNINGS:
        out.setStatus(DefinitionProtos.Definition.Status.HAS_WARNINGS);
        break;
      case NO_ERRORS:
        out.setStatus(DefinitionProtos.Definition.Status.NO_ERRORS);
        break;
      default:
        throw new IllegalStateException("Unknown typechecking status");
    }

    out.setHasTypeClassReference(definition.getReferable().getTypeClassReference() != null);
    out.setHasUniverses(definition.hasUniverses());

    final ExpressionSerialization defSerializer = new ExpressionSerialization(myCallTargetIndexProvider);

    if (definition instanceof ClassDefinition) {
      // type cannot possibly have errors
      out.setClass_(writeClassDefinition(defSerializer, (ClassDefinition) definition));
    } else if (definition instanceof DataDefinition) {
      out.setData(writeDataDefinition(defSerializer, (DataDefinition) definition));
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
      fBuilder.setType(defSerializer.writeExpr(field.getType(Sort.STD)));
      if (field.getTypeLevel() != null) {
        fBuilder.setTypeLevel(defSerializer.writeExpr(field.getTypeLevel()));
      }
      fBuilder.setIsExplicit(field.getReferable().isExplicitField());
      fBuilder.setIsParameter(field.getReferable().isParameterField());
      fBuilder.setIsProperty(field.isProperty());
      fBuilder.setIsHideable(field.isHideable());
      builder.addPersonalField(fBuilder.build());
    }

    for (ClassField classField : definition.getFields()) {
      builder.addFieldRef(myCallTargetIndexProvider.getDefIndex(classField));
    }
    for (Map.Entry<ClassField, LamExpression> impl : definition.getImplemented()) {
      builder.putImplementations(myCallTargetIndexProvider.getDefIndex(impl.getKey()), defSerializer.writeExpr(impl.getValue()));
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

    for (Map.Entry<? extends Set<ClassField>, ? extends Level> entry : definition.getLevels().entrySet()) {
      DefinitionProtos.Definition.ClassData.ImplLevel.Builder implLevelBuild = DefinitionProtos.Definition.ClassData.ImplLevel.newBuilder();
      for (ClassField field : entry.getKey()) {
        implLevelBuild.addField(myCallTargetIndexProvider.getDefIndex(field));
      }
      implLevelBuild.setLevel(defSerializer.writeLevel(entry.getValue()));
      builder.addImplLevel(implLevelBuild.build());
    }

    for (ClassField goodThisField : definition.getGoodThisFields()) {
      builder.addGoodField(myCallTargetIndexProvider.getDefIndex(goodThisField));
    }

    for (ClassField typeClassField : definition.getTypeClassFields()) {
      builder.addTypeClassField(myCallTargetIndexProvider.getDefIndex(typeClassField));
    }

    List<? extends ClassField> fieldOrder = definition.getTypecheckingFieldOrder();
    if (fieldOrder != null) {
      DefinitionProtos.Definition.ClassData.FieldOrder.Builder fieldOrderBuilder = DefinitionProtos.Definition.ClassData.FieldOrder.newBuilder();
      for (ClassField field : fieldOrder) {
        fieldOrderBuilder.addField(myCallTargetIndexProvider.getDefIndex(field));
      }
      builder.setTypecheckingFieldOrder(fieldOrderBuilder.build());
    }

    return builder.build();
  }

  private DefinitionProtos.Definition.DataData writeDataDefinition(ExpressionSerialization defSerializer, DataDefinition definition) {
    DefinitionProtos.Definition.DataData.Builder builder = DefinitionProtos.Definition.DataData.newBuilder();

    builder.addAllParam(defSerializer.writeParameters(definition.getParameters()));
    if (definition.getParametersTypecheckingOrder() != null) {
      builder.addAllParametersTypecheckingOrder(definition.getParametersTypecheckingOrder());
    }
    builder.addAllGoodThisParameters(definition.getGoodThisParameters());
    builder.addAllTypeClassParameters(definition.getTypeClassParameters());
    if (definition.status().headerIsOK()) {
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
      cBuilder.addAllTypeClassParameters(constructor.getTypeClassParameters());
      if (constructor.getBody() != null) {
        cBuilder.setConditions(writeBody(defSerializer, constructor.getBody()));
      }
      cBuilder.setNumberOfIntervalParameters(constructor.getNumberOfIntervalParameters());

      builder.addConstructor(cBuilder.build());
    }

    builder.setMatchesOnInterval(definition.matchesOnInterval());
    builder.setIsTruncated(definition.isTruncated());
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

  private DefinitionProtos.Definition.FunctionData writeFunctionDefinition(ExpressionSerialization defSerializer, FunctionDefinition definition) {
    DefinitionProtos.Definition.FunctionData.Builder builder = DefinitionProtos.Definition.FunctionData.newBuilder();

    if (definition.getParameters() != null) {
      builder.addAllParam(defSerializer.writeParameters(definition.getParameters()));
      if (definition.getParametersTypecheckingOrder() != null) {
        builder.addAllParametersTypecheckingOrder(definition.getParametersTypecheckingOrder());
      }
      builder.addAllGoodThisParameters(definition.getGoodThisParameters());
      builder.addAllTypeClassParameters(definition.getTypeClassParameters());
    }
    if (definition.getResultType() != null) {
      builder.setType(defSerializer.writeExpr(definition.getResultType()));
    }
    if (definition.getResultTypeLevel() != null) {
      builder.setTypeLevel(defSerializer.writeExpr(definition.getResultTypeLevel()));
    }
    builder.setIsLemma(definition.isLemma());
    builder.setVisibleParameter(definition.getVisibleParameter());
    if (definition.status().bodyIsOK() && definition.getActualBody() != null) {
      builder.setBody(writeBody(defSerializer, definition.getActualBody()));
    }

    return builder.build();
  }

  private DefinitionProtos.Body writeBody(ExpressionSerialization defSerializer, @Nonnull Body body) {
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
