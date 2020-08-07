package org.arend.term.concrete;

import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.naming.reference.MetaReferable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * User-defined meta in Arend, not Java extension meta.
 */
public class DefinableMetaDefinition extends Concrete.ResolvableDefinition {
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
  public @NotNull MetaReferable getData() {
    return myReferable;
  }
}
