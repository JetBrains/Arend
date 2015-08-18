package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Namespace;

import java.io.File;
import java.io.IOException;

public class DirectorySource implements Source {
  private final File myDirectory;

  public DirectorySource(File directory) {
    myDirectory = directory;
  }

  @Override
  public boolean isAvailable() {
    return myDirectory != null && myDirectory.exists();
  }

  @Override
  public long lastModified() {
    return Long.MAX_VALUE;
  }

  @Override
  public boolean load(Namespace namespace, ClassDefinition classDefinition) throws IOException {
    return false;
  }
}
