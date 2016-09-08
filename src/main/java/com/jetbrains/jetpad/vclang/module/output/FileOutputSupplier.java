package com.jetbrains.jetpad.vclang.module.output;

public class FileOutputSupplier {
  // FIXME[serial]
  /*
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
    if (!(module instanceof FileModuleSourceId)) {
      return null;
    }
    for (File dir : myLibDirs) {
      File file = FileOperations.getFile(dir, module.getModulePath(), FileOperations.SERIALIZED_EXTENSION);
      if (file.exists()) {
        return new FileOutput(myModuleDeserialization, (FileModuleSourceId) module, file, true);
      }
    }
    File file = FileOperations.getFile(myDirectory, module.getModulePath(), FileOperations.SERIALIZED_EXTENSION);
    return new FileOutput(myModuleDeserialization, (FileModuleSourceId) module, file, false);
  }

  @Override
  public FileModuleSourceId locateModule(ModulePath modulePath) {
    return new FileModuleSourceId(modulePath);
  }
  */
}
