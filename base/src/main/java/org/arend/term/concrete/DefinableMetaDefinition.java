package org.arend.term.concrete;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.concrete.expr.ConcreteArgument;
import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.error.ArgumentExplicitnessError;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.typechecking.ContextData;
import org.arend.ext.typechecking.ExpressionTypechecker;
import org.arend.ext.typechecking.MetaDefinition;
import org.arend.ext.typechecking.TypedExpression;
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
  public Concrete.Expression body;

  public DefinableMetaDefinition(MetaReferable referable, List<Concrete.NameParameter> parameters, Concrete.Expression body) {
    myReferable = referable;
    myParameters = parameters;
    this.body = body;
  }

  public List<? extends Concrete.NameParameter> getParameters() {
    return myParameters;
  }

  @Override
  public boolean checkArguments(@NotNull List<? extends ConcreteArgument> arguments) {
    return checkArguments(arguments, null, null);
  }

  private boolean checkArguments(@NotNull List<? extends ConcreteArgument> arguments, @Nullable ErrorReporter errorReporter, @Nullable ConcreteSourceNode marker) {
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
    return checkArguments(contextData.getArguments(), errorReporter, contextData.getReferenceExpression());
  }

  @SuppressWarnings("unchecked")
  @Override
  public @Nullable ConcreteExpression getConcreteRepresentation(@NotNull List<? extends ConcreteArgument> arguments) {
    return Concrete.AppExpression.make(null, new Concrete.ReferenceExpression(null, myReferable), (List<Concrete.Argument>) arguments);
  }

  @Override
  public @Nullable TypedExpression invokeMeta(@NotNull ExpressionTypechecker typechecker, @NotNull ContextData contextData) {
    if (myParameters.isEmpty()) return typechecker.typecheck(body, contextData.getExpectedType());
    var arguments = contextData.getArguments();
    assert myParameters.size() == arguments.size();
    var subst = new SubstConcreteExpressionVisitor<Void>();
    for (int i = 0; i < myParameters.size(); i++) {
      subst.bind(Objects.requireNonNull(myParameters.get(i).getReferable()),
        (Concrete.Expression) arguments.get(i).getExpression());
    }
    var substExpr = body.accept(subst, null);
    return typechecker.typecheck(substExpr, contextData.getExpectedType());
  }

  @Override
  public @NotNull MetaReferable getData() {
    return myReferable;
  }

  @Override
  public <P, R> R accept(ConcreteResolvableDefinitionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitMeta(this, params);
  }
}
