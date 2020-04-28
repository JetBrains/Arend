package org.arend.repl;

import org.arend.ext.error.ErrorReporter;
import org.arend.ext.module.ModulePath;
import org.arend.library.LibraryDependency;
import org.arend.library.LibraryHeader;
import org.arend.library.UnmodifiableSourceLibrary;
import org.arend.module.FullModulePath;
import org.arend.source.BinarySource;
import org.arend.term.group.ChildGroup;
import org.arend.typechecking.TypecheckerState;
import org.arend.util.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class ReplLibrary extends UnmodifiableSourceLibrary {
  public static final @NotNull FullModulePath replModulePath = new FullModulePath(null, FullModulePath.LocationKind.TEST, Collections.singletonList("Repl"));
  private final @NotNull List<LibraryDependency> myDependencies = new ArrayList<>();
  protected final @NotNull Path currentDir = Paths.get(".").toAbsolutePath();

  public ReplLibrary(@NotNull TypecheckerState typecheckerState) {
    super("Repl", typecheckerState);
  }

  @Override
  public @Nullable BinarySource getBinarySource(ModulePath modulePath) {
    return null;
  }

  @Override
  protected @Nullable LibraryHeader loadHeader(ErrorReporter errorReporter) {
    return new LibraryHeader(List.copyOf(getLoadedModules()), Collections.emptyList(), Range.unbound(), null, null);
  }

  @Override
  public @NotNull Collection<? extends LibraryDependency> getDependencies() {
    return myDependencies;
  }

  public boolean addDependency(@NotNull LibraryDependency dependency) {
    return myDependencies.add(dependency);
  }

  @Override
  public boolean supportsPersisting() {
    return false;
  }

  public @Nullable ChildGroup getGroup() {
    return getModuleGroup(replModulePath);
  }

  public void setGroup(@Nullable ChildGroup group) {
    onGroupLoaded(replModulePath, group, true);
  }
}
