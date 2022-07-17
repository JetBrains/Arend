package org.arend.term.group;

import org.arend.module.scopeprovider.ModuleScopeProvider;
import org.arend.naming.reference.LocatedReferable;
import org.arend.naming.scope.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FileGroup extends StaticGroup {
  private Scopes myScopes = Scopes.EMPTY;

  public FileGroup(LocatedReferable referable, List<Statement> statements) {
    super(referable, statements, null);
  }

  public void setModuleScopeProvider(ModuleScopeProvider moduleScopeProvider) {
    if (myScopes.getExpressionScope() != EmptyScope.INSTANCE) {
      throw new IllegalStateException();
    }
    myScopes = new Scopes(ScopeFactory.forGroup(this, moduleScopeProvider), LevelLexicalScope.insideOf(this, EmptyScope.INSTANCE, true), LevelLexicalScope.insideOf(this, EmptyScope.INSTANCE, false)).caching();
  }

  @NotNull
  @Override
  public Scope getGroupScope(LexicalScope.Extent extent) {
    return myScopes.getExpressionScope();
  }

  @Override
  public Scope getGroupPLevelScope() {
    return myScopes.getPLevelScope();
  }

  @Override
  public Scope getGroupHLevelScope() {
    return myScopes.getHLevelScope();
  }

  @Override
  public Scopes getGroupScopes() {
    return myScopes;
  }
}
