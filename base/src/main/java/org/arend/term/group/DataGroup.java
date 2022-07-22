package org.arend.term.group;

import org.arend.naming.reference.LocatedReferable;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class DataGroup extends StaticGroup {
  private final List<? extends InternalReferable> myConstructors;

  public DataGroup(LocatedReferable referable, List<? extends InternalReferable> constructors, List<Statement> statements, ChildGroup parent) {
    super(referable, statements, parent);
    myConstructors = constructors;
  }

  @NotNull
  @Override
  public Collection<? extends InternalReferable> getInternalReferables() {
    return myConstructors;
  }

  @NotNull
  @Override
  public Collection<? extends InternalReferable> getConstructors() {
    return myConstructors;
  }
}
