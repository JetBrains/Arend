package org.arend.term.concrete;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.concrete.expr.ConcreteArgument;
import org.arend.ext.concrete.expr.ConcreteCoclauses;
import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.concrete.expr.ConcreteReferenceExpression;
import org.arend.ext.error.ArgumentExplicitnessError;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.typechecking.ContextData;
import org.arend.ext.typechecking.ExpressionTypechecker;
import org.arend.ext.typechecking.MetaDefinition;
import org.arend.ext.typechecking.TypedExpression;
import org.arend.naming.reference.LevelReferable;
import org.arend.naming.reference.MetaReferable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * User-defined meta in Arend, not Java extension meta.
 */
public class DefinableMetaDefinition extends Concrete.ResolvableDefinition implements MetaDefinition {
  private final List<Concrete.NameParameter> myParameters;
  private final MetaReferable myReferable;
  public Concrete.@Nullable Expression body;
  private final List<LevelReferable> myPLevelParameters;
  private final List<LevelReferable> myHLevelParameters;

  public DefinableMetaDefinition(MetaReferable referable, List<LevelReferable> pLevelParameters, List<LevelReferable> hLevelParameters, List<Concrete.NameParameter> parameters, Concrete.@Nullable Expression body) {
    myReferable = referable;
    myPLevelParameters = pLevelParameters;
    myHLevelParameters = hLevelParameters;
    myParameters = parameters;
    this.body = body;
  }

  public List<LevelReferable> getPLevelParameters() {
    return myPLevelParameters;
  }

  public List<LevelReferable> getHLevelParameters() {
    return myHLevelParameters;
  }

  public List<? extends Concrete.NameParameter> getParameters() {
    return myParameters;
  }

  @Override
  public boolean checkArguments(@NotNull List<? extends ConcreteArgument> arguments) {
    return checkArguments(arguments, null, null, null);
  }

  protected boolean checkArguments(@NotNull List<? extends ConcreteArgument> arguments, @Nullable ConcreteCoclauses coclauses, @Nullable ErrorReporter errorReporter, @Nullable ConcreteSourceNode marker) {
    for (var argument : arguments) {
      if (!argument.isExplicit()) {
        if (errorReporter != null) {
          errorReporter.report(new ArgumentExplicitnessError(false, argument.getExpression()));
        }
        return false;
      }
    }
    boolean ok = arguments.size() == myParameters.size();
    if (!ok && errorReporter != null) {
      errorReporter.report(new TypecheckingError("Expected " + myParameters.size() + " arguments, found " + arguments.size(), marker));
    }
    return ok;
  }

  @Override
  public boolean checkContextData(@NotNull ContextData contextData, @NotNull ErrorReporter errorReporter) {
    return checkArguments(contextData.getArguments(), contextData.getCoclauses(), errorReporter, contextData.getMarker());
  }

  @Override
  public @Nullable ConcreteExpression getConcreteRepresentation(@NotNull List<? extends ConcreteArgument> arguments) {
    return getConcreteRepresentation(null, null, null, arguments, null);
  }

  protected @Nullable ConcreteExpression getConcreteRepresentation(@Nullable Object data, @Nullable List<Concrete.LevelExpression> pLevels, @Nullable List<Concrete.LevelExpression> hLevels, @NotNull List<? extends ConcreteArgument> arguments, @Nullable ConcreteCoclauses coclauses) {
    if (body == null) return null;
    assert myParameters.size() == arguments.size();
    var subst = new SubstConcreteExpressionVisitor(data);
    for (int i = 0; i < myParameters.size(); i++) {
      subst.bind(Objects.requireNonNull(myParameters.get(i).getReferable()),
        (Concrete.Expression) arguments.get(i).getExpression());
    }
    binLevelParameters(subst, pLevels, myPLevelParameters);
    binLevelParameters(subst, hLevels, myHLevelParameters);
    return body.accept(subst, null);
  }

  private void binLevelParameters(SubstConcreteExpressionVisitor subst, @Nullable List<Concrete.LevelExpression> levels, List<LevelReferable> levelParameters) {
    if (levelParameters != null && levels != null) {
      for (int i = 0; i < levelParameters.size() && i < levels.size(); i++) {
        subst.bind(levelParameters.get(i), levels.get(i));
      }
    }
  }

  @Override
  public @Nullable TypedExpression invokeMeta(@NotNull ExpressionTypechecker typechecker, @NotNull ContextData contextData) {
    ConcreteReferenceExpression refExpr = contextData.getReferenceExpression();
    if (!(refExpr instanceof Concrete.ReferenceExpression)) {
      throw new IllegalStateException();
    }
    ConcreteExpression result = getConcreteRepresentation(refExpr.getData(), ((Concrete.ReferenceExpression) refExpr).getPLevels(), ((Concrete.ReferenceExpression) refExpr).getHLevels(), contextData.getArguments(), contextData.getCoclauses());
    if (result == null) {
      typechecker.getErrorReporter().report(new TypecheckingError("Meta '" + myReferable.getRefName() + "' is not defined", contextData.getMarker()));
      return null;
    }
    return typechecker.typecheck(result, contextData.getExpectedType());
  }

  @Override
  public @NotNull MetaReferable getData() {
    return myReferable;
  }

  @Override
  public @NotNull Concrete.ResolvableDefinition getRelatedDefinition() {
    return this;
  }

  @Override
  public <P, R> R accept(ConcreteResolvableDefinitionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitMeta(this, params);
  }
}
