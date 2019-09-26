package org.arend.core.expr.visitor;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.EvaluatingBinding;
import org.arend.core.context.binding.Variable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.Constructor;
import org.arend.core.elimtree.BranchElimTree;
import org.arend.core.elimtree.ElimTree;
import org.arend.core.elimtree.LeafElimTree;
import org.arend.core.expr.*;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.type.TypeExpression;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.core.subst.SubstVisitor;

import java.util.*;

public class ElimBindingVisitor extends BaseExpressionVisitor<Void, Expression> {
  private final FindMissingBindingVisitor myVisitor;
  private Variable myFoundVariable = null;

  private ElimBindingVisitor(Set<Binding> bindings) {
    myVisitor = new FindMissingBindingVisitor(bindings);
  }

  public Variable getFoundVariable() {
    return myFoundVariable;
  }

  public static Expression findBindings(Expression expression, Set<Binding> bindings, boolean removeImplementations) {
    ElimBindingVisitor visitor = new ElimBindingVisitor(bindings);
    visitor.myFoundVariable = expression.accept(visitor.myVisitor, null);
    if (visitor.myFoundVariable == null) {
      return expression;
    }

    if (removeImplementations) {
      ClassCallExpression classCall = expression.checkedCast(ClassCallExpression.class);
      if (classCall != null) {
        return visitor.visitClassCall(classCall, true);
      }
    }

    return expression.normalize(NormalizeVisitor.Mode.WHNF).accept(visitor, null);
  }

  private Expression findBindings(Expression expression, boolean normalize) {
    myFoundVariable = expression.accept(myVisitor, null);
    if (myFoundVariable == null) {
      return expression;
    }
    return (normalize ? expression.normalize(NormalizeVisitor.Mode.WHNF) : expression).accept(this, null);
  }

  @Override
  public Expression visitApp(AppExpression expr, Void params) {
    Expression result = findBindings(expr.getFunction(), false);
    if (result == null) {
      return null;
    }
    Expression arg = findBindings(expr.getArgument(), true);
    if (arg == null) {
      return null;
    }
    return AppExpression.make(result, arg);
  }

  private List<Expression> visitDefCallArguments(List<? extends Expression> args) {
    List<Expression> result = new ArrayList<>(args.size());
    for (Expression arg : args) {
      Expression newArg = findBindings(arg, true);
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
    Expression newExpr = findBindings(expr.getArgument(), false);
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
      Expression newArg = findBindings(arg, true);
      if (newArg == null) {
        return null;
      }
      dataTypeArgs.add(newArg);
    }
    return ConCallExpression.make(expr.getDefinition(), expr.getSortArgument(), dataTypeArgs, newArgs);
  }

  public ClassCallExpression visitClassCall(ClassCallExpression expr, boolean removeImplementations) {
    Map<ClassField, Expression> newFieldSet = new HashMap<>();
    for (Map.Entry<ClassField, Expression> entry : expr.getImplementedHere().entrySet()) {
      Expression newImpl = findBindings(entry.getValue(), true);
      if (newImpl == null) {
        if (removeImplementations) {
          continue;
        } else {
          return null;
        }
      }
      newFieldSet.put(entry.getKey(), newImpl);
    }
    return new ClassCallExpression(expr.getDefinition(), expr.getSortArgument(), newFieldSet, expr.getSort(), expr.hasUniverses());
  }

  @Override
  public ClassCallExpression visitClassCall(ClassCallExpression expr, Void params) {
    return visitClassCall(expr, false);
  }

  @Override
  public Expression visitReference(ReferenceExpression expr, Void params) {
    if (!myVisitor.getBindings().contains(expr.getBinding())) {
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
    return expr.getSubstExpression() != null ? findBindings(expr.getSubstExpression(), true) : expr;
  }

  @Override
  public LamExpression visitLam(LamExpression expr, Void params) {
    ExprSubstitution substitution = new ExprSubstitution();
    SingleDependentLink parameters = DependentLink.Helper.subst(expr.getParameters(), substitution);
    if (!visitDependentLink(parameters)) {
      return null;
    }
    Expression body = findBindings(expr.getBody().subst(substitution), true);
    myVisitor.freeParameters(parameters);
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
    Expression codomain = findBindings(expr.getCodomain().subst(substitution), true);
    myVisitor.freeParameters(parameters);
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
    Expression errorExpr = findBindings(expr.getExpression(), true);
    if (errorExpr == null) {
      myFoundVariable = null;
      return new ErrorExpression(null, expr.getError());
    } else {
      return new ErrorExpression(errorExpr, expr.getError());
    }
  }

  @Override
  public TupleExpression visitTuple(TupleExpression expr, Void params) {
    List<Expression> newFields = new ArrayList<>(expr.getFields().size());
    for (Expression field : expr.getFields()) {
      Expression newField = findBindings(field, true);
      if (newField == null) {
        return null;
      }
      newFields.add(newField);
    }

    SigmaExpression sigmaExpr = (SigmaExpression) findBindings(expr.getSigmaType(), false);
    return sigmaExpr == null ? null : new TupleExpression(newFields, sigmaExpr);
  }

  @Override
  public SigmaExpression visitSigma(SigmaExpression expr, Void params) {
    DependentLink parameters = DependentLink.Helper.copy(expr.getParameters());
    if (visitDependentLink(parameters)) {
      myVisitor.freeParameters(parameters);
      return new SigmaExpression(expr.getSort(), parameters);
    } else {
      return null;
    }
  }

  @Override
  public Expression visitProj(ProjExpression expr, Void params) {
    Expression newExpr = findBindings(expr.getExpression(), false);
    return newExpr == null ? null : ProjExpression.make(newExpr, expr.getField());
  }

  private boolean visitDependentLink(DependentLink parameters) {
    for (DependentLink link = parameters; link.hasNext(); link = link.getNext()) {
      DependentLink link1 = link.getNextTyped(null);
      Expression type = findBindings(link1.getTypeExpr(), true);
      if (type == null) {
        for (; parameters != link; parameters = parameters.getNext()) {
          myVisitor.getBindings().remove(parameters);
        }
        return false;
      }
      link1.setType(type instanceof Type ? (Type) type : new TypeExpression(type, link1.getType().getSortOfType()));

      for (; link != link1; link = link.getNext()) {
        myVisitor.getBindings().add(link);
      }
      myVisitor.getBindings().add(link);
    }
    return true;
  }

  @Override
  public NewExpression visitNew(NewExpression expr, Void params) {
    ClassCallExpression newExpr = visitClassCall(expr.getExpression(), false);
    return newExpr == null ? null : new NewExpression(newExpr);
  }

  @Override
  public Expression visitPEval(PEvalExpression expr, Void params) {
    Expression result = visitFunCall(expr.getExpression(), null);
    return result instanceof FunCallExpression ? new PEvalExpression((FunCallExpression) result) : null;
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
      Expression newArg = findBindings(argument, true);
      if (newArg == null) {
        return null;
      }
    }

    ExprSubstitution substitution = new ExprSubstitution();
    DependentLink parameters = DependentLink.Helper.subst(expr.getParameters(), substitution);
    if (!visitDependentLink(parameters)) {
      return null;
    }

    Expression newType = findBindings(expr.getResultType().subst(substitution), true);
    if (newType == null) {
      return null;
    }

    Expression newTypeLevel;
    if (expr.getResultTypeLevel() != null) {
      newTypeLevel = findBindings(expr.getResultTypeLevel().subst(substitution), true);
      if (newTypeLevel == null) {
        return null;
      }
    } else {
      newTypeLevel = null;
    }

    ElimTree newElimTree = findBindingInElimTree(expr.getElimTree());
    myVisitor.freeParameters(parameters);
    return newElimTree == null ? null : new CaseExpression(parameters, newType, newTypeLevel, newElimTree, newArgs);
  }

  private ElimTree findBindingInElimTree(ElimTree elimTree) {
    ExprSubstitution substitution = new ExprSubstitution();
    DependentLink parameters = DependentLink.Helper.subst(elimTree.getParameters(), substitution);
    if (!visitDependentLink(parameters)) {
      return null;
    }

    if (elimTree instanceof LeafElimTree) {
      Expression newExpr = findBindings(((LeafElimTree) elimTree).getExpression().subst(substitution), true);
      myVisitor.freeParameters(parameters);
      return newExpr == null ? null : new LeafElimTree(parameters, newExpr);
    } else {
      Map<Constructor, ElimTree> newChildren = new HashMap<>();
      SubstVisitor visitor = new SubstVisitor(substitution, LevelSubstitution.EMPTY);
      for (Map.Entry<Constructor, ElimTree> entry : ((BranchElimTree) elimTree).getChildren()) {
        ElimTree newElimTree = findBindingInElimTree(visitor.substElimTree(entry.getValue()));
        if (newElimTree == null) {
          myVisitor.freeParameters(parameters);
          return null;
        }
        newChildren.put(entry.getKey(), newElimTree);
      }

      myVisitor.freeParameters(parameters);
      return new BranchElimTree(parameters, newChildren);
    }
  }

  @Override
  public OfTypeExpression visitOfType(OfTypeExpression expr, Void params) {
    Expression newExpr = findBindings(expr.getExpression(), true);
    if (newExpr == null) {
      return null;
    }
    Expression newType = findBindings(expr.getTypeOf(), true);
    return newType == null ? null : new OfTypeExpression(newExpr, newType);
  }

  @Override
  public Expression visitInteger(IntegerExpression expr, Void params) {
    return expr;
  }
}
