package org.arend.repl;

import org.arend.ext.module.ModulePath;
import org.arend.library.BaseLibrary;
import org.arend.library.LibraryDependency;
import org.arend.module.FullModulePath;
import org.arend.term.group.ChildGroup;
import org.arend.typechecking.TypecheckerState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ReplLibrary extends BaseLibrary {
  public static final @NotNull FullModulePath replModulePath = new FullModulePath(null, FullModulePath.LocationKind.TEST, Collections.singletonList("Repl"));
  private final @NotNull List<LibraryDependency> dependencies = new ArrayList<>();
  private ChildGroup group = null;

  public ReplLibrary(TypecheckerState typecheckerState) {
    super(typecheckerState);
  }

  @Override
  public @NotNull String getName() {
    return replModulePath.getLastName();
  }

  @Override
  public @NotNull Collection<? extends ModulePath> getLoadedModules() {
    return Collections.singletonList(replModulePath);
  }

  @Override
  public @NotNull Collection<? extends LibraryDependency> getDependencies() {
    return dependencies;
  }

  public boolean addDependency(@NotNull LibraryDependency dependency) {
    return dependencies.add(dependency);
  }

  @Override
  public @Nullable ChildGroup getModuleGroup(ModulePath modulePath) {
    return containsModule(modulePath) ? getGroup() : null;
  }

  public ChildGroup getGroup() {
    return group;
  }

  public void setGroup(ChildGroup group) {
    this.group = group;
  }

  @Override
  public boolean containsModule(ModulePath modulePath) {
    return Objects.equals(modulePath, replModulePath);
  }
}
