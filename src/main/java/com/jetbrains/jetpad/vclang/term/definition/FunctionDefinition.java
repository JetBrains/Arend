package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.naming.Namespace;
import com.jetbrains.jetpad.vclang.naming.ResolvedName;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.LevelSubstVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.FunCall;

public class FunctionDefinition extends Definition implements Function {
  private DependentLink myParameters;
  private Expression myResultType;
  private ElimTreeNode myElimTree;
  private boolean myTypeHasErrors;

  public FunctionDefinition(ResolvedName rn, Abstract.Definition.Precedence precedence) {
    super(rn, precedence);
    myTypeHasErrors = true;
    myParameters = EmptyDependentLink.getInstance();
  }

  public FunctionDefinition(ResolvedName rn, Abstract.Definition.Precedence precedence, DependentLink parameters, Expression resultType, ElimTreeNode elimTree) {
    super(rn, precedence);
    assert parameters != null;
    hasErrors(false);
    myParameters = parameters;
    myResultType = resultType;
    myTypeHasErrors = false;
    myElimTree = elimTree;
  }

  public FunctionDefinition(ResolvedName rn, Abstract.Definition.Precedence precedence, DependentLink parameters, Expression resultType, ElimTreeNode elimTree, TypeUniverse universe) {
    super(rn, precedence, universe);
    assert parameters != null;
    hasErrors(false);
    myParameters = parameters;
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
  public int getNumberOfRequiredArguments() {
    return DependentLink.Helper.size(myParameters);
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
  public Expression getType() {
    if (myTypeHasErrors) {
      return null;
    }
    return Function.Helper.getFunctionType(this);
  }

  @Override
  public FunCallExpression getDefCall() {
    return FunCall(this);
  }

  @Override
  public FunctionDefinition substPolyParams(LevelSubstitution substitution) {
    if (!isPolymorphic() || myTypeHasErrors) {
      return this;
    }

    Substitution subst = new Substitution(new ExprSubstitution(), substitution);
    DependentLink newParams = DependentLink.Helper.subst(myParameters, subst);

    return new FunctionDefinition(getResolvedName(), getPrecedence(), newParams,
            myResultType.subst(subst), myElimTree.subst(subst), getUniverse().subst(subst.LevelSubst));
  }
}
