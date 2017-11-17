package com.jetbrains.jetpad.vclang.frontend.term.group;

import com.jetbrains.jetpad.vclang.module.scopeprovider.ModuleScopeProvider;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.scope.CachingScope;
import com.jetbrains.jetpad.vclang.naming.scope.EmptyScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.naming.scope.ScopeFactory;
import com.jetbrains.jetpad.vclang.term.Group;

import javax.annotation.Nonnull;
import java.util.List;

public class FileGroup extends StaticGroup {
  private Scope myScope = EmptyScope.INSTANCE;

  public FileGroup(GlobalReferable referable, List<Group> staticGroups, List<SimpleNamespaceCommand> namespaceCommands) {
    super(referable, staticGroups, namespaceCommands, null);
  }

  public void setModuleScopeProvider(ModuleScopeProvider moduleScopeProvider) {
    if (myScope != EmptyScope.INSTANCE) {
      throw new IllegalStateException();
    }
    myScope = CachingScope.make(ScopeFactory.forGroup(this, moduleScopeProvider));
  }

  @Nonnull
  @Override
  public Scope getGroupScope() {
    return myScope;
  }
}
