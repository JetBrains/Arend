package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Namespace;

import java.io.IOException;
import java.util.List;

public class CompositeSource implements Source {
  private final List<Source> mySources;
  private int myIndex = -1;

  public CompositeSource(List<Source> sources) {
    mySources = sources;
  }

  @Override
  public boolean isAvailable() {
    for (int i = 0; i < mySources.size(); i++) {
      Source source = mySources.get(i);
      if (source.isAvailable()) {
        myIndex = i;
        return true;
      }
    }
    return false;
  }

  @Override
  public long lastModified() {
    return myIndex != -1 ? mySources.get(myIndex).lastModified() : 0;
  }

  @Override
  public boolean load(Namespace namespace, ClassDefinition classDefinition) throws IOException {
    return myIndex != -1 && mySources.get(myIndex).load(namespace, classDefinition);
  }
}
