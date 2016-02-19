package com.jetbrains.jetpad.vclang.module;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PathModuleID implements ModuleID {
  private final ModulePath myModulePath;

  public PathModuleID(ModulePath modulePath) {
    this.myModulePath = modulePath;
  }

  public PathModuleID(String... path) {
    this.myModulePath = new ModulePath(Arrays.asList(path));
  }

  @Override
  public ModulePath getModulePath() {
    return myModulePath;
  }

  @Override
  public void serialize(DataOutputStream stream) throws IOException {
    String[] path = myModulePath.list();
    stream.writeInt(path.length);
    for (String str : path) {
      stream.writeUTF(str);
    }
  }

  @Override
  public ModuleID deserialize(DataInputStream stream) throws IOException {
    int pathLength = stream.readInt();
    List<String> path = new ArrayList<>();
    for (int i = 0; i < pathLength; i++) {
      path.add(stream.readUTF());
    }
    return new PathModuleID(new ModulePath(path));
  }

  @Override
  public boolean equals(Object o) {
    return o == this || o instanceof PathModuleID && myModulePath.equals(((PathModuleID) o).getModulePath());
  }

  @Override
  public int hashCode() {
    return myModulePath.hashCode();
  }
}
