package org.arend.core.definition;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.elimtree.Body;
import org.arend.core.expr.*;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.Levels;
import org.arend.ext.core.definition.CoreFunctionDefinition;
import org.arend.ext.core.level.LevelSubstitution;
import org.arend.naming.reference.TCDefReferable;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class FunctionDefinition extends Definition implements Function, CoreFunctionDefinition {
  private DependentLink myParameters;
  private Expression myResultType;
  private Expression myResultTypeLevel;
  private Body myBody;
  private List<Integer> myParametersTypecheckingOrder;
  private Kind myKind = Kind.FUNC;
  private HiddenStatus myBodyIsHidden = HiddenStatus.NOT_HIDDEN;
  private List<Boolean> myGoodThisParameters = Collections.emptyList();
  private List<TypeClassParameterKind> myTypeClassParameters = Collections.emptyList();
  private int myVisibleParameter = -1;
  private final ParametersLevels<ParametersLevel> myParametersLevels = new ParametersLevels<>();
  private Set<Definition> myRecursiveDefinitions = Collections.emptySet();
  private boolean myHasEnclosingClass;
  private List<Boolean> myStrictParameters = Collections.emptyList();
  private List<Boolean> myOmegaParameters = Collections.emptyList();
  private List<LevelVariable> myLevelParameters;
  private UniverseKind myUniverseKind = UniverseKind.NO_UNIVERSES;

  public enum HiddenStatus { NOT_HIDDEN, HIDDEN, REALLY_HIDDEN }

  public FunctionDefinition(TCDefReferable referable) {
    super(referable, TypeCheckingStatus.NEEDS_TYPE_CHECKING);
    myParameters = EmptyDependentLink.getInstance();
  }

  @Override
  public Body getBody() {
    return isSFunc() || myBodyIsHidden != HiddenStatus.NOT_HIDDEN ? null : myBody;
  }

  @Override
  public Body getActualBody() {
    return myBodyIsHidden == HiddenStatus.REALLY_HIDDEN ? null : myBody;
  }

  public Body getReallyActualBody() {
    return myBody;
  }

  public HiddenStatus getBodyHiddenStatus() {
    return myBodyIsHidden;
  }

  public void hideBody() {
    myBodyIsHidden = HiddenStatus.HIDDEN;
  }

  public void reallyHideBody() {
    myBodyIsHidden = HiddenStatus.REALLY_HIDDEN;
  }

  public void setBody(Body body) {
    myBody = body;
  }

  @Override
  public boolean hasStrictParameters() {
    return !myStrictParameters.isEmpty();
  }

  @Override
  public boolean isStrict(int parameter) {
    return parameter < myStrictParameters.size() && myStrictParameters.get(parameter);
  }

  public List<Boolean> getStrictParameters() {
    return myStrictParameters;
  }

  public void setStrictParameters(List<Boolean> parameters) {
    myStrictParameters = parameters;
  }

  @Override
  public boolean isOmegaParameter(int index) {
    return index < myOmegaParameters.size() && myOmegaParameters.get(index);
  }

  public List<Boolean> getOmegaParameters() {
    return myOmegaParameters;
  }

  @Override
  public void setOmegaParameters(List<Boolean> parameters) {
    myOmegaParameters = parameters;
  }

  @Override
  public @NotNull Set<? extends Definition> getRecursiveDefinitions() {
    return myRecursiveDefinitions;
  }

  @Override
  public List<LevelVariable> getLevelParameters() {
    return myLevelParameters;
  }

  @Override
  public void setLevelParameters(List<LevelVariable> parameters) {
    myLevelParameters = parameters;
  }

  public void setRecursiveDefinitions(Set<Definition> recursiveDefinitions) {
    myRecursiveDefinitions = recursiveDefinitions;
  }

  public boolean isSFunc() {
    return myKind == Kind.SFUNC || myKind == Kind.LEMMA || myKind == Kind.TYPE;
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
  protected boolean hasEnclosingClass() {
    return myHasEnclosingClass;
  }

  public void setHasEnclosingClass(boolean hasEnclosingClass) {
    myHasEnclosingClass = hasEnclosingClass;
  }

  @Override
  public List<TypeClassParameterKind> getTypeClassParameters() {
    return myTypeClassParameters;
  }

  @Override
  public void setTypeClassParameters(List<TypeClassParameterKind> typeClassParameters) {
    myTypeClassParameters = typeClassParameters;
  }

  @Override
  public UniverseKind getUniverseKind() {
    return myUniverseKind;
  }

  @Override
  public void setUniverseKind(UniverseKind kind) {
    myUniverseKind = kind;
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

  @Override
  public <P, R> R accept(DefinitionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitFunction(this, params);
  }

  public void addParametersLevel(ParametersLevel parametersLevel) {
    myParametersLevels.add(parametersLevel);
  }

  @Override
  public Expression getTypeWithParams(List<? super DependentLink> params, Levels levels) {
    if (!status().headerIsOK()) {
      return null;
    }

    ExprSubstitution subst = new ExprSubstitution();
    LevelSubstitution levelSubst = levels.makeSubstitution(this);
    params.addAll(DependentLink.Helper.toList(DependentLink.Helper.subst(myParameters, subst, levelSubst)));
    return myResultType.subst(subst, levelSubst);
  }

  @Override
  public Expression getDefCall(Levels levels, List<Expression> arguments) {
    return FunCallExpression.make(this, levels, arguments);
  }
}