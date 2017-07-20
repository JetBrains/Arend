package com.jetbrains.jetpad.vclang.frontend.resolving;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.List;

public interface OpenCommand extends Abstract.SourceNode {
  ModulePath getModulePath();
  List<String> getPath();
  Abstract.Definition getResolvedClass();

  boolean isHiding();
  List<String> getNames();
}
