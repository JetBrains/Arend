package org.arend.term.group;

import org.arend.module.scopeprovider.ModuleScopeProvider;
import org.arend.naming.reference.LocatedReferable;
import org.arend.naming.scope.*;

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
    myScopes = new Scopes(ScopeFactory.forGroup(this, moduleScopeProvider, Scope.Kind.EXPR), ScopeFactory.forGroup(this, moduleScopeProvider, Scope.Kind.PLEVEL), ScopeFactory.forGroup(this, moduleScopeProvider, Scope.Kind.HLEVEL)).caching();
  }

  @Override
  public Scope getGroupScope(Scope.Kind kind) {
    return myScopes.getScope(kind);
  }

  @Override
  public Scopes getGroupScopes() {
    return myScopes;
  }
}
