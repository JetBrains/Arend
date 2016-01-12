package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.FunCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.param.Argument;
import com.jetbrains.jetpad.vclang.term.expr.param.Utils;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;

import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.FunCall;

public class FunctionDefinition extends Definition implements Function {
  // TODO: myArguments should have type List<TypeArguments>
  private List<Argument> myArguments;
  private Expression myResultType;
  private ElimTreeNode myElimTree;
  private boolean myTypeHasErrors;

  public FunctionDefinition(Namespace parentNamespace, Name name, Abstract.Definition.Precedence precedence) {
    super(parentNamespace, name, precedence);
    myTypeHasErrors = true;
  }

  public FunctionDefinition(Namespace parentNamespace, Name name, Abstract.Definition.Precedence precedence, List<Argument> arguments, Expression resultType, ElimTreeNode elimTree) {
    super(parentNamespace, name, precedence);
    setUniverse(new Universe.Type(0, Universe.Type.PROP));
    hasErrors(false);
    myArguments = arguments;
    myResultType = resultType;
    myTypeHasErrors = false;
    myElimTree = elimTree;
  }

  public Namespace getStaticNamespace() {
    return getParentNamespace().getChild(getName());
  }

  @Override
  public ElimTreeNode getElimTree() {
    return myElimTree;
  }

  @Override
  public boolean isAbstract() {
    return myElimTree == null;
  }

  public void setElimTree(ElimTreeNode elimTree) {
    myElimTree = elimTree;
  }

  @Override
  public List<Argument> getArguments() {
    return myArguments;
  }

  public void setArguments(List<Argument> arguments) {
    myArguments = arguments;
  }

  @Override
  public Expression getResultType() {
    return myResultType;
  }

  public void setResultType(Expression resultType) {
    myResultType = resultType;
  }

  public boolean typeHasErrors() {
    return myTypeHasErrors;
  }

  public void typeHasErrors(boolean has) {
    myTypeHasErrors = has;
  }

  @Override
  public Expression getBaseType() {
    if (myTypeHasErrors) {
      return null;
    }
    return Utils.getFunctionType(this);
  }

  @Override
  public FunCallExpression getDefCall() {
    return FunCall(this);
  }
}
