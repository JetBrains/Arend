package org.arend.term.group;

import org.arend.module.scopeprovider.EmptyModuleScopeProvider;
import org.arend.naming.scope.LexicalScope;
import org.arend.naming.scope.Scope;
import org.arend.naming.scope.ScopeFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ChildGroup extends Group {
  @Nullable ChildGroup getParentGroup();

  @NotNull
  default Scope getGroupScope(LexicalScope.Extent extent) {
    ChildGroup parent = getParentGroup();
    return parent == null ? ScopeFactory.forGroup(this, EmptyModuleScopeProvider.INSTANCE) : LexicalScope.insideOf(this, parent.getGroupScope(LexicalScope.Extent.EVERYTHING), extent);
  }

  default Scope getGroupScope() {
    return getGroupScope(LexicalScope.Extent.EXTERNAL_AND_FIELDS);
  }
}
