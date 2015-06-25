package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.serialization.ModuleSerialization;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;

import java.io.File;
import java.io.IOException;

public class FileOutput implements Output {
  private final File myFile;

  public FileOutput(File file) {
    myFile = file;
  }

  @Override
  public boolean canRead() {
    return myFile != null && myFile.exists();
  }

  @Override
  public boolean canWrite() {
    return myFile != null;
  }

  @Override
  public long lastModified() {
    return myFile.lastModified();
  }

  @Override
  public int read(ClassDefinition classDefinition) throws IOException {
    return ModuleSerialization.readFile(myFile, classDefinition);
  }

  @Override
  public void write(ClassDefinition classDefinition) throws IOException {
    ModuleSerialization.writeFile(classDefinition, myFile);
  }
}
