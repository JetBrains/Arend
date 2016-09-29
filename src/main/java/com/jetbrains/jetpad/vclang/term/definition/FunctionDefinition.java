package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.expr.FunCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.FunCall;

public class FunctionDefinition extends Definition implements Function {
  private DependentLink myParameters;
  private Type myResultType;
  private ElimTreeNode myElimTree;
  private boolean myTypeHasErrors;
  private final Namespace myOwnNamespace;

  public FunctionDefinition(Abstract.Definition abstractDef, Namespace ownNamespace) {
    super(abstractDef);
    myOwnNamespace = ownNamespace;
    myTypeHasErrors = true;
    myParameters = EmptyDependentLink.getInstance();
  }

  public FunctionDefinition(Abstract.Definition abstractDef, Namespace ownNamespace, DependentLink parameters, Type resultType, ElimTreeNode elimTree) {
    super(abstractDef);
    assert parameters != null;
    myOwnNamespace = ownNamespace;
    hasErrors(false);
    myParameters = parameters;
    myResultType = resultType;
    myTypeHasErrors = false;
    myElimTree = elimTree;
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
  public DependentLink getParameters() {
    return myParameters;
  }

  public void setParameters(DependentLink parameters) {
    assert parameters != null;
    myParameters = parameters;
  }

  public Type getResultType() {
    return myResultType;
  }

  @Override
  public int getNumberOfRequiredArguments() {
    return DependentLink.Helper.size(myParameters);
  }

  public void setResultType(Type resultType) {
    myResultType = resultType;
  }

  @Override
  public boolean typeHasErrors() {
    return myTypeHasErrors;
  }

  public void typeHasErrors(boolean has) {
    myTypeHasErrors = has;
  }

  @Override
  public Type getType(LevelSubstitution polyParams) {
    if (myTypeHasErrors) {
      return null;
    }
    return myResultType.addParameters(myParameters, false).subst(new ExprSubstitution(), polyParams);
  }

  @Override
  public FunCallExpression getDefCall() {
    return FunCall(this);
  }

  @Override
  public int getNumberOfParameters() {
    return DependentLink.Helper.size(myParameters);
  }

  @Override
  public Namespace getOwnNamespace() {
    return myOwnNamespace;
  }
}