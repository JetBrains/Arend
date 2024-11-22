package org.arend.term.group;

import org.arend.module.scopeprovider.ModuleScopeProvider;
import org.arend.naming.reference.LocatedReferable;
import org.arend.naming.scope.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class FileGroup extends StaticGroup {
  private Scope myScope = EmptyScope.INSTANCE;

  public FileGroup(LocatedReferable referable, List<Statement> statements) {
    super(referable, statements, Collections.emptyList(), null, false);
  }

  public void setModuleScopeProvider(ModuleScopeProvider moduleScopeProvider) {
    if (myScope != EmptyScope.INSTANCE) {
      throw new IllegalStateException();
    }
    myScope = CachingScope.make(ScopeFactory.forGroup(this, moduleScopeProvider));
  }

  @NotNull
  @Override
  public Scope getGroupScope() {
    return myScope;
  }
}
