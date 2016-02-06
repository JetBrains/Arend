package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.module.*;
import com.jetbrains.jetpad.vclang.module.utils.FileOperations;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FileSourceSupplier implements SourceSupplier {
  private final File myDirectory;
  private final ModuleLoader myModuleLoader;
  private final ErrorReporter myErrorReporter;
  private final Map<ModulePath, byte[]> myCache = new HashMap<>();

  public FileSourceSupplier(ModuleLoader moduleLoader, ErrorReporter errorReporter, File directory) {
    myModuleLoader = moduleLoader;
    myErrorReporter = errorReporter;
    myDirectory = directory;
  }

  @Override
  public Source getSource(ModuleID module) {
    if (!(module instanceof FileModuleID))
      return null;
    if (!Arrays.equals(getSha256Hash(module.getModulePath()), ((FileModuleID) module).getSha256())) {
      return null;
    }
    return new FileSource(myModuleLoader, myErrorReporter, (FileModuleID) module, FileOperations.getFile(myDirectory, module.getModulePath(), FileOperations.EXTENSION));
  }

  private byte[] getSha256Hash(ModulePath modulePath) {
    byte[] sha256 = myCache.get(modulePath);
    if (sha256 != null)
      return sha256;

    myCache.put(modulePath, FileOperations.calcSha256(FileOperations.getFile(myDirectory, modulePath, FileOperations.EXTENSION)));
    return myCache.get(modulePath);
  }

  @Override
  public FileModuleID locateModule(ModulePath modulePath) {
    byte[] sha256 = getSha256Hash(modulePath);
    if (sha256.length == 0) {
      return null;
    }
    return new FileModuleID(sha256, modulePath);
  }
}
