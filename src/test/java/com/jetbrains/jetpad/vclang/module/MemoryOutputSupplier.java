package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.module.output.OutputSupplier;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;

import java.util.HashMap;

public class MemoryOutputSupplier implements OutputSupplier {
  private HashMap<ResolvedName, MemoryOutput> myOutputs = new HashMap<>();

  @Override
  public MemoryOutput getOutput(ResolvedName module) {
    if (!myOutputs.containsKey(module)) {
      myOutputs.put(module, new MemoryOutput(module));
    }
    return myOutputs.get(module);
  }

  @Override
  public MemoryOutput locateOutput(ResolvedName module) {
    return myOutputs.get(module);
  }
}
