package org.arend.core.definition;

import org.arend.core.context.binding.inference.FunctionInferenceVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.*;
import org.arend.core.expr.type.ExpectedType;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.core.subst.StdLevelSubstitution;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.result.TypecheckingResult;
import org.arend.typechecking.visitor.CheckTypeVisitor;

import java.util.*;

public class CoerceData {
  private Map<Definition, List<FunctionDefinition>> myMapFrom = new HashMap<>();
  private Map<Definition, List<Definition>> myMapTo = new HashMap<>();
  private final Definition myDefinition;

  public CoerceData(Definition definition) {
    myDefinition = definition;
  }

  public Set<Map.Entry<Definition, List<FunctionDefinition>>> getMapFrom() {
    return myMapFrom.entrySet();
  }

  public Set<Map.Entry<Definition, List<Definition>>> getMapTo() {
    return myMapTo.entrySet();
  }

  public boolean isEmpty() {
    return myMapFrom.isEmpty() && myMapTo.isEmpty();
  }

  public void putCoerceFrom(Definition classifyingDefinition, List<FunctionDefinition> coercingDefinitions) {
    myMapFrom.put(classifyingDefinition, coercingDefinitions);
  }

  public void putCoerceTo(Definition classifyingDefinition, List<Definition> coercingDefinitions) {
    myMapTo.put(classifyingDefinition, coercingDefinitions);
  }

  public static TypecheckingResult coerce(TypecheckingResult result, ExpectedType expectedType, Concrete.Expression sourceNode, CheckTypeVisitor visitor) {
    DefCallExpression actualDefCall = result.type.cast(DefCallExpression.class);
    DefCallExpression expectedDefCall = expectedType instanceof Expression ? ((Expression) expectedType).cast(DefCallExpression.class) : null;
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
      List<FunctionDefinition> defs = expectedCoerceData.myMapFrom.get(actualDefCall.getDefinition());
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
      List<FunctionDefinition> defs = expectedCoerceData.myMapFrom.get(null);
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

  private static TypecheckingResult coerceResult(TypecheckingResult result, Collection<? extends Definition> defs, ExpectedType expectedType, Concrete.Expression sourceNode, CheckTypeVisitor visitor, boolean argStrict, boolean resultStrict) {
    for (Definition def : defs) {
      if (def instanceof ClassField) {
        ClassField field = (ClassField) def;
        ClassCallExpression classCall = result.type.cast(ClassCallExpression.class);
        Sort sort = classCall == null ? Sort.generateInferVars(visitor.getEquations(), field.getParentClass().hasUniverses(), sourceNode) : classCall.getSortArgument();
        result = new TypecheckingResult(FieldCallExpression.make(field, sort, result.expression), field.getType(sort).applyExpression(result.expression).normalize(NormalizationMode.WHNF));
      } else {
        List<Expression> arguments = new ArrayList<>();
        DependentLink link = def.getParameters();
        ExprSubstitution substitution = new ExprSubstitution();
        int index = 0;
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

        Sort sortArg = Sort.generateInferVars(visitor.getEquations(), def.hasUniverses(), sourceNode);
        LevelSubstitution levelSubst = new StdLevelSubstitution(sortArg);
        if (!visitor.checkNormalizedResult(link.getTypeExpr().subst(substitution, levelSubst), result, sourceNode, argStrict)) {
          if (argStrict) {
            return null;
          }
          result.expression = new ErrorExpression(result.expression, null);
        }

        substitution.add(link, result.expression);
        result = new TypecheckingResult(new FunCallExpression((FunctionDefinition) def, sortArg, arguments), ((FunctionDefinition) def).getResultType().subst(substitution, levelSubst).normalize(NormalizationMode.WHNF));
      }
    }

    if (!visitor.checkNormalizedResult(expectedType, result, sourceNode, resultStrict)) {
      if (resultStrict) {
        return null;
      }
      result.expression = new ErrorExpression(result.expression, null);
    }
    return result;
  }

  public FunctionDefinition addCoerceFrom(Definition classifyingDefinition, FunctionDefinition coercingDefinition) {
    if (!(classifyingDefinition instanceof DataDefinition || classifyingDefinition instanceof ClassDefinition)) {
      classifyingDefinition = null;
    }

    List<FunctionDefinition> newList = myMapFrom.compute(classifyingDefinition, (k, oldList) -> oldList != null && oldList.size() == 1 ? oldList : Collections.singletonList(coercingDefinition));
    FunctionDefinition oldDef = newList.size() == 1 && newList.get(0) != coercingDefinition ? newList.get(0) : null;
    if (oldDef != null) {
      return oldDef;
    }

    CoerceData coerceData = classifyingDefinition != null ? classifyingDefinition.getCoerceData() : null;
    if (coerceData != null) {
      for (Map.Entry<Definition, List<FunctionDefinition>> entry : coerceData.myMapFrom.entrySet()) {
        if (entry.getKey() != null && entry.getKey() != classifyingDefinition && entry.getKey() != myDefinition) {
          myMapFrom.computeIfAbsent(entry.getKey(), k -> {
            List<FunctionDefinition> list = new ArrayList<>(entry.getValue().size() + 1);
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

  public void addCoercingField(ClassField coercingField) {
    DefCallExpression defCall = coercingField.getType(Sort.STD).getCodomain().cast(DefCallExpression.class);
    Definition classifyingDefinition = defCall == null ? null : defCall.getDefinition();
    if (!(classifyingDefinition instanceof DataDefinition || classifyingDefinition instanceof ClassDefinition || classifyingDefinition instanceof Constructor)) {
      classifyingDefinition = null;
    }

    myMapTo.put(classifyingDefinition, Collections.singletonList(coercingField));
  }
}
