package com.jetbrains.jetpad.vclang.prelude;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.library.LibraryHeader;
import com.jetbrains.jetpad.vclang.library.PersistableSourceLibrary;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.scopeprovider.ModuleScopeProvider;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.SimpleGlobalReferable;
import com.jetbrains.jetpad.vclang.naming.scope.CachingScope;
import com.jetbrains.jetpad.vclang.naming.scope.LexicalScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Precedence;
import com.jetbrains.jetpad.vclang.term.group.Group;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.util.LongName;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;

/**
 * A base class for fake libraries which are used to load prelude.
 */
public abstract class PreludeFakeLibrary extends PersistableSourceLibrary {
  private Group myGroup;
  private Scope myScope;

  /**
   * Creates a new {@code PreludeFakeLibrary}
   *
   * @param typecheckerState the underling typechecker state of this library.
   */
  protected PreludeFakeLibrary(TypecheckerState typecheckerState) {
    super(typecheckerState);
  }

  @Override
  public void onModuleLoaded(ModulePath modulePath, Group group) {
    if (!modulePath.equals(Prelude.MODULE_PATH)) {
      throw new IllegalStateException();
    }
    myGroup = group;
    myScope = CachingScope.make(LexicalScope.opened(group));
  }

  @Nullable
  @Override
  public ModulePath getDefinitionModule(GlobalReferable referable) {
    return Prelude.MODULE_PATH;
  }

  @Nullable
  @Override
  public LongName getDefinitionFullName(GlobalReferable referable) {
    return new LongName(Collections.singletonList(referable.textRepresentation()));
  }

  @Nonnull
  @Override
  protected GlobalReferable generateReferable(ModulePath modulePath, LongName name, Precedence precedence, GlobalReferable typecheckable) {
    return new SimpleGlobalReferable(precedence, name.getLastName(), typecheckable);
  }

  @Nullable
  @Override
  protected LibraryHeader loadHeader(ErrorReporter errorReporter) {
    return new LibraryHeader(Collections.singletonList(Prelude.MODULE_PATH), Collections.emptyList());
  }

  @Override
  public void unload() {
    super.unload();
    myGroup = null;
    myScope = null;
  }

  @Nonnull
  @Override
  public String getName() {
    return "PreludeFakeLibrary";
  }

  @Nonnull
  @Override
  public ModuleScopeProvider getModuleScopeProvider() {
    return module -> module.equals(Prelude.MODULE_PATH) ? myScope : null;
  }

  @Nonnull
  @Override
  public Collection<? extends ModulePath> getLoadedModules() {
    return myGroup == null ? Collections.emptyList() : Collections.singletonList(Prelude.MODULE_PATH);
  }

  @Nullable
  @Override
  public Group getModuleGroup(ModulePath modulePath) {
    return modulePath.equals(Prelude.MODULE_PATH) ? myGroup : null;
  }

  @Override
  public boolean containsModule(ModulePath modulePath) {
    return modulePath.equals(Prelude.MODULE_PATH);
  }
}
