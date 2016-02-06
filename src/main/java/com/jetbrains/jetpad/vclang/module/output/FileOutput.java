package com.jetbrains.jetpad.vclang.module.output;

import com.jetbrains.jetpad.vclang.module.*;
import com.jetbrains.jetpad.vclang.serialization.ModuleDeserialization;
import com.jetbrains.jetpad.vclang.serialization.ModuleSerialization;

import java.io.File;
import java.io.IOException;

public class FileOutput implements Output {
  private final boolean myReadOnly;
  private final FileModuleID myModule;
  private final File myFile;
  private final ModuleDeserialization myModuleDeserialization;

  public FileOutput(ModuleDeserialization moduleDeserialization, FileModuleID module, File file, boolean readOnly) {
    myReadOnly = readOnly;
    myModule = module;
    myFile = file;
    myModuleDeserialization = moduleDeserialization;
  }

  @Override
  public Header getHeader() throws IOException {
    return ModuleDeserialization.readHeaderFromFile(myFile, myModule);
  }

  @Override
  public boolean canRead() {
    return myFile.exists() && myFile.canRead();
  }

  @Override
  public boolean canWrite() {
    return !myReadOnly && myFile.canWrite();
  }

  @Override
  public long lastModified() {
    return myFile.lastModified();
  }

  @Override
  public void readStubs() throws IOException {
    if (myFile.exists()) {
      ModuleDeserialization.readStubsFromFile(myFile, myModule);
    }
  }

  @Override
  public ModuleLoader.Result read() throws IOException {
    return myModuleDeserialization.readFile(myFile, myModule);
  }

  @Override
  public void write() throws IOException {
    ModuleSerialization.writeFile(myModule, myFile);
  }
}
