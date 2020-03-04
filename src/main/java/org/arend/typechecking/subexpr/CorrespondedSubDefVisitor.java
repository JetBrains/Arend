package org.arend.typechecking.subexpr;

import org.arend.core.definition.Constructor;
import org.arend.core.definition.DataDefinition;
import org.arend.core.definition.Definition;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.elimtree.Body;
import org.arend.core.elimtree.ElimBody;
import org.arend.core.elimtree.ElimClause;
import org.arend.core.expr.Expression;
import org.arend.core.pattern.Pattern;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteDefinitionVisitor;
import org.arend.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class CorrespondedSubDefVisitor implements
    ConcreteDefinitionVisitor<@NotNull Definition, @Nullable Pair<Expression, Concrete.Expression>> {
  private final @NotNull CorrespondedSubExprVisitor visitor;

  public CorrespondedSubDefVisitor(@NotNull CorrespondedSubExprVisitor visitor) {
    this.visitor = visitor;
  }

  public CorrespondedSubDefVisitor(@NotNull Concrete.Expression subExpr) {
    this(new CorrespondedSubExprVisitor(subExpr));
  }

  private Pair<Expression, Concrete.Expression> visitBody(
      @NotNull Concrete.FunctionBody body,
      @Nullable Body coreBody,
      @Nullable Expression coreResultType
  ) {
    if (body instanceof Concrete.TermFunctionBody && coreBody instanceof Expression) {
      Concrete.Expression term = body.getTerm();
      if (term != null) return term.accept(visitor, (Expression) coreBody);
    } else if (body instanceof Concrete.ElimFunctionBody && coreBody instanceof ElimBody) {
      // Assume they have the same order.
      List<Concrete.FunctionClause> clauses = body.getClauses();
      List<? extends ElimClause<Pattern>> coreClauses = ((ElimBody) coreBody).getClauses();
      // Interval pattern matching are stored in a special way,
      // maybe it's a TODO to implement it.
      if (clauses.size() != coreClauses.size()) return null;
      for (int i = 0; i < clauses.size(); i++) {
        Concrete.FunctionClause clause = clauses.get(i);
        ElimClause<Pattern> coreClause = coreClauses.get(i);
        Concrete.Expression expression = clause.getExpression();
        if (expression == null) return null;
        Pair<Expression, Concrete.Expression> clauseVisited = expression.accept(visitor, coreClause.getExpression());
        if (clauseVisited != null) return clauseVisited;
      }
    } else if (body instanceof Concrete.CoelimFunctionBody && coreBody == null) {
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
      Pair<Expression, Concrete.Expression> typeResult = resultType.accept(visitor, coreResultType);
      if (typeResult != null) return typeResult;
    }
    Pair<Expression, Concrete.Expression> parametersResult = visitor
        .visitSigmaParameters(def.getParameters(), coreDef.getParameters());
    if (parametersResult != null) return parametersResult;
    return visitBody(def.getBody(), coreDef.getActualBody(), coreResultType);
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitData(Concrete.DataDefinition def, Definition params) {
    DataDefinition coreDef;
    if (params instanceof DataDefinition) coreDef = (DataDefinition) params;
    else return null;
    return def.getConstructorClauses()
        .stream()
        .flatMap(clause -> clause.getConstructors().stream())
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
    // TODO :)
    return null;
  }
}
