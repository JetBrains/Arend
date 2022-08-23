package org.arend.term.group;

import org.arend.naming.reference.LocatedReferable;
import org.arend.naming.reference.ParameterReferable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class StaticGroup implements ChildGroup, Statement {
  private final LocatedReferable myReferable;
  private final List<Statement> myStatements;
  private final List<ParameterReferable> myExternalParameters;
  private final ChildGroup myParent;

  public StaticGroup(LocatedReferable referable, List<Statement> statements, List<ParameterReferable> externalParameters, ChildGroup parent) {
    myReferable = referable;
    myStatements = statements;
    myExternalParameters = externalParameters;
    myParent = parent;
  }

  @NotNull
  @Override
  public LocatedReferable getReferable() {
    return myReferable;
  }

  @Override
  public @NotNull List<? extends Statement> getStatements() {
    return myStatements;
  }

  @NotNull
  @Override
  public List<? extends InternalReferable> getInternalReferables() {
    return Collections.emptyList();
  }

  @Override
  public @NotNull List<? extends ParameterReferable> getExternalParameters() {
    return myExternalParameters;
  }

  @Nullable
  @Override
  public ChildGroup getParentGroup() {
    return myParent;
  }

  @Override
  public Group getGroup() {
    return this;
  }
}
