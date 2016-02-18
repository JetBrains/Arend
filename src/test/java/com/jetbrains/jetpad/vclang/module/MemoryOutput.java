package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.module.output.Output;
import com.jetbrains.jetpad.vclang.serialization.ModuleDeserialization;
import com.jetbrains.jetpad.vclang.serialization.ModuleSerialization;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;

import java.io.*;
import java.util.ArrayList;

public class MemoryOutput implements Output {
  private final ResolvedName myModule;
  private final MemoryOutputSupplier.MemoryOutputEntry myEntry;

  public MemoryOutput(ResolvedName module, MemoryOutputSupplier.MemoryOutputEntry entry) {
    myModule = module;
    myEntry = entry;
  }

  @Override
  public Header getHeader() throws IOException {
    return ModuleDeserialization.readHeaderFromStream(new DataInputStream(new ByteArrayInputStream(myEntry.data)));
  }

  @Override
  public boolean canRead() {
    return myEntry != null && (myEntry.data != null || myEntry.children != null);
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
  public boolean isContainer() {
    return myEntry.data == null;
  }

  @Override
  public void readStubs() throws IOException {
    if (!isContainer())
      ModuleDeserialization.readStubsFromStream(new DataInputStream(new ByteArrayInputStream(myEntry.data)), myModule);
    else
      myModule.parent.getChild(myModule.name.name);
    for (String childName : myEntry.children) {
      myModule.toNamespace().getChild(childName);
    }
  }

  @Override
  public ModuleLoadingResult read() throws IOException {
    return new ModuleDeserialization().readStream(new DataInputStream(new ByteArrayInputStream(myEntry.data)), myModule);
  }

  @Override
  public void write() throws IOException {
    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    ModuleSerialization.writeStream(myModule, new DataOutputStream(byteStream));
    myEntry.data = byteStream.toByteArray();
    if (myEntry.children == null) {
      myEntry.children = new ArrayList<>();
    }
    myEntry.lastModified = System.nanoTime();
  }
}
