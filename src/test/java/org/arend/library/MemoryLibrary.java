package org.arend.library;

import org.arend.ext.error.ErrorReporter;
import org.arend.ext.module.ModulePath;
import org.arend.source.BinarySource;
import org.arend.source.Source;
import org.arend.term.group.ChildGroup;
import org.arend.term.group.Group;
import org.arend.typechecking.TypecheckerState;
import org.arend.util.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MemoryLibrary extends UnmodifiableSourceLibrary {
  private final Map<ModulePath, MemoryRawSource> myRawSources = new LinkedHashMap<>();
  private final Map<ModulePath, BinarySource> myBinarySources = new LinkedHashMap<>();

  protected MemoryLibrary(TypecheckerState typecheckerState) {
    super("test_library", typecheckerState);
  }

  public ChildGroup getModuleGroup(ModulePath modulePath) {
    return getModuleGroup(modulePath, false);
  }

  @Nullable
  @Override
  public Source getRawSource(ModulePath modulePath) {
    return myRawSources.get(modulePath);
  }

  @Nullable
  @Override
  public BinarySource getBinarySource(ModulePath modulePath) {
    return myBinarySources.get(modulePath);
  }

  @Nullable
  @Override
  protected LibraryHeader loadHeader(ErrorReporter errorReporter) {
    return new LibraryHeader(new ArrayList<>(myRawSources.keySet()), Collections.emptyList(), Range.unbound(), null, null);
  }

  public void addModule(ModulePath module, String text) {
    myRawSources.put(module, new MemoryRawSource(module, text));
    myBinarySources.put(module, new MemoryBinarySource(module));
  }

  public void updateModule(ModulePath module, String text, boolean updateVersion) {
    myRawSources.put(module, new MemoryRawSource(module, text, updateVersion ? 1 : 0));
  }

  public void removeRawSource(ModulePath module) {
    myRawSources.remove(module);
  }

  public void removeBinarySource(ModulePath module) {
    myBinarySources.put(module, new MemoryBinarySource(module));
  }

  @NotNull
  @Override
  public List<? extends LibraryDependency> getDependencies() {
    return Collections.emptyList();
  }

  @Override
  public void reset() {
    super.reset();
    for (Map.Entry<ModulePath, MemoryRawSource> entry : myRawSources.entrySet()) {
      entry.setValue(new MemoryRawSource(entry.getValue()));
    }
  }
}
