package com.jetbrains.jetpad.vclang.module.source;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface SerializableModuleSourceId extends ModuleSourceId {
  void serialize(DataOutputStream stream) throws IOException;
  ModuleSourceId deserialize(DataInputStream stream) throws IOException;
}
