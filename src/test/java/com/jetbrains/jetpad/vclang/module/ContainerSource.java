package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.module.source.Source;
import com.jetbrains.jetpad.vclang.term.definition.Name;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ContainerSource implements Source {
  private final ResolvedName myModule;
  private final List<String> myChildren;

  public ContainerSource(ResolvedName module) {
    myChildren = new ArrayList<>();
    myModule = module;
  }

  @Override
  public boolean isAvailable() {
    return true;
  }

  @Override
  public long lastModified() {
    return Long.MAX_VALUE;
  }

  @Override
  public boolean isContainer() {
    return true;
  }

  void addChild(String name) {
    myChildren.add(name);
  }

  @Override
  public ModuleLoadingResult load(boolean childrenOnly) throws IOException {
    if (childrenOnly) {
      for (String childName : myChildren) {
        myModule.toNamespace().getChild(new Name(childName));
      }
    } else {
      myModule.parent.getChild(myModule.name);
    }
    return new ModuleLoadingResult(myModule.toNamespaceMember(), false, 0);
  }
}
