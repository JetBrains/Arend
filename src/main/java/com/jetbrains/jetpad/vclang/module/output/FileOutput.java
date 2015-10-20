package com.jetbrains.jetpad.vclang.module.output;

import com.jetbrains.jetpad.vclang.module.ModuleLoadingResult;
import com.jetbrains.jetpad.vclang.serialization.ModuleDeserialization;
import com.jetbrains.jetpad.vclang.serialization.ModuleSerialization;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FileOutput implements Output {
  private final File myFile;
  private final ResolvedName myModule;
  private final ModuleDeserialization myModuleDeserialization;

  public FileOutput(ModuleDeserialization moduleDeserialization, ResolvedName module, File file) {
    myFile = file;
    myModule = module;
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
  public List<List<String>> getDependencies() {
    return null;
  }

  @Override
  public ModuleLoadingResult read() throws IOException {
    return myModuleDeserialization.readFile(myFile, myModule);
  }

  @Override
  public void write() throws IOException {
    ModuleSerialization.writeFile(myModule, myFile);
  }
}
