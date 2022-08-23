package org.arend.term.group;

import org.arend.naming.reference.LocatedReferable;
import org.arend.naming.reference.ParameterReferable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DataGroup extends StaticGroup {
  private final List<? extends InternalReferable> myConstructors;

  public DataGroup(LocatedReferable referable, List<? extends InternalReferable> constructors, List<Statement> statements, List<ParameterReferable> parameterReferables, ChildGroup parent) {
    super(referable, statements, parameterReferables, parent);
    myConstructors = constructors;
  }

  @NotNull
  @Override
  public List<? extends InternalReferable> getInternalReferables() {
    return myConstructors;
  }

  @NotNull
  @Override
  public List<? extends InternalReferable> getConstructors() {
    return myConstructors;
  }
}
