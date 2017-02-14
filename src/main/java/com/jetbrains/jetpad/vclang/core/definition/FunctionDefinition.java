package com.jetbrains.jetpad.vclang.core.definition;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.FunCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeMax;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.core.sort.LevelArguments;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.FunCall;

public class FunctionDefinition extends Definition implements Function {
  private DependentLink myParameters;
  private TypeMax myResultType;
  private ElimTreeNode myElimTree;

  public FunctionDefinition(Abstract.Definition abstractDef) {
    super(abstractDef, TypeCheckingStatus.HEADER_HAS_ERRORS);
    myParameters = EmptyDependentLink.getInstance();
  }

  public FunctionDefinition(Abstract.Definition abstractDef, DependentLink parameters, TypeMax resultType, ElimTreeNode elimTree) {
    super(abstractDef, resultType == null ? TypeCheckingStatus.HEADER_HAS_ERRORS : elimTree == null ? TypeCheckingStatus.BODY_NEEDS_TYPE_CHECKING : TypeCheckingStatus.NO_ERRORS);
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
  public TypeMax getTypeWithParams(List<DependentLink> params, LevelArguments polyArguments) {
    if (!status().headerIsOK()) {
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