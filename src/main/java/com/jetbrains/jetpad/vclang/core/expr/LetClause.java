package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.binding.NamedBinding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Callable;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;

import java.util.List;

public class LetClause extends NamedBinding implements Callable {
  private List<Sort> mySorts;
  private List<SingleDependentLink> myParameters;
  private ElimTreeNode myElimTree;
  private Type myResultType;

  public LetClause(String name, List<Sort> sorts, List<SingleDependentLink> parameters, Type resultType, ElimTreeNode elimTree) {
    super(name);
    assert sorts.size() == parameters.size();
    mySorts = sorts;
    myParameters = parameters;
    myResultType = resultType;
    myElimTree = elimTree;
  }

  public ElimTreeNode getElimTree() {
    return myElimTree;
  }

  public void setElimTree(ElimTreeNode elimTree) {
    myElimTree = elimTree;
  }

  public List<Sort> getSortList() {
    return mySorts;
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
      type = new PiExpression(mySorts.get(i), myParameters.get(i), type.getExpr());
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
