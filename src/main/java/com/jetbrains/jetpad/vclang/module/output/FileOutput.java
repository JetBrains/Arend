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
  private final List<String> myChildren;
  private final ResolvedName myModule;
  private final ModuleDeserialization myModuleDeserialization;

  public FileOutput(ModuleDeserialization moduleDeserialization, ResolvedName module, File file, List<String> children) {
    myFile = file;
    myChildren = children;
    myModule = module;
    myModuleDeserialization = moduleDeserialization;
  }

  @Override
  public Header getHeader() throws IOException {
    return ModuleDeserialization.readHeaderFromFile(myFile);
  }

  @Override
  public boolean canRead() {
    return (myFile != null && myFile.exists() || myChildren != null);
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
  public boolean isContainer() {
    return (myFile == null || !myFile.exists()) && myChildren != null;
  }

  @Override
  public void readStubs() throws IOException {
    if (myFile != null && myFile.exists()) {
      ModuleDeserialization.readStubsFromFile(myFile, myModule);
    } else {
      myModule.parent.getChild(myModule.name.name);
    }
    for (String childName : myChildren) {
      myModule.toNamespace().getChild(childName);
    }
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
