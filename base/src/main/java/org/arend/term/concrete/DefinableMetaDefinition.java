package org.arend.term.concrete;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.concrete.expr.ConcreteArgument;
import org.arend.ext.concrete.expr.ConcreteCoclauses;
import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.concrete.expr.ConcreteReferenceExpression;
import org.arend.ext.error.ArgumentExplicitnessError;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.reference.Precedence;
import org.arend.ext.typechecking.ContextData;
import org.arend.ext.typechecking.ExpressionTypechecker;
import org.arend.ext.typechecking.MetaDefinition;
import org.arend.ext.typechecking.TypedExpression;
import org.arend.ext.util.Pair;
import org.arend.extImpl.ConcreteFactoryImpl;
import org.arend.naming.reference.LevelReferable;
import org.arend.naming.reference.MetaReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCDefReferable;
import org.arend.term.prettyprint.PrettyPrintVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * User-defined meta in Arend, not Java extension meta.
 */
public class DefinableMetaDefinition extends Concrete.ResolvableDefinition implements MetaDefinition {
  private final List<Concrete.Parameter> myParameters;
  private final MetaReferable myReferable;
  public Concrete.@Nullable Expression body;

  public DefinableMetaDefinition(MetaReferable referable, Concrete.LevelParameters pLevelParameters, Concrete.LevelParameters hLevelParameters, List<Concrete.Parameter> parameters, Concrete.@Nullable Expression body) {
    myReferable = referable;
    this.pLevelParameters = pLevelParameters;
    this.hLevelParameters = hLevelParameters;
    myParameters = parameters;
    this.body = body;
    stage = Concrete.Stage.NOT_RESOLVED;
  }

  public Concrete.LevelParameters getPLevelParameters() {
    return pLevelParameters;
  }

  public List<? extends LevelReferable> getPLevelParametersList() {
    return pLevelParameters == null ? null : pLevelParameters.getReferables();
  }

  public Concrete.LevelParameters getHLevelParameters() {
    return hLevelParameters;
  }

  public List<? extends LevelReferable> getHLevelParametersList() {
    return hLevelParameters == null ? null : hLevelParameters.getReferables();
  }

  @Override
  public List<? extends Concrete.Parameter> getParameters() {
    return myParameters;
  }

  @Override
  public void addParameters(List<? extends Concrete.Parameter> parameters, List<Pair<TCDefReferable,Integer>> parametersOriginalDefinitions) {
    throw new IllegalStateException();
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

    int params = 0;
    for (Concrete.Parameter parameter : myParameters) {
      params += parameter.getRefList().size();
    }
    boolean ok = arguments.size() >= params;
    if (!ok && errorReporter != null) {
      errorReporter.report(new TypecheckingError("Expected " + params + " arguments, found " + arguments.size(), marker));
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
    List<Referable> refs = new ArrayList<>();
    for (Concrete.Parameter parameter : myParameters) {
      refs.addAll(parameter.getRefList());
    }
    assert refs.size() <= arguments.size();

    var subst = new SubstConcreteVisitor(data);
    for (int i = 0; i < refs.size(); i++) {
      Referable ref = refs.get(i);
      if (ref != null) {
        subst.bind(ref, (Concrete.Expression) arguments.get(i).getExpression());
      }
    }

    binLevelParameters(subst, pLevels, getPLevelParametersList());
    binLevelParameters(subst, hLevels, getHLevelParametersList());
    Concrete.Expression result = body.accept(subst, null);
    if (result == null) return null;
    if (arguments.size() > refs.size()) {
      result = new ConcreteFactoryImpl(result.getData()).app(result, arguments.subList(refs.size(), arguments.size()));
    }
    if (coclauses != null) {
      if (!(coclauses instanceof Concrete.Coclauses)) {
        throw new IllegalArgumentException();
      }
      result = Concrete.ClassExtExpression.make(result.getData(), result, (Concrete.Coclauses) coclauses);
    }
    return result;
  }

  private void binLevelParameters(SubstConcreteVisitor subst, @Nullable List<Concrete.LevelExpression> levels, List<? extends LevelReferable> levelParameters) {
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

  @Override
  public void prettyPrint(PrettyPrintVisitor visitor, Precedence prec) {
    visitor.visitMeta(this, null);
  }
}
