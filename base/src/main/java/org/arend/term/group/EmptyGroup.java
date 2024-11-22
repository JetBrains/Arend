package org.arend.term.group;

import org.arend.naming.reference.LocatedReferable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class EmptyGroup implements ChildGroup, Statement {
  private final LocatedReferable myReferable;
  private final ChildGroup myParent;
  private final boolean myDynamicContext;

  public EmptyGroup(LocatedReferable referable, ChildGroup parent, boolean isDynamicScope) {
    myReferable = referable;
    myParent = parent;
    myDynamicContext = isDynamicScope;
  }

  @Nullable
  @Override
  public ChildGroup getParentGroup() {
    return myParent;
  }

  @Override
  public boolean isDynamicContext() {
    return myDynamicContext;
  }

  @NotNull
  @Override
  public LocatedReferable getReferable() {
    return myReferable;
  }

  @Override
  public @NotNull List<? extends Statement> getStatements() {
    return Collections.emptyList();
  }

  @NotNull
  @Override
  public List<? extends InternalReferable> getInternalReferables() {
    return Collections.emptyList();
  }

  @Override
  public Group getGroup() {
    return this;
  }
}
