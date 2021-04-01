package org.arend.core.expr.visitor;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.EvaluatingBinding;
import org.arend.core.context.param.UnusedIntervalDependentLink;
import org.arend.ext.variable.Variable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.elimtree.ElimBody;
import org.arend.core.elimtree.ElimClause;
import org.arend.core.expr.*;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.type.TypeExpression;
import org.arend.core.pattern.Pattern;
import org.arend.core.subst.ExprSubstitution;
import org.arend.ext.core.ops.NormalizationMode;

import java.util.*;

public class ElimBindingVisitor extends ExpressionTransformer<Void> {
  private final FindMissingBindingVisitor myKeepVisitor;
  private final FindBindingVisitor myElimVisitor;
  private Variable myFoundVariable = null;

  private ElimBindingVisitor(Set<Binding> bindings, boolean keep) {
    myKeepVisitor = keep ? new FindMissingBindingVisitor(new HashSet<>(bindings)) : null;
    myElimVisitor = keep ? null : new FindBindingVisitor(bindings);
  }

  public Variable getFoundVariable() {
    return myFoundVariable;
  }

  public static Expression elimLamBinding(LamExpression expr) {
    if (expr == null) {
      return null;
    }
    Expression result = expr.getParameters().getNext().hasNext() ? new LamExpression(expr.getResultSort(), expr.getParameters().getNext(), expr.getBody()) : expr.getBody();
    return expr.getParameters() == UnusedIntervalDependentLink.INSTANCE ? result : elimBinding(result, expr.getParameters());
  }

  public static Expression elimBinding(Expression expression, Binding binding) {
    ElimBindingVisitor visitor = new ElimBindingVisitor(Collections.singleton(binding), false);
    visitor.myFoundVariable = expression.accept(visitor.myElimVisitor, null) ? visitor.myElimVisitor.getResult() : null;
    return visitor.myFoundVariable == null ? expression : expression.normalize(NormalizationMode.WHNF).accept(visitor, null);
  }

  public static Expression keepBindings(Expression expression, Set<Binding> bindings, boolean removeImplementations) {
    ElimBindingVisitor visitor = new ElimBindingVisitor(bindings, true);
    visitor.myFoundVariable = expression.accept(visitor.myKeepVisitor, null) ? visitor.myKeepVisitor.getResult() : null;
    if (visitor.myFoundVariable == null) {
      return expression;
    }

    expression = expression.normalize(NormalizationMode.WHNF);
    if (removeImplementations) {
      ClassCallExpression classCall = expression.cast(ClassCallExpression.class);
      if (classCall != null) {
        return visitor.visitClassCall(classCall, true);
      }
    }

    return expression.accept(visitor, null);
  }

  private boolean findVars(Expression expression) {
    myFoundVariable = expression.accept(myKeepVisitor != null ? myKeepVisitor : myElimVisitor, null) ? (myKeepVisitor != null ? myKeepVisitor.getResult() : myElimVisitor.getResult()) : null;
    return myFoundVariable != null;
  }

  private Expression acceptSelf(Expression expression, boolean normalize) {
    return !findVars(expression) ? expression : (normalize ? expression.normalize(NormalizationMode.WHNF) : expression).accept(this, null);
  }

  @Override
  public Expression visitApp(AppExpression expr, Void params) {
    Expression result = acceptSelf(expr.getFunction(), false);
    if (result == null) {
      return null;
    }
    Expression arg = acceptSelf(expr.getArgument(), true);
    if (arg == null) {
      return null;
    }
    return AppExpression.make(result, arg, expr.isExplicit());
  }

  private boolean visitDefCallArguments(List<? extends Expression> args, List<Expression> result) {
    for (Expression arg : args) {
      Expression newArg = acceptSelf(arg, true);
      if (newArg == null) {
        return false;
      }
      result.add(newArg);
    }
    return true;
  }

  private List<Expression> visitDefCallArguments(List<? extends Expression> args) {
    List<Expression> result = new ArrayList<>(args.size());
    return visitDefCallArguments(args, result) ? result : null;
  }

  @Override
  public Expression visitDefCall(DefCallExpression expr, Void params) {
    List<Expression> newArgs = visitDefCallArguments(expr.getDefCallArguments());
    return newArgs == null ? null : expr.getDefinition().getDefCall(expr.getSortArgument(), newArgs);
  }

  @Override
  public Expression visitFieldCall(FieldCallExpression expr, Void params) {
    Expression newExpr = acceptSelf(expr.getArgument(), false);
    return newExpr == null ? null : FieldCallExpression.make(expr.getDefinition(), expr.getSortArgument(), newExpr);
  }

  @Override
  protected Expression visit(Expression expr, Void params) {
    return acceptSelf(expr, true);
  }

  public ClassCallExpression visitClassCall(ClassCallExpression expr, boolean removeImplementations) {
    Map<ClassField, Expression> newFieldSet = new HashMap<>();
    ClassCallExpression result = new ClassCallExpression(expr.getDefinition(), expr.getSortArgument(), newFieldSet, expr.getSort(), expr.getUniverseKind());
    if (myKeepVisitor != null) {
      myKeepVisitor.getBindings().add(expr.getThisBinding());
    }
    for (Map.Entry<ClassField, Expression> entry : expr.getImplementedHere().entrySet()) {
      Expression newImpl = acceptSelf(entry.getValue(), true);
      if (newImpl == null) {
        if (removeImplementations) {
          continue;
        } else {
          if (myKeepVisitor != null) {
            myKeepVisitor.getBindings().remove(expr.getThisBinding());
          }
          return null;
        }
      }
      newFieldSet.put(entry.getKey(), newImpl.subst(expr.getThisBinding(), new ReferenceExpression(result.getThisBinding())));
    }
    if (myKeepVisitor != null) {
      myKeepVisitor.getBindings().remove(expr.getThisBinding());
    }

    result.setSort(result.getDefinition().computeSort(result.getSortArgument(), result.getImplementedHere(), result.getThisBinding()));
    result.updateHasUniverses();
    return result;
  }

  @Override
  public ClassCallExpression visitClassCall(ClassCallExpression expr, Void params) {
    return visitClassCall(expr, false);
  }

  @Override
  public Expression visitReference(ReferenceExpression expr, Void params) {
    if (myKeepVisitor != null ? !myKeepVisitor.getBindings().contains(expr.getBinding()) : myElimVisitor.getBindings().contains(expr.getBinding())) {
      if (expr.getBinding() instanceof EvaluatingBinding) {
        Expression e = ((EvaluatingBinding) expr.getBinding()).getExpression();
        return findVars(e) ? e.accept(this, null) : expr;
      }
      myFoundVariable = expr.getBinding();
      return null;
    } else {
      return expr;
    }
  }

  @Override
  public Expression visitInferenceReference(InferenceReferenceExpression expr, Void params) {
    if (expr.getSubstExpression() != null) {
      return acceptSelf(expr.getSubstExpression(), true);
    }
    if (myKeepVisitor != null) {
      expr.getVariable().getBounds().retainAll(myKeepVisitor.getBindings());
    }
    return expr;
  }

  @Override
  public Expression visitSubst(SubstExpression expr, Void params) {
    if (myKeepVisitor != null) {
      if (expr.isInferenceVariable()) {
        ((InferenceReferenceExpression) expr.getExpression()).getVariable().getBounds().removeIf(bound -> !myKeepVisitor.getBindings().contains(bound) && !expr.getSubstitution().getKeys().contains(bound));
        ExprSubstitution substitution = new ExprSubstitution();
        for (Map.Entry<Binding, Expression> entry : expr.getSubstitution().getEntries()) {
          substitution.add(entry.getKey(), entry.getValue().accept(this, null));
        }
        return SubstExpression.make(expr, substitution, expr.getLevelSubstitution());
      } else {
        return expr.getSubstExpression().accept(this, null);
      }
    } else {
      ExprSubstitution substitution = new ExprSubstitution();
      for (Map.Entry<Binding, Expression> entry : expr.getSubstitution().getEntries()) {
        substitution.add(entry.getKey(), entry.getValue().accept(this, null));
      }
      return SubstExpression.make(expr.getExpression().accept(this, null), substitution, expr.getLevelSubstitution());
    }
  }

  @Override
  public LamExpression visitLam(LamExpression expr, Void params) {
    boolean isUnused = expr.getParameters() == UnusedIntervalDependentLink.INSTANCE;
    SingleDependentLink oldParameters = isUnused ? expr.getParameters().getNext() : expr.getParameters();
    Expression body;
    if (oldParameters.hasNext()) {
      ExprSubstitution substitution = new ExprSubstitution();
      SingleDependentLink parameters = DependentLink.Helper.subst(oldParameters, substitution);
      if (!visitDependentLink(parameters)) {
        return null;
      }
      body = acceptSelf(expr.getBody().subst(substitution), true);
      if (myKeepVisitor != null) {
        myKeepVisitor.freeParameters(parameters);
      }
      if (body == null) {
        return null;
      }
      body = new LamExpression(expr.getResultSort(), parameters, body);
    } else {
      body = acceptSelf(expr.getBody(), true);
      if (body == null) {
        return null;
      }
    }
    return isUnused ? new LamExpression(expr.getResultSort(), UnusedIntervalDependentLink.INSTANCE, body) : (LamExpression) body;
  }

  @Override
  public PiExpression visitPi(PiExpression expr, Void params) {
    ExprSubstitution substitution = new ExprSubstitution();
    SingleDependentLink parameters = DependentLink.Helper.subst(expr.getParameters(), substitution);
    if (!visitDependentLink(parameters)) {
      return null;
    }
    Expression codomain = acceptSelf(expr.getCodomain().subst(substitution), true);
    if (myKeepVisitor != null) {
      myKeepVisitor.freeParameters(parameters);
    }
    if (codomain == null) {
      return null;
    }
    return new PiExpression(expr.getResultSort(), parameters, codomain);
  }

  @Override
  public UniverseExpression visitUniverse(UniverseExpression expr, Void params) {
    return expr;
  }

  @Override
  public ErrorExpression visitError(ErrorExpression expr, Void params) {
    if (expr.getExpression() == null) {
      return expr;
    }
    Expression errorExpr = acceptSelf(expr.getExpression(), true);
    if (errorExpr == null) {
      myFoundVariable = null;
      return expr.replaceExpression(null);
    } else {
      return expr.replaceExpression(errorExpr);
    }
  }

  @Override
  public TupleExpression visitTuple(TupleExpression expr, Void params) {
    List<Expression> newFields = new ArrayList<>(expr.getFields().size());
    for (Expression field : expr.getFields()) {
      Expression newField = acceptSelf(field, true);
      if (newField == null) {
        return null;
      }
      newFields.add(newField);
    }

    SigmaExpression sigmaExpr = (SigmaExpression) acceptSelf(expr.getSigmaType(), false);
    return sigmaExpr == null ? null : new TupleExpression(newFields, sigmaExpr);
  }

  @Override
  public SigmaExpression visitSigma(SigmaExpression expr, Void params) {
    DependentLink parameters = DependentLink.Helper.copy(expr.getParameters());
    if (visitDependentLink(parameters)) {
      if (myKeepVisitor != null) {
        myKeepVisitor.freeParameters(parameters);
      }
      return new SigmaExpression(expr.getSort(), parameters);
    } else {
      return null;
    }
  }

  @Override
  public Expression visitProj(ProjExpression expr, Void params) {
    Expression newExpr = acceptSelf(expr.getExpression(), false);
    return newExpr == null ? null : ProjExpression.make(newExpr, expr.getField());
  }

  private boolean visitDependentLink(DependentLink parameters) {
    for (DependentLink link = parameters; link.hasNext(); link = link.getNext()) {
      DependentLink link1 = link.getNextTyped(null);
      Expression type = acceptSelf(link1.getTypeExpr(), true);
      if (type == null) {
        if (myKeepVisitor != null) {
          for (; parameters != link; parameters = parameters.getNext()) {
            myKeepVisitor.getBindings().remove(parameters);
          }
        }
        return false;
      }
      link1.setType(type instanceof Type ? (Type) type : new TypeExpression(type, link1.getType().getSortOfType()));

      if (myKeepVisitor != null) {
        for (; link != link1; link = link.getNext()) {
          myKeepVisitor.getBindings().add(link);
        }
        myKeepVisitor.getBindings().add(link);
      }
    }
    return true;
  }

  @Override
  public NewExpression visitNew(NewExpression expr, Void params) {
    ClassCallExpression classCall = visitClassCall(expr.getClassCall(), false);
    if (classCall == null) {
      return null;
    }

    Expression renew = expr.getRenewExpression();
    if (renew == null) {
      return new NewExpression(null, classCall);
    }
    renew = acceptSelf(renew, true);
    if (renew == null) {
      return null;
    }
    return new NewExpression(renew, classCall);
  }

  @Override
  public Expression visitPEval(PEvalExpression expr, Void params) {
    Expression result = expr.getExpression().accept(this, null);
    return result != null ? new PEvalExpression(result) : null;
  }

  @Override
  public LetExpression visitLet(LetExpression letExpression, Void params) {
    throw new IllegalStateException();
    /*
    List<LetClause> newClauses = new ArrayList<>(letExpression.getClauses().size());
    for (LetClause clause : letExpression.getClauses()) {
      Expression newClauseExpr = findBindings(clause.getExpression(), true);
      if (newClauseExpr == null) {
        return null;
      }
      newClauses.add(new LetClause(clause.getName(), newClauseExpr));
    }

    Expression newExpr = findBindings(letExpression.getExpression(), true);
    return newExpr == null ? null : new LetExpression(newClauses, newExpr);
    */
  }

  @Override
  public CaseExpression visitCase(CaseExpression expr, Void params) {
    List<Expression> newArgs = new ArrayList<>(expr.getArguments().size());
    for (Expression argument : expr.getArguments()) {
      Expression newArg = acceptSelf(argument, true);
      if (newArg == null) {
        return null;
      }
      newArgs.add(newArg);
    }

    ExprSubstitution substitution = new ExprSubstitution();
    DependentLink parameters = DependentLink.Helper.subst(expr.getParameters(), substitution);
    if (!visitDependentLink(parameters)) {
      return null;
    }

    Expression newType = acceptSelf(expr.getResultType().subst(substitution), true);
    if (newType == null) {
      return null;
    }

    Expression newTypeLevel;
    if (expr.getResultTypeLevel() != null) {
      newTypeLevel = acceptSelf(expr.getResultTypeLevel().subst(substitution), true);
      if (newTypeLevel == null) {
        return null;
      }
    } else {
      newTypeLevel = null;
    }

    if (myKeepVisitor != null) {
      myKeepVisitor.freeParameters(parameters);
    }

    List<ElimClause<Pattern>> clauses = new ArrayList<>();
    for (ElimClause<Pattern> clause : expr.getElimBody().getClauses()) {
      ExprSubstitution clauseSubst = new ExprSubstitution();
      DependentLink clauseParams = DependentLink.Helper.subst(Pattern.getFirstBinding(clause.getPatterns()), clauseSubst);
      if (!visitDependentLink(clauseParams)) {
        return null;
      }
      Expression newExpr;
      if (clause.getExpression() != null) {
        newExpr = acceptSelf(clause.getExpression(), true);
        if (newExpr == null) {
          return null;
        }
      } else {
        newExpr = null;
      }
      clauses.add(new ElimClause<>(Pattern.replaceBindings(clause.getPatterns(), clauseParams), newExpr));
    }

    return new CaseExpression(expr.isSCase(), parameters, newType, newTypeLevel, new ElimBody(clauses, expr.getElimBody().getElimTree()), newArgs);
  }

  @Override
  public OfTypeExpression visitOfType(OfTypeExpression expr, Void params) {
    Expression newExpr = acceptSelf(expr.getExpression(), true);
    if (newExpr == null) {
      return null;
    }
    Expression newType = acceptSelf(expr.getTypeOf(), true);
    return newType == null ? null : new OfTypeExpression(newExpr, newType);
  }

  @Override
  public Expression visitInteger(IntegerExpression expr, Void params) {
    return expr;
  }

  @Override
  public Expression visitTypeCoerce(TypeCoerceExpression expr, Void params) {
    List<Expression> newArgs = visitDefCallArguments(expr.getClauseArguments());
    if (newArgs == null) return null;
    Expression newArg = expr.getArgument().accept(this, null);
    return newArg == null ? null : TypeCoerceExpression.make(expr.getDefinition(), expr.getSortArgument(), expr.getClauseIndex(), newArgs, newArg, expr.isFromLeftToRight());
  }
}
