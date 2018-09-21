package org.arend.term.group;

import org.arend.module.scopeprovider.EmptyModuleScopeProvider;
import org.arend.naming.scope.LexicalScope;
import org.arend.naming.scope.Scope;
import org.arend.naming.scope.ScopeFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ChildGroup extends Group {
  @Nullable ChildGroup getParentGroup();

  @Nonnull
  default Scope getGroupScope() {
    ChildGroup parent = getParentGroup();
    return parent == null ? ScopeFactory.forGroup(this, EmptyModuleScopeProvider.INSTANCE) : LexicalScope.insideOf(this, parent.getGroupScope());
  }
}
