package com.jetbrains.jetpad.vclang.module;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class NameModuleID implements SerializableModuleID {
  private final String myName;

  public NameModuleID(String name) {
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
  public ModuleID deserialize(DataInputStream stream) throws IOException {
    return new NameModuleID(stream.readUTF());
  }

  @Override
  public boolean equals(Object o) {
    return o == this || o instanceof NameModuleID && myName.equals(((NameModuleID) o).myName);
  }

  @Override
  public int hashCode() {
    return myName.hashCode();
  }
}
