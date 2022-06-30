package org.arend.term.group;

import org.arend.naming.reference.LocatedReferable;
import org.arend.term.NamespaceCommand;
import org.arend.term.abs.Abstract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class StaticGroup implements ChildGroup, Statement {
  private final LocatedReferable myReferable;
  private final List<Statement> myStatements;
  private final ChildGroup myParent;

  public StaticGroup(LocatedReferable referable, List<Statement> statements, ChildGroup parent) {
    myReferable = referable;
    myStatements = statements;
    myParent = parent;
  }

  @NotNull
  @Override
  public LocatedReferable getReferable() {
    return myReferable;
  }

  @Override
  public @NotNull Collection<? extends Statement> getStatements() {
    return myStatements;
  }

  @NotNull
  @Override
  public Collection<? extends InternalReferable> getInternalReferables() {
    return Collections.emptyList();
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
