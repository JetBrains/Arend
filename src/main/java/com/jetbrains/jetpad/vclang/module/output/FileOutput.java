package com.jetbrains.jetpad.vclang.module.output;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.serialization.ModuleDeserialization;
import com.jetbrains.jetpad.vclang.serialization.ModuleSerialization;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;

import java.io.File;
import java.io.IOException;

public class FileOutput implements Output {
  private final File myFile;
  private final ModuleDeserialization myModuleDeserialization;

  public FileOutput(ModuleDeserialization moduleDeserialization, File file) {
    myFile = file;
    myModuleDeserialization = moduleDeserialization;
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
  public int read(Namespace namespace, ClassDefinition classDefinition) throws IOException {
    return myModuleDeserialization.readFile(myFile, namespace, classDefinition);
  }

  @Override
  public void write(Namespace namespace, ClassDefinition classDefinition) throws IOException {
    ModuleSerialization.writeFile(namespace, classDefinition, myFile);
  }
}
