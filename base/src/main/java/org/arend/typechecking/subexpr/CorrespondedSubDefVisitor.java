package org.arend.typechecking.subexpr;

import org.arend.core.definition.*;
import org.arend.core.elimtree.Body;
import org.arend.core.elimtree.ElimBody;
import org.arend.core.elimtree.ElimClause;
import org.arend.core.expr.ClassCallExpression;
import org.arend.core.expr.Expression;
import org.arend.core.expr.PiExpression;
import org.arend.core.pattern.Pattern;
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
      List<Concrete.FunctionClause> clauses = body.getClauses();
      List<? extends ElimClause<Pattern>> coreClauses = ((ElimBody) coreBody).getClauses();
      return visitor.visitElimTree(clauses, coreClauses);
    } else if (body instanceof Concrete.CoelimFunctionBody && coreBody == null && coreResultType instanceof ClassCallExpression) {
      Map<ClassField, Expression> implementations = ((ClassCallExpression) coreResultType).getImplementedHere();
      List<Concrete.CoClauseElement> coclauses = body.getCoClauseElements();
      for (Concrete.CoClauseElement coclause : coclauses)
        if (coclause instanceof Concrete.ClassFieldImpl) {
          Pair<Expression, Concrete.Expression> statementVisited = visitor.visitStatement(implementations, (Concrete.ClassFieldImpl) coclause);
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
    ClassDefinition coreDef;
    if (params instanceof ClassDefinition) coreDef = (ClassDefinition) params;
    else return null;
    for (Concrete.ClassElement concreteRaw : def.getElements())
      if (concreteRaw instanceof Concrete.ClassField) {
        Concrete.ClassField concrete = (Concrete.ClassField) concreteRaw;
        TCFieldReferable referable = concrete.getData();
        Optional<Expression> field = coreDef.getFields()
            .stream()
            .filter(classField -> classField.getReferable() == referable)
            .map(ClassField::getResultType)
            .findFirst();
        if (!field.isPresent()) continue;
        Expression fieldExpr = field.get();
        List<Concrete.TypeParameter> concreteParameters = concrete.getParameters();
        // Clone the list and remove the first "this" parameter
        List<Concrete.TypeParameter> parameters = concreteParameters.isEmpty()
            ? Collections.emptyList() : concreteParameters.subList(1, concreteParameters.size());
        Pair<Expression, Concrete.Expression> accept = !parameters.isEmpty() && fieldExpr instanceof PiExpression
            ? visitor.visitPiImpl(parameters, concrete.getResultType(), (PiExpression) fieldExpr)
            : concrete.getResultType().accept(visitor, fieldExpr);
        if (accept != null) return accept;
      }
    return null;
  }
}
