package org.arend.typechecking.subexpr;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.ClassField;
import org.arend.core.elimtree.ElimClause;
import org.arend.core.expr.*;
import org.arend.core.expr.let.LetClause;
import org.arend.core.pattern.Pattern;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.naming.reference.ClassReferable;
import org.arend.naming.reference.FieldReferable;
import org.arend.naming.reference.Referable;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteExpressionVisitor;
import org.arend.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class CorrespondedSubExprVisitor implements
    ConcreteExpressionVisitor<@NotNull Expression,
        @Nullable Pair<@NotNull Expression, Concrete.@NotNull Expression>> {
  private final @NotNull Concrete.Expression subExpr;
  private @Nullable SubExprError error;

  public @Nullable SubExprError getError() {
    return error;
  }

  public CorrespondedSubExprVisitor(@NotNull Concrete.Expression subExpr) {
    this.subExpr = subExpr;
  }

  private boolean matchesSubExpr(@NotNull Concrete.Expression expr) {
    return Objects.equals(expr.getData(), subExpr.getData());
  }

  private @Nullable Pair<Expression, Concrete.Expression> atomicExpr(@NotNull Concrete.Expression expr, @NotNull Expression coreExpr) {
    return matchesSubExpr(expr) ? new Pair<>(coreExpr, expr) : null;
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitHole(Concrete.HoleExpression expr, Expression coreExpr) {
    return atomicExpr(expr, coreExpr);
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitReference(Concrete.ReferenceExpression expr, Expression coreExpr) {
    return atomicExpr(expr, coreExpr);
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitThis(Concrete.ThisExpression expr, Expression coreExpr) {
    return atomicExpr(expr, coreExpr);
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitInferenceReference(Concrete.InferenceReferenceExpression expr, Expression coreExpr) {
    return atomicExpr(expr, coreExpr);
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitNumericLiteral(Concrete.NumericLiteral expr, Expression coreExpr) {
    return atomicExpr(expr, coreExpr);
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitUniverse(Concrete.UniverseExpression expr, Expression coreExpr) {
    return atomicExpr(expr, coreExpr);
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitGoal(Concrete.GoalExpression expr, Expression coreExpr) {
    return atomicExpr(expr, coreExpr);
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitProj(Concrete.ProjExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return new Pair<>(coreExpr, expr);
    ProjExpression coreProjExpr = coreExpr.cast(ProjExpression.class);
    if (coreProjExpr == null) {
      error = SubExprError.mismatch(coreExpr);
      return null;
    }
    return expr.getExpression().accept(this, coreProjExpr.getExpression());
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitNew(Concrete.NewExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return new Pair<>(coreExpr, expr);
    NewExpression coreNewExpr = coreExpr.cast(NewExpression.class);
    if (coreNewExpr == null) {
      error = SubExprError.mismatch(coreExpr);
      return null;
    }
    return expr.getExpression().accept(this, coreNewExpr.getClassCall());
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitTuple(Concrete.TupleExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return new Pair<>(coreExpr, expr);
    TupleExpression coreTupleExpr = coreExpr.cast(TupleExpression.class);
    if (coreTupleExpr == null) {
      error = SubExprError.mismatch(coreExpr);
      return null;
    }
    Pair<Expression, Concrete.Expression> result =
        visitExprs(coreTupleExpr.getFields(), expr.getFields());
    if (error != null) error.setErrorExpr(coreExpr);
    return result;
  }

  private @Nullable Pair<@NotNull Expression, Concrete.@NotNull Expression>
  visitExprs(@NotNull List<? extends Expression> coreExpr,
             @NotNull List<? extends Concrete.Expression> expr) {
    for (int i = 0; i < expr.size(); i++) {
      Pair<Expression, Concrete.Expression> accepted = expr.get(i)
          .accept(this, coreExpr.get(i));
      if (accepted != null) return accepted;
    }
    error = new SubExprError(SubExprError.Kind.ExprListNoMatch);
    return null;
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitLet(Concrete.LetExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return new Pair<>(coreExpr, expr);
    LetExpression coreLetExpr = coreExpr.cast(LetExpression.class);
    if (coreLetExpr == null) {
      error = SubExprError.mismatch(coreExpr);
      return null;
    }
    List<Concrete.LetClause> exprClauses = expr.getClauses();
    List<LetClause> coreClauses = coreLetExpr.getClauses();
    for (int i = 0; i < exprClauses.size(); i++) {
      Pair<Expression, Concrete.Expression> accepted =
              visitLetClause(coreClauses.get(i), exprClauses.get(i));
      if (accepted != null) return accepted;
    }
    return expr.getExpression().accept(this, coreLetExpr.getExpression());
  }

  private @Nullable Pair<@NotNull Expression, Concrete.@NotNull Expression>
  visitLetClause(@NotNull LetClause coreLetClause, @NotNull Concrete.LetClause exprLetClause) {
    Pair<Expression, Concrete.Expression> accepted = exprLetClause.getTerm().accept(this, coreLetClause.getExpression());
    if (accepted != null) return accepted;

    Concrete.Expression resultType = exprLetClause.getResultType();
    if (resultType == null) {
      error = new SubExprError(SubExprError.Kind.ConcreteHasNoTypeBinding, coreLetClause.getTypeExpr());
      return null;
    }

    Expression coreResultType = coreLetClause.getTypeExpr();
    if (coreResultType instanceof PiExpression) {
      return visitPiImpl(exprLetClause.getParameters(), resultType, (PiExpression) coreResultType);
    } else return resultType.accept(this, coreResultType);
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitTyped(Concrete.TypedExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return new Pair<>(coreExpr, expr);
    return expr.expression.accept(this, coreExpr);
  }

  private @Nullable Pair<@NotNull Expression, Concrete.@NotNull Expression>
  visitClonedApp(@NotNull Concrete.AppExpression expr, @NotNull Expression coreExpr) {
    // This is a mutable reference
    List<Concrete.Argument> arguments = expr.getArguments();
    if (arguments.isEmpty()) return expr.getFunction().accept(this, coreExpr);

    int lastArgIndex = arguments.size() - 1;
    if (coreExpr instanceof AppExpression) {
      Concrete.Argument lastArgument = arguments.get(lastArgIndex);
      Expression function = coreExpr;
      PiExpression type;
      AppExpression coreAppExpr;
      do {
        assert function instanceof AppExpression;
        coreAppExpr = (AppExpression) function;
        function = coreAppExpr.getFunction();
        Expression functionType = function.getType();
        if (functionType != null)
          functionType = functionType.normalize(NormalizationMode.WHNF);
        type = functionType instanceof PiExpression ? (PiExpression) functionType : null;
        if (type == null) {
          error = new SubExprError(SubExprError.Kind.ExpectPi, functionType);
          return null;
        }
      } while (type.getParameters().isExplicit() != lastArgument.isExplicit());
      Pair<Expression, Concrete.Expression> accepted = lastArgument.getExpression().accept(this, coreAppExpr.getArgument());
      if (accepted != null) return accepted;
      arguments.remove(lastArgIndex);
      return visitClonedApp(expr, function);
    } else if (coreExpr instanceof LamExpression) {
      // `f a` (concrete) gets elaborated to `\b -> f a b` (core) if `f` takes 2
      // arguments, so we try to match `f a` (concrete) and `f a b` (core),
      // ignoring the extra argument `b`.
      return visitClonedApp(expr, ((LamExpression) coreExpr).getBody());
    } else if (coreExpr instanceof DefCallExpression) {
      return visitDefCallArguments((DefCallExpression) coreExpr, arguments);
    } else return null;
  }

  private @Nullable Pair<@NotNull Expression, Concrete.@NotNull Expression>
  visitDefCallArguments(@NotNull DefCallExpression expression,
                        @NotNull List<Concrete.Argument> arguments) {
    Iterator<Concrete.Argument> iterator = arguments.iterator();
    if (expression instanceof ClassCallExpression)
      return visitClassCallArguments((ClassCallExpression) expression, iterator);
    else return visitNonClassCallDefCallArguments(expression, iterator);
  }

  private @Nullable Pair<@NotNull Expression, Concrete.@NotNull Expression>
  visitClassCallArguments(@NotNull ClassCallExpression coreClassCall,
                          @NotNull Iterator<Concrete.Argument> arguments) {
    Map<ClassField, Expression> implementedHere = coreClassCall.getImplementedHere();
    Concrete.Argument argument = arguments.next();
    ClassDefinition definition = coreClassCall.getDefinition();
    for (ClassField field : definition.getFields()) {
      if (definition.isImplemented(field)) continue;
      if (argument.isExplicit() == field.getReferable().isExplicitField()) {
        Expression implementation = implementedHere.get(field);
        if (implementation == null) continue;
        Pair<Expression, Concrete.Expression> accepted = argument.getExpression().accept(this, implementation);
        if (accepted != null) return accepted;
        if (arguments.hasNext()) argument = arguments.next();
      }
    }
    return null;
  }

  /**
   * Please always call {@link CorrespondedSubExprVisitor#visitDefCallArguments(DefCallExpression, List)}
   * instead.
   */
  private @Nullable Pair<@NotNull Expression, Concrete.@NotNull Expression>
  visitNonClassCallDefCallArguments(@NotNull DefCallExpression expression,
                                    @NotNull Iterator<Concrete.Argument> arguments) {
    Iterator<? extends Expression> defCallArgs = expression.getDefCallArguments().iterator();
    Concrete.Argument argument = arguments.next();
    for (DependentLink parameter = expression.getDefinition().getParameters();
         parameter.hasNext();
         parameter = parameter.getNext()) {
      assert defCallArgs.hasNext();
      Expression coreArg = defCallArgs.next();
      // Take care of implicit application
      if (parameter.isExplicit() == argument.isExplicit()) {
        Pair<Expression, Concrete.Expression> accepted = argument.getExpression().accept(this, coreArg);
        if (accepted != null) return accepted;
        if (arguments.hasNext()) argument = arguments.next();
      }
    }
    return null;
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitApp(Concrete.AppExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return new Pair<>(coreExpr, expr);
    if (subExpr instanceof Concrete.AppExpression && Objects.equals(
        ((Concrete.AppExpression) subExpr).getFunction().getData(),
        expr.getFunction().getData()
    )) return new Pair<>(coreExpr, expr);
    if (subExpr instanceof Concrete.ReferenceExpression && Objects.equals(
        subExpr.getData(),
        expr.getFunction().getData()
    )) return new Pair<>(coreExpr, expr);
    Concrete.Expression cloned = Concrete.AppExpression.make(expr.getData(), expr.getFunction(), new ArrayList<>(expr.getArguments()));
    return visitClonedApp(((Concrete.AppExpression) cloned), coreExpr);
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitLam(Concrete.LamExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return new Pair<>(coreExpr, expr);
    Expression body = coreExpr;
    for (Concrete.Parameter parameter : expr.getParameters()) {
      if (body instanceof LamExpression) {
        LamExpression coreLamExpr = (LamExpression) body;
        Concrete.Expression type = parameter.getType();
        if (type != null) {
          Pair<Expression, Concrete.Expression> ty = type.accept(this, coreLamExpr.getParameters().getTypeExpr());
          if (ty != null) return ty;
        }
        body = coreLamExpr.getBody();
      } else return null;
    }
    return expr.getBody().accept(this, body);
  }

  protected Pair<Expression, Concrete.Expression> visitParameter(Concrete.Parameter parameter, DependentLink link) {
    Concrete.Expression type = parameter.getType();
    if (type == null) return null;
    return type.accept(this, link.getTypeExpr());
  }

  private @Nullable Pair<@NotNull Expression, Concrete.@NotNull Expression>
  visitPiImpl(List<? extends Concrete.Parameter> parameters,
              @NotNull Concrete.Expression codomain,
              @NotNull PiExpression corePi) {
    for (Concrete.Parameter parameter : parameters) {
      DependentLink link = corePi.getParameters();
      Pair<Expression, Concrete.Expression> expression = visitParameter(parameter, link);
      if (expression != null) return expression;
      Expression corePiCodomain = corePi.getCodomain();
      if (corePiCodomain instanceof PiExpression)
        corePi = (PiExpression) corePiCodomain;
      else return codomain.accept(this, corePiCodomain);
    }
    return null;
  }

  @Nullable Pair<@NotNull Expression, Concrete.@NotNull Expression>
  visitSigmaParameters(List<? extends Concrete.Parameter> parameters, DependentLink sig) {
    for (Concrete.Parameter parameter : parameters) {
      Pair<Expression, Concrete.Expression> expression = visitParameter(parameter, sig);
      if (expression != null) return expression;
      sig = sig.getNextTyped(null).getNext();
    }
    return null;
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitPi(Concrete.PiExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return new Pair<>(coreExpr, expr);
    PiExpression corePiExpr = coreExpr.cast(PiExpression.class);
    if (corePiExpr == null) return null;
    return visitPiImpl(expr.getParameters(), expr.getCodomain(), corePiExpr);
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitSigma(Concrete.SigmaExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return new Pair<>(coreExpr, expr);
    SigmaExpression coreSigmaExpr = coreExpr.cast(SigmaExpression.class);
    if (coreSigmaExpr == null) return null;
    return visitSigmaParameters(expr.getParameters(), coreSigmaExpr.getParameters());
  }

  @Nullable Pair<@NotNull Expression, Concrete.@NotNull Expression> visitElimTree(
      List<Concrete.FunctionClause> clauses,
      List<? extends ElimClause<Pattern>> coreClauses
  ) {
    // Interval pattern matching are stored in a special way,
    // maybe it's a TODO to implement it.
    if (clauses.size() != coreClauses.size()) return null;
    for (int i = 0; i < clauses.size(); i++) {
      Concrete.FunctionClause clause = clauses.get(i);
      ElimClause<Pattern> coreClause = coreClauses.get(i);
      Concrete.Expression expression = clause.getExpression();
      if (expression == null) return null;
      Pair<Expression, Concrete.Expression> clauseVisited = expression.accept(this, coreClause.getExpression());
      if (clauseVisited != null) return clauseVisited;
    }
    return null;
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitCase(Concrete.CaseExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return new Pair<>(coreExpr, expr);
    CaseExpression coreCaseExpr = coreExpr.cast(CaseExpression.class);
    if (coreCaseExpr == null) return null;
    Pair<Expression, Concrete.Expression> expression = visitExprs(coreCaseExpr.getArguments(), expr
        .getArguments()
        .stream()
        .map(i -> i.expression)
        .collect(Collectors.toList()));
    if (expression != null) return expression;
    Concrete.Expression resultType = expr.getResultType();
    if (resultType != null) {
      Pair<Expression, Concrete.Expression> accepted = resultType.accept(this, coreCaseExpr.getResultType());
      if (accepted != null) return accepted;
    }
    return visitElimTree(expr.getClauses(), coreCaseExpr.getElimBody().getClauses());
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitEval(Concrete.EvalExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return new Pair<>(coreExpr, expr);
    return expr.getExpression().accept(this, coreExpr);
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitClassExt(Concrete.ClassExtExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return new Pair<>(coreExpr, expr);
    ClassCallExpression coreClassExpr = coreExpr.cast(ClassCallExpression.class);
    if (coreClassExpr == null) return null;
    Map<ClassField, Expression> implementedHere = coreClassExpr.getImplementedHere();
    for (Concrete.ClassFieldImpl statement : expr.getStatements()) {
      Pair<Expression, Concrete.Expression> field = visitStatement(implementedHere, statement);
      if (field != null) return field;
    }
    return expr.getBaseClassExpression().accept(this, coreClassExpr.getThisBinding().getTypeExpr());
  }

  @Nullable Pair<@NotNull Expression, Concrete.@NotNull Expression>
  visitStatement(@NotNull Map<ClassField, Expression> implementedHere, @NotNull Concrete.ClassFieldImpl statement) {
    Referable implementedField = statement.getImplementedField();
    if (implementedField == null) return null;
    // Class extension -- base class call.
    if (implementedField instanceof ClassReferable) {
      Collection<? extends FieldReferable> fields = ((ClassReferable) implementedField).getFieldReferables();
      Optional<Pair<Expression, Concrete.Expression>> baseClassCall = implementedHere.entrySet()
              .stream()
              // The suppressed warning presents here, but it's considered safe.
              .filter(entry -> fields.contains(entry.getKey().getReferable()))
              .map(Map.Entry::getValue)
              .filter(expr -> expr instanceof FieldCallExpression)
              .findFirst()
              .map(expr -> ((FieldCallExpression) expr))
              .map(e -> statement.implementation.accept(this, e.getArgument()));
      if (baseClassCall.isPresent()) return baseClassCall.get();
    }
    return implementedHere.entrySet()
        .stream()
        .filter(entry -> entry.getKey().getReferable() == implementedField)
        .findFirst()
        .map(Map.Entry::getValue)
        .map(e -> statement.implementation.accept(this, e))
        .orElse(null);
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitBinOpSequence(Concrete.BinOpSequenceExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return new Pair<>(coreExpr, expr);
    throw new IllegalStateException("BinOpSequence shouldn't appear");
  }
}
