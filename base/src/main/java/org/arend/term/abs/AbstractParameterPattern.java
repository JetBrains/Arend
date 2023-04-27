package org.arend.term.abs;

import org.arend.naming.reference.Referable;
import org.arend.term.Fixity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class AbstractParameterPattern implements Abstract.Pattern {
  private final Abstract.Parameter myParameter;
  private final Referable myReferable;

  public AbstractParameterPattern(Abstract.Parameter parameter, Referable referable) {
    myParameter = parameter;
    myReferable = referable;
  }

  @Override
  public @NotNull Abstract.SourceNode getTopmostEquivalentSourceNode() {
    return myParameter.getTopmostEquivalentSourceNode();
  }

  @Override
  public @Nullable Abstract.SourceNode getParentSourceNode() {
    return myParameter.getParentSourceNode();
  }

  @Override
  public @Nullable Object getData() {
    return myParameter.getData();
  }

  @Override
  public boolean isUnnamed() {
    return myReferable == null;
  }

  @Override
  public boolean isExplicit() {
    return myParameter.isExplicit();
  }

  @Override
  public boolean isTuplePattern() {
    return false;
  }

  @Override
  public @Nullable Integer getInteger() {
    return null;
  }

  public @Nullable Referable getSingleReferable() {
    return myReferable;
  }

  @Override
  public @Nullable Referable getConstructorReference() {
    return myReferable;
  }

  @Override
  public @Nullable Fixity getFixity() {
    return null;
  }

  @Override
  public @NotNull List<? extends Abstract.Pattern> getSequence() {
    return Collections.emptyList();
  }

  @Override
  public @Nullable Abstract.Expression getType() {
    return myParameter.getType();
  }

  @Override
  public @Nullable Abstract.TypedReferable getAsPattern() {
    return null;
  }
}
