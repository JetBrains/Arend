package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.module.output.Output;
import com.jetbrains.jetpad.vclang.serialization.ModuleDeserialization;
import com.jetbrains.jetpad.vclang.serialization.ModuleSerialization;

import java.io.*;
import java.util.ArrayList;

public class MemoryOutput implements Output {
  private final PathModuleID myModule;
  private final MemoryOutputSupplier.MemoryOutputEntry myEntry;

  public MemoryOutput(PathModuleID moduleID, MemoryOutputSupplier.MemoryOutputEntry entry) {
    myModule = moduleID;
    myEntry = entry;
  }

  @Override
  public Header getHeader() throws IOException {
    return ModuleDeserialization.readHeaderFromStream(new DataInputStream(new ByteArrayInputStream(myEntry.data)), myModule);
  }

  @Override
  public boolean canRead() {
    return myEntry != null && myEntry.data != null;
  }

  @Override
  public boolean canWrite() {
    return myEntry != null;
  }

  @Override
  public long lastModified() {
    return myEntry.lastModified;
  }

  @Override
  public void readStubs() throws IOException {
    ModuleDeserialization.readStubsFromStream(new DataInputStream(new ByteArrayInputStream(myEntry.data)), myModule);
  }

  @Override
  public ModuleLoader.Result read() throws IOException {
    return new ModuleDeserialization().readStream(new DataInputStream(new ByteArrayInputStream(myEntry.data)), myModule);
  }

  @Override
  public void write() throws IOException {
    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    ModuleSerialization.writeStream(myModule, new DataOutputStream(byteStream));
    myEntry.data = byteStream.toByteArray();
    myEntry.lastModified = System.nanoTime();
  }
}
