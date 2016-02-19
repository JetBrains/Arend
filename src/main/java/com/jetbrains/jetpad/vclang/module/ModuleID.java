package com.jetbrains.jetpad.vclang.module;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface ModuleID {
  ModulePath getModulePath();

  void serialize(DataOutputStream stream) throws IOException;
  ModuleID deserialize(DataInputStream stream) throws IOException;
}
