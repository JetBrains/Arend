package org.arend.core.definition;

import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.context.param.TypedDependentLink;
import org.arend.core.context.param.UntypedDependentLink;
import org.arend.core.elimtree.Body;
import org.arend.core.expr.*;
import org.arend.core.expr.type.TypeExpression;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.ext.core.definition.CoreFunctionDefinition;
import org.arend.naming.reference.TCReferable;
import org.arend.prelude.Prelude;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class FunctionDefinition extends Definition implements Function, CoreFunctionDefinition {
  private DependentLink myParameters;
  private Expression myResultType;
  private Expression myResultTypeLevel;
  private Body myBody;
  private List<Integer> myParametersTypecheckingOrder;
  private Kind myKind = Kind.FUNC;
  private boolean myBodyIsHidden = false;
  private List<Boolean> myGoodThisParameters = Collections.emptyList();
  private List<TypeClassParameterKind> myTypeClassParameters = Collections.emptyList();
  private int myVisibleParameter = -1;
  private ParametersLevels<ParametersLevel> myParametersLevels = new ParametersLevels<>();

  public FunctionDefinition(TCReferable referable) {
    super(referable, TypeCheckingStatus.HEADER_NEEDS_TYPE_CHECKING);
    myParameters = EmptyDependentLink.getInstance();
  }

  @Override
  public Body getBody() {
    return myKind != Kind.FUNC || myBodyIsHidden ? null : myBody;
  }

  @Override
  public Body getActualBody() {
    return myBody;
  }

  public boolean isBodyHidden() {
    return myBodyIsHidden;
  }

  public void hideBody() {
    myBodyIsHidden = true;
  }

  public void setBody(Body body) {
    myBody = body;
  }

  public boolean isSFunc() {
    return myKind != Kind.FUNC;
  }

  @NotNull
  @Override
  public Kind getKind() {
    return myKind;
  }

  public void setKind(Kind kind) {
    myKind = kind;
  }

  @NotNull
  @Override
  public DependentLink getParameters() {
    return myParameters;
  }

  public void setParameters(DependentLink parameters) {
    myParameters = parameters;
  }

  @Override
  public Expression getResultType() {
    return myResultType;
  }

  public void setResultType(Expression resultType) {
    myResultType = resultType;
  }

  @Override
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
    return myParametersLevels.getList();
  }

  public void addParametersLevel(ParametersLevel parametersLevel) {
    myParametersLevels.add(parametersLevel);
  }

  @Override
  public Expression getTypeWithParams(List<? super DependentLink> params, Sort sortArgument) {
    if (!status().headerIsOK()) {
      return null;
    }

    ExprSubstitution subst = new ExprSubstitution();
    if (this == Prelude.PATH_INFIX && sortArgument.isProp()) {
      Sort sort = Sort.SetOfLevel(sortArgument.getPLevel());
      TypedDependentLink param = new TypedDependentLink(false, myParameters.getName(), new UniverseExpression(sort), EmptyDependentLink.getInstance());
      TypedDependentLink param3 = new TypedDependentLink(true, myParameters.getNext().getNext().getName(), new TypeExpression(new ReferenceExpression(param), sort), EmptyDependentLink.getInstance());
      UntypedDependentLink param2 = new UntypedDependentLink(myParameters.getNext().getName(), param3);
      param.setNext(param2);

      params.add(param);
      params.add(param2);
      params.add(param3);
      return new UniverseExpression(Sort.PROP);
    }

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
      myResultType = new ErrorExpression();
    }
  }
}