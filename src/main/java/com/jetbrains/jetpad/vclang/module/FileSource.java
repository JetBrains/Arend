package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileSource extends ParseSource {
  private final File myFile;

  public FileSource(ModuleLoader moduleLoader, Module module, File file) {
    super(moduleLoader, module);
    myFile = file;
  }

  @Override
  public boolean isAvailable() {
    return myFile != null && myFile.exists();
  }

  @Override
  public long lastModified() {
    return myFile.lastModified();
  }

  @Override
  public boolean load(ClassDefinition classDefinition) throws IOException {
    setStream(new FileInputStream(myFile));
    return super.load(classDefinition);
  }
}
