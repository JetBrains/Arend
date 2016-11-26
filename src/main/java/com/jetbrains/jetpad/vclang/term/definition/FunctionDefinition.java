package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.FunCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelArguments;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;
import com.jetbrains.jetpad.vclang.term.expr.type.TypeMax;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.FunCall;

public class FunctionDefinition extends Definition implements Function {
  private DependentLink myParameters;
  private TypeMax myResultType;
  private ElimTreeNode myElimTree;
  private boolean myTypeHasErrors;
  private TypeCheckingStatus myHasErrors;

  public FunctionDefinition(Abstract.Definition abstractDef) {
    super(abstractDef);
    myTypeHasErrors = true;
    myHasErrors = TypeCheckingStatus.TYPE_CHECKING;
    myParameters = EmptyDependentLink.getInstance();
  }

  public FunctionDefinition(Abstract.Definition abstractDef, DependentLink parameters, Type resultType, ElimTreeNode elimTree) {
    super(abstractDef);
    assert parameters != null;
    myParameters = parameters;
    myResultType = resultType;
    myElimTree = elimTree;
    myTypeHasErrors = resultType == null;
    myHasErrors = myTypeHasErrors ? TypeCheckingStatus.HAS_ERRORS : myElimTree == null ? TypeCheckingStatus.TYPE_CHECKING : TypeCheckingStatus.NO_ERRORS;
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

  public TypeMax getResultType() {
    return myResultType;
  }

  @Override
  public int getNumberOfRequiredArguments() {
    return DependentLink.Helper.size(myParameters);
  }

  public void setResultType(TypeMax resultType) {
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
  public TypeCheckingStatus hasErrors() {
    return myHasErrors;
  }

  @Override
  public void hasErrors(TypeCheckingStatus status) {
    myHasErrors = status;
  }

  @Override
  public TypeMax getTypeWithParams(List<DependentLink> params, LevelArguments polyArguments) {
    if (myTypeHasErrors) {
      return null;
    }
    ExprSubstitution subst = new ExprSubstitution();
    LevelSubstitution polySubst = polyArguments.toLevelSubstitution(this);
    params.addAll(DependentLink.Helper.toList(DependentLink.Helper.subst(myParameters, subst, polySubst)));
    return myResultType.subst(subst, polySubst);
  }

  @Override
  public FunCallExpression getDefCall(LevelArguments polyArguments) {
    return FunCall(this, polyArguments, new ArrayList<Expression>());
  }

  @Override
  public FunCallExpression getDefCall(LevelArguments polyArguments, List<Expression> args) {
    return new FunCallExpression(this, polyArguments, args);
  }
}