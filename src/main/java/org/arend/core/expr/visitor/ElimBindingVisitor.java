package org.arend.core.expr.visitor;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.EvaluatingBinding;
import org.arend.core.context.binding.Variable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.elimtree.BranchElimTree;
import org.arend.core.elimtree.ElimTree;
import org.arend.core.elimtree.LeafElimTree;
import org.arend.core.expr.*;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.type.TypeExpression;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.core.subst.SubstVisitor;
import org.arend.ext.core.elimtree.CoreBranchKey;
import org.arend.ext.core.ops.NormalizationMode;

import java.util.*;

public class ElimBindingVisitor extends BaseExpressionVisitor<Void, Expression> {
  private final FindMissingBindingVisitor myKeepVisitor;
  private final FindBindingVisitor myElimVisitor;
  private Variable myFoundVariable = null;

  private ElimBindingVisitor(Set<Binding> bindings, boolean keep) {
    myKeepVisitor = keep ? new FindMissingBindingVisitor(bindings) : null;
    myElimVisitor = keep ? null : new FindBindingVisitor(bindings);
  }

  public Variable getFoundVariable() {
    return myFoundVariable;
  }

  public static Expression elimLamBinding(LamExpression expr) {
    return expr == null ? null : elimBinding(expr.getParameters().getNext().hasNext() ? new LamExpression(expr.getResultSort(), expr.getParameters().getNext(), expr.getBody()) : expr.getBody(), expr.getParameters());
  }

  public static Expression elimBinding(Expression expression, Binding binding) {
    ElimBindingVisitor visitor = new ElimBindingVisitor(Collections.singleton(binding), false);
    visitor.myFoundVariable = expression.accept(visitor.myElimVisitor, null);
    return visitor.myFoundVariable == null ? expression : expression.normalize(NormalizationMode.WHNF).accept(visitor, null);
  }

  public static Expression keepBindings(Expression expression, Set<Binding> bindings, boolean removeImplementations) {
    ElimBindingVisitor visitor = new ElimBindingVisitor(bindings, true);
    visitor.myFoundVariable = expression.accept(visitor.myKeepVisitor, null);
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

  private Expression acceptSelf(Expression expression, boolean normalize) {
    myFoundVariable = expression.accept(myKeepVisitor != null ? myKeepVisitor : myElimVisitor, null);
    if (myFoundVariable == null) {
      return expression;
    }
    return (normalize ? expression.normalize(NormalizationMode.WHNF) : expression).accept(this, null);
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
    return AppExpression.make(result, arg);
  }

  private List<Expression> visitDefCallArguments(List<? extends Expression> args) {
    List<Expression> result = new ArrayList<>(args.size());
    for (Expression arg : args) {
      Expression newArg = acceptSelf(arg, true);
      if (newArg == null) {
        return null;
      }
      result.add(newArg);
    }
    return result;
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
  public Expression visitConCall(ConCallExpression expr, Void params) {
    List<Expression> newArgs = visitDefCallArguments(expr.getDefCallArguments());
    if (newArgs == null) {
      return null;
    }
    List<Expression> dataTypeArgs = new ArrayList<>(expr.getDataTypeArguments().size());
    for (Expression arg : expr.getDataTypeArguments()) {
      Expression newArg = acceptSelf(arg, true);
      if (newArg == null) {
        return null;
      }
      dataTypeArgs.add(newArg);
    }
    return ConCallExpression.make(expr.getDefinition(), expr.getSortArgument(), dataTypeArgs, newArgs);
  }

  public ClassCallExpression visitClassCall(ClassCallExpression expr, boolean removeImplementations) {
    Map<ClassField, Expression> newFieldSet = new HashMap<>();
    ClassCallExpression result = new ClassCallExpression(expr.getDefinition(), expr.getSortArgument(), newFieldSet, expr.getSort(), expr.hasUniverses());
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

    result.setSort(result.getDefinition().computeSort(result.getImplementedHere(), result.getThisBinding()));
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
        return ((EvaluatingBinding) expr.getBinding()).getExpression().accept(this, null);
      }
      myFoundVariable = expr.getBinding();
      return null;
    } else {
      return expr;
    }
  }

  @Override
  public Expression visitInferenceReference(InferenceReferenceExpression expr, Void params) {
    return expr.getSubstExpression() != null ? acceptSelf(expr.getSubstExpression(), true) : expr;
  }

  @Override
  public Expression visitSubst(SubstExpression expr, Void params) {
    return expr.getSubstExpression().accept(this, null);
  }

  @Override
  public LamExpression visitLam(LamExpression expr, Void params) {
    ExprSubstitution substitution = new ExprSubstitution();
    SingleDependentLink parameters = DependentLink.Helper.subst(expr.getParameters(), substitution);
    if (!visitDependentLink(parameters)) {
      return null;
    }
    Expression body = acceptSelf(expr.getBody().subst(substitution), true);
    if (myKeepVisitor != null) {
      myKeepVisitor.freeParameters(parameters);
    }
    if (body == null) {
      return null;
    }
    return new LamExpression(expr.getResultSort(), parameters, body);
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
      return new ErrorExpression(null, expr.isGoal());
    } else {
      return new ErrorExpression(errorExpr, expr.isGoal());
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

    ElimTree newElimTree = findBindingInElimTree(expr.getElimTree());
    if (myKeepVisitor != null) {
      myKeepVisitor.freeParameters(parameters);
    }
    return newElimTree == null ? null : new CaseExpression(expr.isSCase(), parameters, newType, newTypeLevel, newElimTree, newArgs);
  }

  private ElimTree findBindingInElimTree(ElimTree elimTree) {
    ExprSubstitution substitution = new ExprSubstitution();
    DependentLink parameters = DependentLink.Helper.subst(elimTree.getParameters(), substitution);
    if (!visitDependentLink(parameters)) {
      return null;
    }

    if (elimTree instanceof LeafElimTree) {
      Expression newExpr = acceptSelf(((LeafElimTree) elimTree).getExpression().subst(substitution), true);
      if (myKeepVisitor != null) {
        myKeepVisitor.freeParameters(parameters);
      }
      return newExpr == null ? null : new LeafElimTree(parameters, newExpr);
    } else {
      Map<CoreBranchKey, ElimTree> newChildren = new HashMap<>();
      SubstVisitor visitor = new SubstVisitor(substitution, LevelSubstitution.EMPTY);
      for (Map.Entry<CoreBranchKey, ElimTree> entry : ((BranchElimTree) elimTree).getChildren()) {
        ElimTree newElimTree = findBindingInElimTree(visitor.substElimTree(entry.getValue()));
        if (newElimTree == null) {
          if (myKeepVisitor != null) {
            myKeepVisitor.freeParameters(parameters);
          }
          return null;
        }
        newChildren.put(entry.getKey(), newElimTree);
      }

      if (myKeepVisitor != null) {
        myKeepVisitor.freeParameters(parameters);
      }
      return new BranchElimTree(parameters, newChildren);
    }
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
}
