package com.jetbrains.jetpad.vclang;

import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModuleSerialization {
  static private final byte[] SIGNATURE = { 0x00, (byte) 0xb1, (byte) 0xce, (byte) 0xd1 };
  static private final int VERSION = 0;

  static public void writeClass(ClassDefinition def, Path outputDir) throws IOException {
    Files.createDirectories(outputDir);
    DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputDir.resolve(def.getName() + ".vcc").toFile())));
    stream.write(SIGNATURE);
    stream.writeInt(VERSION);
    stream.close();
  }

  static public ClassDefinition readClass(Path file) throws IOException, IncorrectFormat {
    DataInputStream stream = new DataInputStream(new BufferedInputStream(new FileInputStream(file.toFile())));
    byte[] signature = new byte[4];
    stream.readFully(signature);
    if (signature != SIGNATURE) {
      throw new IncorrectFormat();
    }
    int version = stream.readInt();
    if (version != VERSION) {
      throw new WrongVersion(version);
    }
    return null;
  }

  static class IncorrectFormat extends Exception {
    @Override
    public String toString() {
      return "Incorrect format";
    }
  }

  static class WrongVersion extends IncorrectFormat {
    private final int myVersion;

    WrongVersion(int version) {
      myVersion = version;
    }

    @Override
    public String toString() {
      return "Version of the file format (" + myVersion + ") differs from the version of the program + (" + VERSION + ")";
    }
  }
}
