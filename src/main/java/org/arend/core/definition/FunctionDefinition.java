package org.arend.core.definition;

import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.elimtree.Body;
import org.arend.core.expr.ErrorExpression;
import org.arend.core.expr.Expression;
import org.arend.core.expr.FunCallExpression;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.naming.reference.TCReferable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FunctionDefinition extends Definition implements Function {
  private DependentLink myParameters;
  private Expression myResultType;
  private Expression myResultTypeLevel;
  private Body myBody;
  private List<Integer> myParametersTypecheckingOrder;
  private boolean myLemma;
  private List<Boolean> myGoodThisParameters = Collections.emptyList();
  private List<TypeClassParameterKind> myTypeClassParameters = Collections.emptyList();
  private int myVisibleParameter = -1;
  private List<ParametersLevel> myParametersLevels = Collections.emptyList();

  public FunctionDefinition(TCReferable referable) {
    super(referable, TypeCheckingStatus.HEADER_HAS_ERRORS);
    myParameters = EmptyDependentLink.getInstance();
  }

  @Override
  public Body getBody() {
    return myLemma ? null : myBody;
  }

  public Body getActualBody() {
    return myBody;
  }

  public void setBody(Body body) {
    myBody = body;
  }

  public boolean isLemma() {
    return myLemma;
  }

  public void setIsLemma(boolean isLemma) {
    myLemma = isLemma;
  }

  @Override
  public DependentLink getParameters() {
    return myParameters;
  }

  public void setParameters(DependentLink parameters) {
    myParameters = parameters;
  }

  public Expression getResultType() {
    return myResultType;
  }

  public void setResultType(Expression resultType) {
    myResultType = resultType;
  }

  public Expression getResultTypeLevel() {
    return myResultTypeLevel;
  }

  public void setResultTypeLevel(Expression resultTypeLevel) {
    myResultTypeLevel = resultTypeLevel;
  }

  @Override
  public int getVisibleParameter() {
    return myVisibleParameter;
  }

  public void setVisibleParameter(int index) {
    myVisibleParameter = index;
  }

  @Override
  public List<Integer> getParametersTypecheckingOrder() {
    return myParametersTypecheckingOrder;
  }

  @Override
  public void setParametersTypecheckingOrder(List<Integer> order) {
    myParametersTypecheckingOrder = order;
  }

  @Override
  public List<Boolean> getGoodThisParameters() {
    return myGoodThisParameters;
  }

  @Override
  public void setGoodThisParameters(List<Boolean> goodThisParameters) {
    myGoodThisParameters = goodThisParameters;
  }

  @Override
  public List<TypeClassParameterKind> getTypeClassParameters() {
    return myTypeClassParameters;
  }

  @Override
  public void setTypeClassParameters(List<TypeClassParameterKind> typeClassParameters) {
    myTypeClassParameters = typeClassParameters;
  }

  public void setTypeClassParameter(int index, TypeClassParameterKind kind) {
    if (index < myTypeClassParameters.size()) {
      myTypeClassParameters.set(index, kind);
    }
  }

  @Override
  public List<? extends ParametersLevel> getParametersLevels() {
    return myParametersLevels;
  }

  public void addLevelParameters(ParametersLevel parametersLevel) {
    if (myParametersLevels.isEmpty()) {
      myParametersLevels = new ArrayList<>();
    }
    myParametersLevels.add(parametersLevel);
  }

  @Override
  public Expression getTypeWithParams(List<? super DependentLink> params, Sort sortArgument) {
    if (!status().headerIsOK()) {
      return null;
    }
    ExprSubstitution subst = new ExprSubstitution();
    LevelSubstitution polySubst = sortArgument.toLevelSubstitution();
    params.addAll(DependentLink.Helper.toList(DependentLink.Helper.subst(myParameters, subst, polySubst)));
    return myResultType.subst(subst, polySubst);
  }

  @Override
  public FunCallExpression getDefCall(Sort sortArgument, List<Expression> arguments) {
    return new FunCallExpression(this, sortArgument, arguments);
  }

  @Override
  public void fill() {
    if (myParameters == null) {
      myParameters = EmptyDependentLink.getInstance();
    }
    if (myResultType == null) {
      myResultType = new ErrorExpression(null, null);
    }
  }
}