package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.serialization.ModuleSerialization;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;

import java.io.File;
import java.io.IOException;

public class FileOutput implements Output {
  private final File myFile;
  private final ModuleSerialization myModuleSerialization;

  public FileOutput(ModuleSerialization moduleSerialization, File file) {
    myFile = file;
    myModuleSerialization = moduleSerialization;
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
    return myModuleSerialization.readFile(myFile, classDefinition);
  }

  @Override
  public void write(ClassDefinition classDefinition) throws IOException {
    myModuleSerialization.writeFile(classDefinition, myFile);
  }
}
