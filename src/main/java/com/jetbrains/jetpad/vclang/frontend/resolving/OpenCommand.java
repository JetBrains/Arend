package com.jetbrains.jetpad.vclang.frontend.resolving;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.term.Abstract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface OpenCommand extends Abstract.SourceNode {
  @Nullable ModulePath getModulePath();
  @Nonnull List<String> getPath();
  @Nullable Abstract.Definition getResolvedClass();

  boolean isHiding();
  @Nullable List<String> getNames();
}
