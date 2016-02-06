package com.jetbrains.jetpad.vclang.module.output;

import com.jetbrains.jetpad.vclang.module.FileModuleID;
import com.jetbrains.jetpad.vclang.module.ModuleID;
import com.jetbrains.jetpad.vclang.module.utils.FileOperations;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.serialization.ModuleDeserialization;

import java.io.File;
import java.util.*;

public class FileOutputSupplier implements OutputSupplier {
  private final File myDirectory;
  private final List<File> myLibDirs;
  private final ModuleDeserialization myModuleDeserialization;

  public FileOutputSupplier(ModuleDeserialization moduleDeserialization, File directory, List<File> libDirs) {
    myDirectory = directory;
    myLibDirs = libDirs;
    myModuleDeserialization = moduleDeserialization;
  }

  @Override
  public Output getOutput(ModuleID module) {
    if (!(module instanceof FileModuleID)) {
      return null;
    }
    String fileName = FileOperations.sha256ToStr(((FileModuleID) module).getSha256()) + FileOperations.SERIALIZED_EXTENSION;
    for (File dir : myLibDirs) {
      File file = new File(FileOperations.getFile(dir, module.getModulePath(), ""), fileName);
      if (file.exists()) {
        return new FileOutput(myModuleDeserialization, (FileModuleID) module, file, true);
      }
    }
    File file = new File(FileOperations.getFile(myDirectory, module.getModulePath(), ""), fileName);
    return new FileOutput(myModuleDeserialization, (FileModuleID) module, file, false);
  }

  private static byte[] getValidOutputHash(File dir) {
    File[] children = dir.listFiles();
    if (children == null || children.length == 0) {
      return null;
    }
    for (File file : children) {
      String fileName = FileOperations.getExtFileName(file, FileOperations.SERIALIZED_EXTENSION);
      if (fileName != null) {
        byte[] hash = FileOperations.strToSha256(fileName);
        if (hash != null) {
          return hash;
        }
      }
    }

    return null;
  }

  private byte[] getValidOutputHash(ModulePath modulePath) {
    byte[] resultHash = getValidOutputHash(FileOperations.getFile(myDirectory, modulePath, ""));
    if (resultHash != null)
      return resultHash;
    for (File dir : myLibDirs) {
      resultHash= getValidOutputHash(FileOperations.getFile(dir, modulePath, ""));
      if (resultHash != null) {
        return resultHash;
      }
    }
    return null;
  }


  @Override
  public FileModuleID locateModule(ModulePath modulePath) {
    byte[] resultHash = getValidOutputHash(modulePath);
    return resultHash == null ? null : new FileModuleID(resultHash, modulePath);
  }
}
