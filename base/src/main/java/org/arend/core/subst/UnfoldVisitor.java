package org.arend.core.subst;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.EvaluatingBinding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.*;
import org.arend.core.expr.let.HaveClause;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.ext.core.level.LevelSubstitution;
import org.arend.ext.variable.Variable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UnfoldVisitor extends SubstVisitor {
  private final Set<? extends Variable> myVariables;
  private final Set<Variable> myUnfolded;
  private final boolean myUnfoldLet;
  private final UnfoldFields myUnfoldFields;
  private final Set<HaveClause> myBoundLetVars = new HashSet<>();

  public enum UnfoldFields { ALL_FIELDS, ONLY_PARAMETERS, ONLY_SPECIFIED }

  public UnfoldVisitor(Set<? extends Variable> variables, Set<Variable> unfolded, boolean unfoldLet, UnfoldFields unfoldFields) {
    super(new ExprSubstitution(), LevelSubstitution.EMPTY);
    myVariables = variables;
    myUnfolded = unfolded;
    myUnfoldLet = unfoldLet;
    myUnfoldFields = unfoldFields;
  }

  @Override
  public boolean isEmpty() {
    return !myUnfoldLet && myUnfoldFields == UnfoldFields.ONLY_SPECIFIED && myVariables.isEmpty() && super.isEmpty();
  }

  private boolean unfoldBinding(Binding binding) {
    return binding instanceof HaveClause && !myBoundLetVars.contains(binding);
  }

  @Override
  public Expression visitReference(ReferenceExpression expr, Void params) {
    if (expr.getBinding() instanceof EvaluatingBinding && (myVariables.contains(expr.getBinding()) || myUnfoldFields == UnfoldFields.ALL_FIELDS && unfoldBinding(expr.getBinding()))) {
      if (myUnfolded != null) {
        myUnfolded.add(expr.getBinding());
      }
      return ((EvaluatingBinding) expr.getBinding()).getExpression();
    } else {
      return super.visitReference(expr, params);
    }
  }

  @Override
  public Expression visitFunCall(FunCallExpression expr, Void params) {
    if (expr.getDefinition().getActualBody() instanceof Expression && myVariables.contains(expr.getDefinition())) {
      if (myUnfolded != null) {
        myUnfolded.add(expr.getDefinition());
      }
      List<Expression> newArgs = new ArrayList<>(expr.getDefCallArguments().size());
      for (Expression argument : expr.getDefCallArguments()) {
        newArgs.add(argument.accept(this, null));
      }

      ExprSubstitution substitution = getExprSubstitution();
      substitution.add(expr.getDefinition().getParameters(), newArgs);
      Expression result = ((Expression) expr.getDefinition().getActualBody()).accept(new SubstVisitor(substitution, expr.getLevelSubstitution().subst(getLevelSubstitution())), null);
      DependentLink.Helper.freeSubsts(expr.getDefinition().getParameters(), substitution);
      return result;
    } else {
      return super.visitFunCall(expr, params);
    }
  }

  @Override
  public Expression visitFieldCall(FieldCallExpression expr, Void params) {
    if (!expr.getDefinition().isProperty() && (myUnfoldFields == UnfoldFields.ALL_FIELDS || myUnfoldFields == UnfoldFields.ONLY_PARAMETERS && expr.getDefinition().getReferable().isParameterField() || myVariables.contains(expr.getDefinition()))) {
      Expression result = NormalizeVisitor.INSTANCE.evalFieldCall(expr.getDefinition(), expr.getArgument());
      if (result != null) {
        if (myUnfolded != null) {
          myUnfolded.add(expr.getDefinition());
        }
        return myUnfoldFields == UnfoldFields.ONLY_PARAMETERS ? result.accept(this, null) : result.subst(getExprSubstitution(), getLevelSubstitution());
      }
    }
    return super.visitFieldCall(expr, params);
  }

  @Override
  public Expression visitLet(LetExpression let, Void params) {
    if (myUnfoldLet) {
      ExprSubstitution substitution = new ExprSubstitution();
      for (HaveClause clause : let.getClauses()) {
        substitution.add(clause, clause.getExpression().accept(this, null).subst(substitution));
        myBoundLetVars.add(clause);
      }
      Expression result = let.getExpression().accept(this, null).subst(substitution);
      for (HaveClause clause : let.getClauses()) {
        myBoundLetVars.remove(clause);
      }
      return result;
    }
    return super.visitLet(let, null);
  }
}
