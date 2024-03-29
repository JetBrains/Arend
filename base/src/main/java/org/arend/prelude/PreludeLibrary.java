package org.arend.prelude;

import org.arend.ext.error.ErrorReporter;
import org.arend.ext.module.ModulePath;
import org.arend.library.LibraryDependency;
import org.arend.library.LibraryHeader;
import org.arend.library.SourceLibrary;
import org.arend.module.scopeprovider.ModuleScopeProvider;
import org.arend.naming.reference.LocatedReferable;
import org.arend.naming.scope.CachingScope;
import org.arend.naming.scope.LexicalScope;
import org.arend.naming.scope.Scope;
import org.arend.term.group.ChildGroup;
import org.arend.term.group.Group;
import org.arend.util.Range;
import org.arend.util.Version;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * A base class for prelude libraries.
 * This class guarantees that prelude is loaded exactly once,
 * so all instances of this class use the same definitions, which are stored in {@link Prelude}.
 */
public abstract class PreludeLibrary extends SourceLibrary {
  private static ChildGroup myGroup;
  private static Scope myScope;

  public static ChildGroup getPreludeGroup() {
    return myGroup;
  }

  public static Scope getPreludeScope() {
    return myScope;
  }

  @Override
  public @NotNull Version getVersion() {
    return Prelude.VERSION;
  }

  @Override
  public void groupLoaded(ModulePath modulePath, @Nullable ChildGroup group, boolean isRaw, boolean inTests) {
    if (!modulePath.equals(Prelude.MODULE_PATH)) {
      throw new IllegalStateException();
    }
    myGroup = group;
    myScope = CachingScope.make(LexicalScope.opened(group));
  }

  @Nullable
  @Override
  protected LibraryHeader loadHeader(ErrorReporter errorReporter) {
    return new LibraryHeader(Collections.singletonList(Prelude.MODULE_PATH), Collections.emptyList(), Prelude.VERSION, Range.unbound(), null, null);
  }

  @Override
  public boolean unload() {
    return false;
  }

  @Override
  public void reset() {}

  @Override
  public void resetGroup(Group group) {}

  @Override
  public void resetDefinition(LocatedReferable referable) {}

  @NotNull
  @Override
  public String getName() {
    return Prelude.LIBRARY_NAME;
  }

  @NotNull
  @Override
  public ModuleScopeProvider getDeclaredModuleScopeProvider() {
    return module -> module.equals(Prelude.MODULE_PATH) ? myScope : null;
  }

  @NotNull
  @Override
  public Collection<? extends ModulePath> getLoadedModules() {
    return isLoaded() ? Collections.singletonList(Prelude.MODULE_PATH) : Collections.emptyList();
  }

  @NotNull
  @Override
  public Collection<? extends LibraryDependency> getDependencies() {
    return Collections.emptyList();
  }

  @Nullable
  @Override
  public ChildGroup getModuleGroup(ModulePath modulePath, boolean inTests) {
    return !inTests && modulePath.equals(Prelude.MODULE_PATH) ? myGroup : null;
  }

  @Override
  public boolean containsModule(ModulePath modulePath) {
    return modulePath.equals(Prelude.MODULE_PATH);
  }
}
