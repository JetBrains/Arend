package com.jetbrains.jetpad.vclang.module.source.file;


import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.source.SerializableModuleSourceId;
import com.jetbrains.jetpad.vclang.module.utils.FileOperations;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileModuleSourceId implements SerializableModuleSourceId {
  private final ModulePath myModulePath;

  public FileModuleSourceId(ModulePath modulePath) {
    myModulePath = modulePath;
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
  public FileModuleSourceId deserialize(DataInputStream stream) throws IOException {
    int pathSize = stream.readInt();
    List<String> path = new ArrayList<>(pathSize);
    for (int i = 0; i < pathSize; i++) {
      path.add(stream.readUTF());
    }
    return new FileModuleSourceId(new ModulePath(path));
  }

  @Override
  public boolean equals(Object o) {
    return o == this || o instanceof FileModuleSourceId && ((FileModuleSourceId) o).myModulePath.equals(myModulePath);
  }

  @Override
  public int hashCode() {
    return myModulePath.hashCode();
  }

  @Override
  public String toString() {
    return FileOperations.getFile(new File("."), myModulePath, FileOperations.EXTENSION).toString();
  }
}
