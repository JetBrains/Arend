package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.module.output.OutputSupplier;
import com.jetbrains.jetpad.vclang.term.definition.Name;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.definition.ResolvedName.toPath;

public class MemoryOutputSupplier implements OutputSupplier {
  static class MemoryOutputEntry {
    List<String> children;
    byte[] data;
    long lastModified;
  }
  private HashMap<List<Name>, MemoryOutputEntry> myOutputs = new HashMap<>();

  @Override
  public MemoryOutput getOutput(ResolvedName module) {
    List<Name> modulePath = toPath(module);
    if (!modulePath.isEmpty()) {
      getOutput(module.parent.getResolvedName());
      MemoryOutputEntry parentEntry = myOutputs.get(toPath(module.parent.getResolvedName()));
      if (parentEntry.children == null)
        parentEntry.children = new ArrayList<>();
      parentEntry.children.add(module.name.name);
    }

    if (!myOutputs.containsKey(modulePath)) {
      myOutputs.put(modulePath, new MemoryOutputEntry());
    }

    return new MemoryOutput(module, myOutputs.get(modulePath));
  }


  @Override
  public MemoryOutput locateOutput(ResolvedName module) {
    return new MemoryOutput(module, myOutputs.get(toPath(module)));
  }
}
