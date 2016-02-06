package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.module.output.OutputSupplier;

import java.util.HashMap;

public class MemoryOutputSupplier implements OutputSupplier {
  static class MemoryOutputEntry {
    byte[] data;
    long lastModified;
  }
  private HashMap<ModulePath, MemoryOutputEntry> myOutputs = new HashMap<>();

  @Override
  public MemoryOutput getOutput(ModuleID moduleID) {
    if (!(moduleID instanceof PathModuleID)) {
      return null;
    }

    if (!myOutputs.containsKey(moduleID.getModulePath())) {
      myOutputs.put(moduleID.getModulePath(), new MemoryOutputEntry());
    }

    return new MemoryOutput((PathModuleID) moduleID, myOutputs.get(moduleID.getModulePath()));
  }


  @Override
  public PathModuleID locateModule(ModulePath modulePath) {
    return myOutputs.containsKey(modulePath) ? new PathModuleID(modulePath) : null;
  }
}
