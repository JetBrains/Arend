package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.binding.NamedBinding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Callable;
import com.jetbrains.jetpad.vclang.core.definition.Function;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;

import java.util.List;

public class LetClause extends NamedBinding implements Function, Callable {
  private List<Level> myPLevels;
  private List<SingleDependentLink> myParameters;
  private ElimTreeNode myElimTree;
  private Type myResultType;

  public LetClause(String name, List<Level> pLevels, List<SingleDependentLink> parameters, Type resultType, ElimTreeNode elimTree) {
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

  public Type getResultType() {
    return myResultType;
  }

  public void setResultType(Type resultType) {
    myResultType = resultType;
  }

  @Override
  public Type getType() {
    Type type = myResultType;
    for (int i = myParameters.size() - 1; i >= 0; i--) {
      type = new PiExpression(myPLevels.get(i), myParameters.get(i), type.getExpr());
    }
    return type;
  }

  @Override
  public Expression getTypeWithParams(List<? super DependentLink> params, Sort sortArgument) {
    ExprSubstitution subst = new ExprSubstitution();
    for (SingleDependentLink parameter : myParameters) {
      params.addAll(DependentLink.Helper.toList(DependentLink.Helper.subst(parameter, subst, LevelSubstitution.EMPTY)));
    }
    return myResultType.getExpr().subst(subst, LevelSubstitution.EMPTY);
  }

  @Override
  public Expression getDefCall(Sort sortArgument, Expression thisExpr, List<Expression> arguments) {
    return new LetClauseCallExpression(this, arguments);
  }
}
