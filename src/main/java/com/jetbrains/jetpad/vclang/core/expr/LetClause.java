package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.binding.NamedBinding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Function;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.ElimTreeNode;

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
}
