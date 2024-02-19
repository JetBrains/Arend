package org.arend.typechecking.subexpr;

import org.arend.core.definition.*;
import org.arend.core.elimtree.Body;
import org.arend.core.elimtree.ElimBody;
import org.arend.core.expr.*;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCFieldReferable;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteDefinitionVisitor;
import org.arend.ext.util.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CorrespondedSubDefVisitor implements
    ConcreteDefinitionVisitor<@NotNull Definition,
        @Nullable Pair<@NotNull Expression, Concrete.@NotNull Expression>> {
  private final @NotNull CorrespondedSubExprVisitor visitor;

  @Contract(pure = true)
  public @NotNull List<@NotNull SubExprError> getExprError() {
    return visitor.getErrors();
  }

  public CorrespondedSubDefVisitor(@NotNull CorrespondedSubExprVisitor visitor) {
    this.visitor = visitor;
  }

  public CorrespondedSubDefVisitor(@NotNull Concrete.Expression subExpr) {
    this(new CorrespondedSubExprVisitor(subExpr));
  }

  private @Nullable Pair<@NotNull Expression, Concrete.@NotNull Expression> visitBody(
      @NotNull Concrete.FunctionBody body,
      @Nullable Body coreBody,
      @Nullable Expression coreResultType
  ) {
    if (body instanceof Concrete.TermFunctionBody) {
      Concrete.Expression term = body.getTerm();
      if (term instanceof Concrete.NewExpression) {
        Concrete.Expression classExpr = ((Concrete.NewExpression) term).getExpression();
        if (classExpr instanceof Concrete.ClassExtExpression) {
          return visitCoclauses(((Concrete.ClassExtExpression) classExpr).getCoclauses().getCoclauseList(), coreBody, coreResultType);
        }
      }
      return coreBody instanceof Expression ? term.accept(visitor, (Expression) coreBody) : null;
    } else if (body instanceof Concrete.ElimFunctionBody && coreBody instanceof ElimBody) {
      // Assume they have the same order.
      return visitor.visitElimTree(body.getClauses(), ((ElimBody) coreBody).getClauses());
    } else if (body instanceof Concrete.CoelimFunctionBody) {
      return visitCoclauses(body.getCoClauseElements(), coreBody, coreResultType);
    } else {
      return null;
    }
  }

  private Pair<Expression, Concrete.Expression> visitCoclauses(List<? extends Concrete.CoClauseElement> coclauses, Body coreBody, Expression coreResultType) {
    Map<ClassField, Expression> implementations = new HashMap<>();
    if (coreResultType instanceof ClassCallExpression) {
      implementations.putAll(((ClassCallExpression) coreResultType).getImplementedHere());
    }
    if (coreBody instanceof NewExpression) {
      implementations.putAll(((NewExpression) coreBody).getClassCall().getImplementedHere());
    }
    for (var coclause : coclauses) {
      if (coclause instanceof Concrete.ClassFieldImpl) {
        var statementVisited = visitor.visitStatement(implementations, (Concrete.ClassFieldImpl) coclause);
        if (statementVisited != null) return statementVisited;
      }
    }
    return null;
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitFunction(Concrete.BaseFunctionDefinition def, Definition params) {
    FunctionDefinition coreDef;
    if (params instanceof FunctionDefinition)
      coreDef = (FunctionDefinition) params;
    else return null;
    Concrete.Expression resultType = def.getResultType();
    Expression coreResultType = coreDef.getResultType();
    if (resultType != null && coreResultType != null) {
      var typeResult = resultType.accept(visitor, coreResultType);
      if (typeResult != null) return typeResult;
    }
    var parametersResult = visitor
        .visitSigmaParameters(def.getParameters(), coreDef.getParameters());
    if (parametersResult != null) return parametersResult;
    return visitBody(def.getBody(), coreDef.getReallyActualBody(), coreResultType);
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitData(Concrete.DataDefinition def, Definition params) {
    DataDefinition coreDef;
    if (params instanceof DataDefinition) coreDef = (DataDefinition) params;
    else return null;
    return def.getConstructorClauses()
      .stream()
      .map(Concrete.ConstructorClause::getConstructors)
      .flatMap(Collection::stream)
      .map(cons -> {
        Constructor coreC = coreDef.getConstructor(cons.getData());
        if (coreC == null) return null;
        return visitor.visitSigmaParameters(cons.getParameters(), coreC.getParameters());
      })
      .filter(Objects::nonNull)
      .findFirst()
      .orElseGet(() -> visitor
        .visitSigmaParameters(def.getParameters(), coreDef.getParameters()));
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitClass(Concrete.ClassDefinition def, Definition params) {
    ClassDefinition coreDef;
    if (params instanceof ClassDefinition) coreDef = (ClassDefinition) params;
    else return null;
    var desugared = def.getStage().ordinal() >= Concrete.Stage.DESUGARIZED.ordinal();
    for (Concrete.ClassElement concreteRaw : def.getElements())
      if (concreteRaw instanceof Concrete.ClassField concrete) {
        TCFieldReferable referable = concrete.getData();
        Optional<Expression> field = coreDef.getAllFields()
          .stream()
          .filter(classField -> classField.getReferable() == referable)
          .map(ClassField::getResultType)
          .findFirst();
        if (field.isEmpty()) continue;
        Expression fieldExpr = field.get();
        var parameters = concrete.getParameters();
        if (desugared && !parameters.isEmpty()) {
          // Clone the list and remove the first "this" parameter if already desugared
          parameters = parameters.subList(1, parameters.size());
        }
        var accept = !parameters.isEmpty() && fieldExpr instanceof PiExpression
          ? visitor.visitPiImpl(parameters, concrete.getResultType(), (PiExpression) fieldExpr)
          : concrete.getResultType().accept(visitor, fieldExpr);
        if (accept != null) return accept;
      } else if (concreteRaw instanceof Concrete.ClassFieldImpl concrete) {
        Referable implementedField = concrete.getImplementedField();
        Optional<AbsExpression> field = coreDef.getImplementedFields()
            .stream()
            .filter(o -> o.getReferable() == implementedField)
            .findFirst()
            .map(coreDef::getImplementation);
        if (field.isEmpty()) continue;
        // The binding is `this` I believe
        var accept = concrete.implementation == null ? null : concrete.implementation.accept(visitor, field.get().getExpression());
        if (accept != null) return accept;
      }
    return null;
  }
}
