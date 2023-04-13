package org.arend.core.definition;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.Expression;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.Levels;
import org.arend.ext.core.level.LevelSubstitution;
import org.arend.naming.reference.MetaReferable;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class MetaTopDefinition extends Definition {
  private List<? extends LevelVariable> myLevelParameters;
  private DependentLink myParameters;
  private List<? extends Boolean> myTypedParameters;

  public MetaTopDefinition(MetaReferable referable) {
    super(referable, TypeCheckingStatus.NEEDS_TYPE_CHECKING);
  }

  @Override
  public MetaReferable getReferable() {
    return (MetaReferable) super.getReferable();
  }

  @Override
  public TopLevelDefinition getTopLevelDefinition() {
    return null;
  }

  @Override
  public List<? extends LevelVariable> getLevelParameters() {
    return myLevelParameters;
  }

  public void setLevelParameters(List<? extends LevelVariable> params) {
    myLevelParameters = params;
  }

  @Override
  public @NotNull DependentLink getParameters() {
    return myParameters;
  }

  public List<? extends Boolean> getTypedParameters() {
    return myTypedParameters;
  }

  @Override
  public void setParameters(DependentLink parameters) {
    myParameters = parameters;
  }

  public void setParameters(DependentLink parameters, List<? extends Boolean> typedParameters) {
    myParameters = parameters;
    myTypedParameters = typedParameters;
  }

  @Override
  public Set<? extends FunctionDefinition> getAxioms() {
    return Collections.emptySet();
  }

  @Override
  public Set<? extends Definition> getGoals() {
    return Collections.emptySet();
  }

  @Override
  public Expression getTypeWithParams(List<? super DependentLink> params, Levels levels) {
    if (!status().headerIsOK()) {
      return null;
    }

    ExprSubstitution subst = new ExprSubstitution();
    LevelSubstitution levelSubst = levels.makeSubstitution(this);
    params.addAll(DependentLink.Helper.toList(DependentLink.Helper.subst(myParameters, subst, levelSubst)));
    return null;
  }

  @Override
  public UniverseKind getUniverseKind() {
    return UniverseKind.WITH_UNIVERSES;
  }

  @Override
  public <P, R> R accept(DefinitionVisitor<? super P, ? extends R> visitor, P params) {
    return null;
  }
}
