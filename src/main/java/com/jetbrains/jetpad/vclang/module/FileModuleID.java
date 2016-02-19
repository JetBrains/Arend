package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.module.utils.FileOperations;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileModuleID implements ModuleID {
  private final byte[] mySha256;
  private final ModulePath myModulePath;

  public FileModuleID(byte[] sha256, ModulePath modulePath) {
    mySha256 = sha256;
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
    stream.write(mySha256);
  }

  @Override
  public FileModuleID deserialize(DataInputStream stream) throws IOException {
    int pathSize = stream.readInt();
    List<String> path = new ArrayList<>(pathSize);
    for (int i = 0; i < pathSize; i++) {
      path.add(stream.readUTF());
    }
    byte[] sha256 = new byte[16];
    stream.readFully(sha256);
    return new FileModuleID(sha256, new ModulePath(path));
  }

  @Override
  public boolean equals(Object o) {
    return o == this || o instanceof FileModuleID && ((FileModuleID) o).myModulePath.equals(myModulePath) && Arrays.equals(mySha256, ((FileModuleID) o).mySha256);
  }

  @Override
  public int hashCode() {
    return myModulePath.hashCode() + 23 * Arrays.hashCode(mySha256);
  }

  public byte[] getSha256() {
    return mySha256;
  }

  @Override
  public String toString() {
    return myModulePath + "{" + FileOperations.sha256ToStr(mySha256).substring(0, 5) + "}";
  }
}
