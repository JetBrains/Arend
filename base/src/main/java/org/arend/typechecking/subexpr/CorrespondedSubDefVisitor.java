package org.arend.typechecking.subexpr;

import org.arend.core.definition.*;
import org.arend.core.elimtree.Body;
import org.arend.core.elimtree.ElimBody;
import org.arend.core.expr.*;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCFieldReferable;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteDefinitionVisitor;
import org.arend.util.Pair;
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
    if (body instanceof Concrete.TermFunctionBody && coreBody instanceof Expression) {
      Concrete.Expression term = body.getTerm();
      if (term != null) return term.accept(visitor, (Expression) coreBody);
    } else if (body instanceof Concrete.ElimFunctionBody && coreBody instanceof ElimBody) {
      // Assume they have the same order.
      return visitor.visitElimTree(body.getClauses(), ((ElimBody) coreBody).getClauses());
    } else if (body instanceof Concrete.CoelimFunctionBody) {
      if (coreBody instanceof NewExpression) coreResultType = ((NewExpression) coreBody).getType();
      if (coreResultType instanceof ClassCallExpression) {
        Map<ClassField, Expression> implementations = ((ClassCallExpression) coreResultType).getImplementedHere();
        for (var coclause : body.getCoClauseElements()) {
          if (coclause instanceof Concrete.ClassFieldImpl) {
            var statementVisited = visitor.visitStatement(implementations, (Concrete.ClassFieldImpl) coclause);
            if (statementVisited != null) return statementVisited;
          }
        }
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
      if (concreteRaw instanceof Concrete.ClassField) {
        var concrete = (Concrete.ClassField) concreteRaw;
        TCFieldReferable referable = concrete.getData();
        Optional<Expression> field = coreDef.getFields()
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
      } else if (concreteRaw instanceof Concrete.ClassFieldImpl) {
        var concrete = (Concrete.ClassFieldImpl) concreteRaw;
        Referable implementedField = concrete.getImplementedField();
        Optional<AbsExpression> field = coreDef.getFields()
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
