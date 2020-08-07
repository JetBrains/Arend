package org.arend.term.concrete;

import org.arend.ext.concrete.expr.ConcreteArgument;
import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.typechecking.ContextData;
import org.arend.ext.typechecking.ExpressionTypechecker;
import org.arend.ext.typechecking.MetaDefinition;
import org.arend.ext.typechecking.TypedExpression;
import org.arend.naming.reference.MetaReferable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * User-defined meta in Arend, not Java extension meta.
 */
public class DefinableMetaDefinition extends Concrete.ResolvableDefinition implements MetaDefinition {
  public final List<Concrete.NameParameter> myParameters;
  private MetaReferable myReferable;
  public Concrete.Expression body;

  public DefinableMetaDefinition(List<Concrete.NameParameter> parameters, Concrete.Expression body) {
    myParameters = parameters;
    this.body = body;
  }

  public void setReferable(@NotNull MetaReferable referable) {
    myReferable = referable;
  }

  public List<? extends Concrete.NameParameter> getParameters() {
    return myParameters;
  }

  @Override
  public void prettyPrint(StringBuilder builder, PrettyPrinterConfig ppConfig) {

  }

  @Override
  public boolean checkArguments(@NotNull List<? extends ConcreteArgument> arguments) {
    // TODO[meta]: implement this
    return false;
  }

  @Override
  public @Nullable ConcreteExpression getConcreteRepresentation(@NotNull List<? extends ConcreteArgument> arguments) {
    // TODO[meta]: implement this
    return null;
  }

  @Override
  public @Nullable TypedExpression invokeMeta(@NotNull ExpressionTypechecker typechecker, @NotNull ContextData contextData) {
    // TODO[meta]: implement this
    return null;
  }

  @Override
  public @NotNull MetaReferable getData() {
    return myReferable;
  }
}
