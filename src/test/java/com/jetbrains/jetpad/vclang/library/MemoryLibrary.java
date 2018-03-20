package com.jetbrains.jetpad.vclang.library;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.source.BinarySource;
import com.jetbrains.jetpad.vclang.source.Source;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class MemoryLibrary extends UnmodifiableSourceLibrary {
  private final Map<ModulePath, MemoryRawSource> myRawSources = new LinkedHashMap<>();
  private final Map<ModulePath, BinarySource> myBinarySources = new LinkedHashMap<>();

  protected MemoryLibrary(TypecheckerState typecheckerState) {
    super("test_library", typecheckerState);
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
    return new LibraryHeader(new ArrayList<>(myRawSources.keySet()), Collections.emptyList());
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
}
