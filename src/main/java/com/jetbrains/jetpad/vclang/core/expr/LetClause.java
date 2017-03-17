package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.binding.NamedBinding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Callable;
import com.jetbrains.jetpad.vclang.core.definition.Function;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.sort.LevelArguments;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;

import java.util.Collections;
import java.util.List;

public class LetClause extends NamedBinding implements Function, Callable {
  private List<Level> myPLevels;
  private List<SingleDependentLink> myParameters;
  private ElimTreeNode myElimTree;
  private Expression myResultType;

  public LetClause(String name, List<Level> pLevels, List<SingleDependentLink> parameters, Expression resultType, ElimTreeNode elimTree) {
    super(name);
    assert pLevels.size() == parameters.size();
    myPLevels = pLevels;
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

  public List<Level> getPLevels() {
    return myPLevels;
  }

  public List<SingleDependentLink> getParameters() {
    return myParameters;
  }

  public Expression getResultType() {
    return myResultType;
  }

  public void setResultType(Expression resultType) {
    myResultType = resultType;
  }

  @Override
  public Expression getType() {
    Expression type = myResultType;
    for (int i = 0; i < myParameters.size(); i++) {
      type = new PiExpression(Collections.singletonList(myPLevels.get(i)), myParameters.get(i), type);
    }
    return type;
  }

  @Override
  public Expression getTypeWithParams(List<DependentLink> params, LevelArguments polyArguments) {
    ExprSubstitution subst = new ExprSubstitution();
    for (SingleDependentLink parameter : myParameters) {
      params.addAll(DependentLink.Helper.toList(DependentLink.Helper.subst(parameter, subst, LevelSubstitution.EMPTY)));
    }
    return myResultType.subst(subst, LevelSubstitution.EMPTY);
  }

  @Override
  public Expression getDefCall(LevelArguments polyArguments, Expression thisExpr, List<Expression> arguments) {
    return new LetClauseCallExpression(this, arguments);
  }
}
