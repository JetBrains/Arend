package org.arend.core.definition;

import org.arend.core.context.binding.inference.FunctionInferenceVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.*;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.Levels;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.error.ErrorReporter;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.CoerceClashError;
import org.arend.typechecking.result.TypecheckingResult;
import org.arend.typechecking.visitor.CheckTypeVisitor;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CoerceData {
  public interface Key {}

  public static class DefinitionKey implements Key {
    public final Definition definition;

    public DefinitionKey(Definition definition) {
      this.definition = definition;
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof DefinitionKey && definition == ((DefinitionKey) o).definition;
    }

    @Override
    public int hashCode() {
      return Objects.hash(definition);
    }
  }

  private static class BaseKey implements Key {
    @Override
    public int hashCode() {
      return Objects.hash();
    }

    @Override
    public boolean equals(Object obj) {
      return obj.getClass().equals(getClass());
    }
  }

  public static class PiKey extends BaseKey {}
  public static class SigmaKey extends BaseKey {}
  public static class UniverseKey extends BaseKey {}
  public static class AnyKey extends BaseKey {}

  private final Map<Key, List<Definition>> myMapFrom = new HashMap<>();
  private final Map<Key, List<Definition>> myMapTo = new HashMap<>();
  private final Definition myDefinition;

  public CoerceData(Definition definition) {
    myDefinition = definition;
  }

  public Set<Map.Entry<Key, List<Definition>>> getMapFrom() {
    return myMapFrom.entrySet();
  }

  public Set<Map.Entry<Key, List<Definition>>> getMapTo() {
    return myMapTo.entrySet();
  }

  public boolean isEmpty() {
    return myMapFrom.isEmpty() && myMapTo.isEmpty();
  }

  public void putCoerceFrom(Key key, List<Definition> coercingDefinitions) {
    myMapFrom.put(key, coercingDefinitions);
  }

  public void putCoerceTo(Key key, List<Definition> coercingDefinitions) {
    myMapTo.put(key, coercingDefinitions);
  }

  public static TypecheckingResult coerceToKey(TypecheckingResult result, Key key, Concrete.Expression sourceNode, CheckTypeVisitor visitor) {
    CoerceData coerceData = result.type instanceof DefCallExpression ? ((DefCallExpression) result.type).getDefinition().getCoerceData() : null;
    if (coerceData == null) {
      return null;
    }

    List<Definition> defs = coerceData.myMapTo.get(key);
    return defs != null ? coerceResult(result, defs, null, sourceNode, visitor, false, false) : null;
  }

  public static TypecheckingResult coerce(TypecheckingResult result, Expression expectedType, Concrete.SourceNode sourceNode, CheckTypeVisitor visitor) {
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

    Key actualKey = getKey(result.type);
    Key expectedKey = getKey(expectedType);

    // Coerce from a definition
    if (expectedCoerceData != null && !(actualKey instanceof AnyKey)) {
      List<Definition> defs = expectedCoerceData.myMapFrom.get(actualKey);
      if (defs != null) {
        return coerceResult(result, defs, expectedType, sourceNode, visitor, false, false);
      }
    }

    // Coerce to a definition
    if (actualCoerceData != null && !(expectedKey instanceof AnyKey)) {
      List<Definition> defs = actualCoerceData.myMapTo.get(expectedKey);
      if (defs != null) {
        return coerceResult(result, defs, expectedType, sourceNode, visitor, false, false);
      }
    }

    // Can't coerce from or to a definition
    if (expectedCoerceData != null && !(actualKey instanceof AnyKey)) {
      actualKey = new AnyKey();
    }
    if (actualCoerceData != null && !(expectedKey instanceof AnyKey)) {
      expectedKey = new AnyKey();
    }

    // Coerce from an arbitrary type
    if (expectedCoerceData != null) {
      List<Definition> defs = expectedCoerceData.myMapFrom.get(actualKey);
      if (defs != null) {
        return coerceResult(result, defs, expectedType, sourceNode, visitor, true, false);
      }
    }

    // Coerce to an arbitrary type
    if (actualCoerceData != null) {
      List<Definition> defs = actualCoerceData.myMapTo.get(expectedKey);
      if (defs != null) {
        return coerceResult(result, defs, expectedType, sourceNode, visitor, false, true);
      }
    }

    // Can't coerce
    return null;
  }

  private static TypecheckingResult coerceResult(TypecheckingResult result, Collection<? extends Definition> defs, Expression expectedType, Concrete.SourceNode sourceNode, CheckTypeVisitor visitor, boolean argStrict, boolean resultStrict) {
    for (Definition def : defs) {
      if (def instanceof ClassField) {
        ClassField field = (ClassField) def;
        ClassCallExpression classCall = result.type.cast(ClassCallExpression.class);
        Levels levels = classCall == null ? field.getParentClass().generateInferVars(visitor.getEquations(), sourceNode) : classCall.getLevels(field.getParentClass());
        Expression resultExpr = FieldCallExpression.make(field, levels, result.expression);
        result = new TypecheckingResult(resultExpr, resultExpr.getType());
      } else if (def instanceof FunctionDefinition || def instanceof Constructor) {
        List<Expression> arguments = new ArrayList<>();
        DependentLink link = def.getParameters();
        ExprSubstitution substitution = new ExprSubstitution();
        int index = 0;

        List<Expression> dataArgs;
        if (def instanceof Constructor) {
          dataArgs = new ArrayList<>();
          for (DependentLink dataParams = ((Constructor) def).getDataTypeParameters(); dataParams.hasNext(); dataParams = dataParams.getNext(), index++) {
            Expression arg = InferenceReferenceExpression.make(new FunctionInferenceVariable(def, link, index + 1, link.getTypeExpr(), sourceNode, visitor.getAllBindings()), visitor.getEquations());
            substitution.add(dataParams, arg);
            dataArgs.add(arg);
            index++;
          }
        } else dataArgs = Collections.emptyList();

        while (true) {
          DependentLink next = link.getNext();
          if (next.hasNext()) {
            Expression arg = InferenceReferenceExpression.make(new FunctionInferenceVariable(def, link, index + 1, link.getTypeExpr(), sourceNode, visitor.getAllBindings()), visitor.getEquations());
            substitution.add(link, arg);
            arguments.add(arg);
            link = next;
            index++;
          } else {
            arguments.add(result.expression);
            break;
          }
        }

        Levels levels = def.generateInferVars(visitor.getEquations(), sourceNode);
        if (!visitor.checkCoerceResult(link.getTypeExpr().subst(substitution, levels.makeSubstitution(def)), result, sourceNode, argStrict)) {
          if (argStrict) {
            return null;
          }
          result.expression = new ErrorExpression(result.expression);
        }

        substitution.add(link, result.expression);
        if (def instanceof FunctionDefinition) {
          result = new TypecheckingResult(FunCallExpression.make((FunctionDefinition) def, levels, arguments), ((FunctionDefinition) def).getResultType().subst(substitution, levels.makeSubstitution(def)).normalize(NormalizationMode.WHNF));
        } else {
          Expression resultExpr = ConCallExpression.make((Constructor) def, levels, dataArgs, arguments);
          result = new TypecheckingResult(resultExpr, resultExpr.computeType().normalize(NormalizationMode.WHNF));
        }
      } else {
        throw new IllegalStateException();
      }
    }

    if (expectedType != null && !visitor.checkCoerceResult(expectedType, result, sourceNode, resultStrict)) {
      if (resultStrict) {
        return null;
      }
      result.expression = new ErrorExpression(result.expression);
    }
    return result;
  }

  public Definition addCoerceFrom(Expression type, FunctionDefinition coercingDefinition) {
    Key key = getKey(type);
    if (key instanceof DefinitionKey && ((DefinitionKey) key).definition == myDefinition) {
      return null;
    }

    List<Definition> newList = myMapFrom.compute(key, (k, oldList) -> oldList != null && oldList.size() == 1 ? oldList : Collections.singletonList(coercingDefinition));
    Definition oldDef = newList.size() == 1 && newList.get(0) != coercingDefinition ? newList.get(0) : null;
    if (oldDef != null) {
      return oldDef;
    }

    CoerceData coerceData = key instanceof DefinitionKey ? ((DefinitionKey) key).definition.getCoerceData() : null;
    if (coerceData != null) {
      for (Map.Entry<Key, List<Definition>> entry : coerceData.myMapFrom.entrySet()) {
        if (!(entry.getKey() instanceof DefinitionKey) || ((DefinitionKey) entry.getKey()).definition != myDefinition) {
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

  public FunctionDefinition addCoerceTo(Expression type, FunctionDefinition coercingDefinition) {
    Key key = getKey(type);
    if (key instanceof DefinitionKey && ((DefinitionKey) key).definition == myDefinition) {
      return null;
    }

    List<Definition> newList = myMapTo.compute(key, (k, oldList) -> oldList != null && oldList.size() == 1 && oldList.get(0) instanceof FunctionDefinition ? oldList : Collections.singletonList(coercingDefinition));
    FunctionDefinition oldDef = newList.size() == 1 && newList.get(0) != coercingDefinition ? (FunctionDefinition) newList.get(0) : null;
    if (oldDef != null) {
      return oldDef;
    }

    CoerceData coerceData = key instanceof DefinitionKey ? ((DefinitionKey) key).definition.getCoerceData() : null;
    if (coerceData != null) {
      for (Map.Entry<Key, List<Definition>> entry : coerceData.myMapTo.entrySet()) {
        if (!(entry.getKey() instanceof DefinitionKey) || ((DefinitionKey) entry.getKey()).definition != myDefinition) {
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

  private static Key getKey(Expression type) {
    DefCallExpression defCall = type.cast(DefCallExpression.class);
    if (defCall != null) {
      Definition def = defCall.getDefinition();
      return def instanceof DataDefinition || def instanceof ClassDefinition || def instanceof Constructor ? new DefinitionKey(def) : new AnyKey();
    }

    PiExpression pi = type.cast(PiExpression.class);
    if (pi != null) {
      return new PiKey();
    }

    SigmaExpression sigma = type.cast(SigmaExpression.class);
    if (sigma != null) {
      return new SigmaKey();
    }

    UniverseExpression universe = type.cast(UniverseExpression.class);
    if (universe != null) {
      return new UniverseKey();
    }

    return new AnyKey();
  }

  public void addCoercingField(ClassField coercingField, @Nullable ErrorReporter errorReporter, @Nullable Concrete.SourceNode cause) {
    Key key = getKey(coercingField.getType().getCodomain());
    if (myMapTo.putIfAbsent(key, Collections.singletonList(coercingField)) != null && errorReporter != null) {
      errorReporter.report(new CoerceClashError(key, cause));
    }
  }

  public void addCoercingConstructor(Constructor constructor, @Nullable ErrorReporter errorReporter, @Nullable Concrete.SourceNode cause) {
    DependentLink param = DependentLink.Helper.getLast(constructor.getParameters());
    if (!param.hasNext()) {
      return;
    }

    Key key = getKey(param.getTypeExpr());
    if (myMapFrom.putIfAbsent(key, Collections.singletonList(constructor)) != null && errorReporter != null) {
      errorReporter.report(new CoerceClashError(key, cause));
    }
  }
}
