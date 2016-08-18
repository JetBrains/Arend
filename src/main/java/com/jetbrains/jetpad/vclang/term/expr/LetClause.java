package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.context.binding.NamedBinding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.Function;
import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;

public class LetClause extends NamedBinding implements Function {
  private DependentLink myParameters;
  private ElimTreeNode myElimTree;
  private Expression myResultType;

  public LetClause(String name, DependentLink parameters, Expression resultType, ElimTreeNode elimTree) {
    super(name);
    assert parameters != null;
    myParameters = parameters;
    myResultType = resultType;
    myElimTree = elimTree;
  }

  @Override
  public ElimTreeNode getElimTree() {
    return myElimTree;
  }

  public void setElimTree(ElimTreeNode elimTree) {
    myElimTree = elimTree;
  }

  @Override
  public DependentLink getParameters() {
    return myParameters;
  }

  public void setParameters(DependentLink parameters) {
    assert parameters != null;
    myParameters = parameters;
  }

  @Override
  public Expression getResultType() {
    return myResultType;
  }

  public void setResultType(Expression resultType) {
    myResultType = resultType;
  }

  @Override
  public int getNumberOfRequiredArguments() {
    return DependentLink.Helper.size(myParameters);
  }

  @Override
  public Expression getType() {
    return myResultType.addParameters(myParameters, false);
  }

  public LetClause subst(ExprSubstitution exprSubst, LevelSubstitution levelSubst) {
    if (exprSubst.getDomain().isEmpty() && levelSubst.getDomain().isEmpty()) {
      return this;
    }

    DependentLink parameters = DependentLink.Helper.subst(myParameters, exprSubst, levelSubst);
    return new LetClause(getName(), parameters, myResultType.subst(exprSubst, levelSubst), myElimTree.subst(exprSubst, levelSubst));
  }
}
