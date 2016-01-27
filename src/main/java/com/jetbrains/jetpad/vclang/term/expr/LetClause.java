package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.context.binding.NamedBinding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Function;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;

public class LetClause extends NamedBinding implements Function {
  private DependentLink myParameters;
  private ElimTreeNode myElimTree;
  private final Expression myResultType;

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

  @Override
  public ClassDefinition getThisClass() {
    return null;
  }

  @Override
  public Expression getType() {
    return Function.Helper.getFunctionType(this);
  }
}
