package org.arend.core.definition;

import org.arend.core.context.binding.inference.FunctionInferenceVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.*;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.error.ErrorReporter;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.CoerceClashError;
import org.arend.typechecking.result.TypecheckingResult;
import org.arend.typechecking.visitor.CheckTypeVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CoerceData {
  private final Map<Definition, List<Definition>> myMapFrom = new HashMap<>();
  private final Map<Definition, List<Definition>> myMapTo = new HashMap<>();
  private final Definition myDefinition;

  public CoerceData(Definition definition) {
    myDefinition = definition;
  }

  public Set<Map.Entry<Definition, List<Definition>>> getMapFrom() {
    return myMapFrom.entrySet();
  }

  public Set<Map.Entry<Definition, List<Definition>>> getMapTo() {
    return myMapTo.entrySet();
  }

  public boolean isEmpty() {
    return myMapFrom.isEmpty() && myMapTo.isEmpty();
  }

  public void putCoerceFrom(Definition classifyingDefinition, List<Definition> coercingDefinitions) {
    myMapFrom.put(classifyingDefinition, coercingDefinitions);
  }

  public void putCoerceTo(Definition classifyingDefinition, List<Definition> coercingDefinitions) {
    myMapTo.put(classifyingDefinition, coercingDefinitions);
  }

  public static TypecheckingResult coerce(TypecheckingResult result, Expression expectedType, Concrete.Expression sourceNode, CheckTypeVisitor visitor) {
    DefCallExpression actualDefCall = result.type.cast(DefCallExpression.class);
    DefCallExpression expectedDefCall = expectedType.cast(DefCallExpression.class);
    if (actualDefCall != null && expectedDefCall != null && (actualDefCall.getDefinition() == expectedDefCall.getDefinition() || actualDefCall.getDefinition() instanceof ClassDefinition && expectedDefCall.getDefinition() instanceof ClassDefinition && ((ClassDefinition) actualDefCall.getDefinition()).isSubClassOf((ClassDefinition) expectedDefCall.getDefinition()))) {
      return null;
    }
    CoerceData actualCoerceData = actualDefCall != null ? actualDefCall.getDefinition().getCoerceData() : null;
    if (actualCoerceData != null && actualCoerceData.myMapTo.isEmpty()) {
      actualCoerceData = null;
    }
    CoerceData expectedCoerceData = expectedDefCall != null ? expectedDefCall.getDefinition().getCoerceData() : null;
    if (expectedCoerceData != null && expectedCoerceData.myMapFrom.isEmpty()) {
      expectedCoerceData = null;
    }
    if (actualCoerceData == null && expectedCoerceData == null) {
      return null;
    }

    // Coerce from a definition
    if (expectedCoerceData != null && actualDefCall != null && isClassifyingDefCall(actualDefCall)) {
      List<Definition> defs = expectedCoerceData.myMapFrom.get(actualDefCall.getDefinition());
      if (defs != null) {
        return coerceResult(result, defs, expectedType, sourceNode, visitor, false, false);
      }
    }

    // Coerce to a definition
    if (actualCoerceData != null && expectedDefCall != null && isClassifyingDefCall(expectedDefCall)) {
      List<Definition> defs = actualCoerceData.myMapTo.get(expectedDefCall.getDefinition());
      if (defs != null) {
        return coerceResult(result, defs, expectedType, sourceNode, visitor, false, false);
      }
    }

    // Can't coerce neither from nor to a definition
    if (actualCoerceData != null && isClassifyingDefCall(expectedDefCall) || expectedCoerceData != null && isClassifyingDefCall(actualDefCall)) {
      return null;
    }

    // Coerce from an arbitrary type
    if (expectedCoerceData != null) {
      List<Definition> defs = expectedCoerceData.myMapFrom.get(null);
      if (defs != null) {
        return coerceResult(result, defs, expectedType, sourceNode, visitor, true, false);
      }
    }

    // Coerce to an arbitrary type
    if (actualCoerceData != null) {
      List<Definition> defs = actualCoerceData.myMapTo.get(null);
      if (defs != null) {
        return coerceResult(result, defs, expectedType, sourceNode, visitor, false, true);
      }
    }

    // Can't coerce
    return null;
  }

  private static boolean isClassifyingDefCall(DefCallExpression defCall) {
    Definition definition = defCall == null ? null : defCall.getDefinition();
    return definition instanceof DataDefinition || definition instanceof ClassDefinition || definition instanceof Constructor;
  }

  private static TypecheckingResult coerceResult(TypecheckingResult result, Collection<? extends Definition> defs, Expression expectedType, Concrete.Expression sourceNode, CheckTypeVisitor visitor, boolean argStrict, boolean resultStrict) {
    for (Definition def : defs) {
      if (def instanceof ClassField) {
        ClassField field = (ClassField) def;
        ClassCallExpression classCall = result.type.cast(ClassCallExpression.class);
        Sort sort = classCall == null ? Sort.generateInferVars(visitor.getEquations(), field.getParentClass().getUniverseKind(), sourceNode) : classCall.getSortArgument();
        result = new TypecheckingResult(FieldCallExpression.make(field, sort, result.expression), field.getType(sort).applyExpression(result.expression).normalize(NormalizationMode.WHNF));
      } else if (def instanceof FunctionDefinition || def instanceof Constructor) {
        List<Expression> arguments = new ArrayList<>();
        DependentLink link = def.getParameters();
        ExprSubstitution substitution = new ExprSubstitution();
        int index = 0;

        List<Expression> dataArgs;
        if (def instanceof Constructor) {
          dataArgs = new ArrayList<>();
          for (DependentLink dataParams = ((Constructor) def).getDataTypeParameters(); dataParams.hasNext(); dataParams = dataParams.getNext(), index++) {
            Expression arg = new InferenceReferenceExpression(new FunctionInferenceVariable(def, link, index + 1, link.getTypeExpr(), sourceNode, visitor.getAllBindings()), visitor.getEquations());
            substitution.add(dataParams, arg);
            dataArgs.add(arg);
            index++;
          }
        } else dataArgs = Collections.emptyList();

        while (true) {
          DependentLink next = link.getNext();
          if (next.hasNext()) {
            Expression arg = new InferenceReferenceExpression(new FunctionInferenceVariable(def, link, index + 1, link.getTypeExpr(), sourceNode, visitor.getAllBindings()), visitor.getEquations());
            substitution.add(link, arg);
            arguments.add(arg);
            link = next;
            index++;
          } else {
            arguments.add(result.expression);
            break;
          }
        }

        Sort sortArg = Sort.generateInferVars(visitor.getEquations(), def.getUniverseKind(), sourceNode);
        LevelSubstitution levelSubst = sortArg.toLevelSubstitution();
        if (!visitor.checkNormalizedResult(link.getTypeExpr().subst(substitution, levelSubst), result, sourceNode, argStrict)) {
          if (argStrict) {
            return null;
          }
          result.expression = new ErrorExpression(result.expression);
        }

        substitution.add(link, result.expression);
        if (def instanceof FunctionDefinition) {
          result = new TypecheckingResult(FunCallExpression.make((FunctionDefinition) def, sortArg, arguments), ((FunctionDefinition) def).getResultType().subst(substitution, levelSubst).normalize(NormalizationMode.WHNF));
        } else {
          Expression resultExpr = ConCallExpression.make((Constructor) def, sortArg, dataArgs, arguments);
          result = new TypecheckingResult(resultExpr, resultExpr.computeType().normalize(NormalizationMode.WHNF));
        }
      } else {
        throw new IllegalStateException();
      }
    }

    if (!visitor.checkNormalizedResult(expectedType, result, sourceNode, resultStrict)) {
      if (resultStrict) {
        return null;
      }
      result.expression = new ErrorExpression(result.expression);
    }
    return result;
  }

  public Definition addCoerceFrom(Definition classifyingDefinition, FunctionDefinition coercingDefinition) {
    if (!(classifyingDefinition instanceof DataDefinition || classifyingDefinition instanceof ClassDefinition)) {
      classifyingDefinition = null;
    }

    List<Definition> newList = myMapFrom.compute(classifyingDefinition, (k, oldList) -> oldList != null && oldList.size() == 1 ? oldList : Collections.singletonList(coercingDefinition));
    Definition oldDef = newList.size() == 1 && newList.get(0) != coercingDefinition ? newList.get(0) : null;
    if (oldDef != null) {
      return oldDef;
    }

    CoerceData coerceData = classifyingDefinition != null ? classifyingDefinition.getCoerceData() : null;
    if (coerceData != null) {
      for (Map.Entry<Definition, List<Definition>> entry : coerceData.myMapFrom.entrySet()) {
        if (entry.getKey() != null && entry.getKey() != classifyingDefinition && entry.getKey() != myDefinition) {
          myMapFrom.computeIfAbsent(entry.getKey(), k -> {
            List<Definition> list = new ArrayList<>(entry.getValue().size() + 1);
            list.addAll(entry.getValue());
            list.add(coercingDefinition);
            return list;
          });
        }
      }
    }

    return null;
  }

  public FunctionDefinition addCoerceTo(Definition classifyingDefinition, FunctionDefinition coercingDefinition) {
    if (!(classifyingDefinition instanceof DataDefinition || classifyingDefinition instanceof ClassDefinition)) {
      classifyingDefinition = null;
    }

    List<Definition> newList = myMapTo.compute(classifyingDefinition, (k, oldList) -> oldList != null && oldList.size() == 1 && oldList.get(0) instanceof FunctionDefinition ? oldList : Collections.singletonList(coercingDefinition));
    FunctionDefinition oldDef = newList.size() == 1 && newList.get(0) != coercingDefinition ? (FunctionDefinition) newList.get(0) : null;
    if (oldDef != null) {
      return oldDef;
    }

    CoerceData coerceData = classifyingDefinition != null ? classifyingDefinition.getCoerceData() : null;
    if (coerceData != null) {
      for (Map.Entry<Definition, List<Definition>> entry : coerceData.myMapTo.entrySet()) {
        if (entry.getKey() != null && entry.getKey() != classifyingDefinition && entry.getKey() != myDefinition) {
          myMapTo.computeIfAbsent(entry.getKey(), k -> {
            List<Definition> list = new ArrayList<>(entry.getValue().size() + 1);
            list.add(coercingDefinition);
            list.addAll(entry.getValue());
            return list;
          });
        }
      }
    }

    return null;
  }

  private Definition getClassifyingDefinition(Expression type) {
    DefCallExpression defCall = type.cast(DefCallExpression.class);
    Definition classifyingDefinition = defCall == null ? null : defCall.getDefinition();
    return classifyingDefinition instanceof DataDefinition || classifyingDefinition instanceof ClassDefinition || classifyingDefinition instanceof Constructor ? classifyingDefinition : null;
  }

  public void addCoercingField(ClassField coercingField, @Nullable ErrorReporter errorReporter, @Nullable Concrete.SourceNode cause) {
    Definition classifyingDefinition = getClassifyingDefinition(coercingField.getType(Sort.STD).getCodomain());
    if (myMapTo.putIfAbsent(classifyingDefinition, Collections.singletonList(coercingField)) != null && errorReporter != null) {
      errorReporter.report(new CoerceClashError(classifyingDefinition, cause));
    }
  }

  public void addCoercingConstructor(Constructor constructor, @Nullable ErrorReporter errorReporter, @Nullable Concrete.SourceNode cause) {
    DependentLink param = DependentLink.Helper.getLast(constructor.getParameters());
    if (!param.hasNext()) {
      return;
    }

    Definition classifyingDefinition = getClassifyingDefinition(param.getTypeExpr());
    if (myMapFrom.putIfAbsent(classifyingDefinition, Collections.singletonList(constructor)) != null && errorReporter != null) {
      errorReporter.report(new CoerceClashError(classifyingDefinition, cause));
    }
  }
}
