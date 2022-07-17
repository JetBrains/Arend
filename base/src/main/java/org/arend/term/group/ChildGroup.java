package org.arend.term.group;

import org.arend.module.scopeprovider.EmptyModuleScopeProvider;
import org.arend.naming.scope.*;
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

  default Scope getGroupPLevelScope() {
    ChildGroup parent = getParentGroup();
    Scope parentScope = parent == null ? EmptyScope.INSTANCE : parent.getGroupPLevelScope();
    return LevelLexicalScope.insideOf(this, parentScope, true);
  }

  default Scope getGroupHLevelScope() {
    ChildGroup parent = getParentGroup();
    Scope parentScope = parent == null ? EmptyScope.INSTANCE : parent.getGroupHLevelScope();
    return LevelLexicalScope.insideOf(this, parentScope, false);
  }

  default Scopes getGroupScopes() {
    return new Scopes(getGroupScope(), getGroupPLevelScope(), getGroupHLevelScope());
  }
}
