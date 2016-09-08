package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.module.ModulePath;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class NameModuleSourceId implements SerializableModuleSourceId {
  private final String myName;

  public NameModuleSourceId(String name) {
    this.myName = name;
  }

  @Override
  public ModulePath getModulePath() {
    return new ModulePath(myName);
  }

  @Override
  public void serialize(DataOutputStream stream) throws IOException {
    stream.writeUTF(myName);
  }

  @Override
  public NameModuleSourceId deserialize(DataInputStream stream) throws IOException {
    return new NameModuleSourceId(stream.readUTF());
  }

  @Override
  public boolean equals(Object o) {
    return o == this || o instanceof NameModuleSourceId && myName.equals(((NameModuleSourceId) o).myName);
  }

  @Override
  public int hashCode() {
    return myName.hashCode();
  }
}
