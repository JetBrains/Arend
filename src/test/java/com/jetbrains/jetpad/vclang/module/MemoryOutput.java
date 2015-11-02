package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.module.output.Output;
import com.jetbrains.jetpad.vclang.serialization.ModuleDeserialization;
import com.jetbrains.jetpad.vclang.serialization.ModuleSerialization;
import com.jetbrains.jetpad.vclang.term.definition.Name;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MemoryOutput implements Output {
  private final ResolvedName myModule;
  private final List<String> myChildren;
  private long myLastModified;
  private byte[] data = null;

  public MemoryOutput(ResolvedName module) {
    myModule = module;
    myChildren = new ArrayList<>();
  }

  @Override
  public Header getHeader() throws IOException {
    return ModuleDeserialization.readHeaderFromStream(new DataInputStream(new ByteArrayInputStream(data)));
  }

  @Override
  public boolean canRead() {
    return data == null;
  }

  @Override
  public boolean canWrite() {
    return true;
  }

  @Override
  public long lastModified() {
    return myLastModified;
  }

  @Override
  public boolean isContainer() {
    return data == null;
  }

  void addChildren(String children) {
    myChildren.add(children);
  }

  @Override
  public void readStubs() throws IOException {
    ModuleDeserialization.readStubsFromStream(new DataInputStream(new ByteArrayInputStream(data)), myModule);
    for (String childName : myChildren) {
      myModule.toNamespace().getChild(new Name(childName));
    }
  }

  @Override
  public ModuleLoadingResult read() throws IOException {
    return new ModuleDeserialization().readStream(new DataInputStream(new ByteArrayInputStream(data)), myModule);
  }

  @Override
  public void write() throws IOException {
    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    ModuleSerialization.writeStream(myModule, new DataOutputStream(new ByteArrayOutputStream()));
    data = byteStream.toByteArray();
    myLastModified = System.nanoTime();
  }
}
