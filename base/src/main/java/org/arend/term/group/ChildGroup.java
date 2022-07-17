package org.arend.term.group;

import org.arend.module.scopeprovider.EmptyModuleScopeProvider;
import org.arend.naming.scope.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ChildGroup extends Group {
  @Nullable ChildGroup getParentGroup();

  @NotNull
  default Scope getGroupScope(LexicalScope.Extent extent, Scope.Kind kind) {
    ChildGroup parent = getParentGroup();
    return parent == null ? ScopeFactory.forGroup(this, EmptyModuleScopeProvider.INSTANCE, kind) : LexicalScope.insideOf(this, parent.getGroupScope(LexicalScope.Extent.EVERYTHING, kind), extent, kind);
  }

  default Scope getGroupScope(Scope.Kind kind) {
    return getGroupScope(LexicalScope.Extent.EXTERNAL_AND_FIELDS, kind);
  }

  default Scope getGroupScope() {
    return getGroupScope(Scope.Kind.EXPR);
  }

  default Scopes getGroupScopes() {
    return new Scopes(getGroupScope(Scope.Kind.EXPR), getGroupScope(Scope.Kind.PLEVEL), getGroupScope(Scope.Kind.HLEVEL));
  }
}
