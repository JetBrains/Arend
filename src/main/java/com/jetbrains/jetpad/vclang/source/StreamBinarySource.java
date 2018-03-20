package com.jetbrains.jetpad.vclang.source;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.library.SourceLibrary;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.error.ExceptionError;
import com.jetbrains.jetpad.vclang.module.serialization.DeserializationException;
import com.jetbrains.jetpad.vclang.module.serialization.ModuleDeserialization;
import com.jetbrains.jetpad.vclang.module.serialization.ModuleProtos;
import com.jetbrains.jetpad.vclang.module.serialization.ModuleSerialization;
import com.jetbrains.jetpad.vclang.source.error.LocationError;
import com.jetbrains.jetpad.vclang.source.error.PersistingError;
import com.jetbrains.jetpad.vclang.term.group.ChildGroup;
import com.jetbrains.jetpad.vclang.term.group.Group;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Represents a source that loads a binary module from an {@link InputStream} and persists it to an {@link OutputStream}.
 */
public abstract class StreamBinarySource implements BinarySource {
  private ModuleProtos.Module myModuleProto;

  /**
   * Gets an input stream from which the source will be loaded.
   *
   * @return an input stream from which the source will be loaded or null if some error occurred.
   */
  @Nullable
  protected abstract InputStream getInputStream() throws IOException;

  /**
   * Gets an output stream to which the source will be persisted.
   *
   * @return an input stream from which the source will be loaded or null if the source does not support persisting.
   */
  @Nullable
  protected abstract OutputStream getOutputStream() throws IOException;

  @Override
  public boolean loadDependencyInfo(SourceLoader sourceLoader) {
    try (InputStream inputStream = getInputStream()) {
      if (inputStream == null) {
        return false;
      }

      myModuleProto = ModuleProtos.Module.parseFrom(inputStream);
      for (ModuleProtos.ModuleCallTargets moduleCallTargets : myModuleProto.getModuleCallTargetsList()) {
        ModulePath module = new ModulePath(moduleCallTargets.getNameList());
        if (sourceLoader.getLibrary().containsModule(module) && !sourceLoader.loadBinaryDependencyInfo(module)) {
          return false;
        }
      }

      return true;
    } catch (IOException e) {
      sourceLoader.getLibraryErrorReporter().report(new ExceptionError(e, getModulePath()));
      return false;
    }
  }

  @Override
  public boolean load(SourceLoader sourceLoader) {
    ModulePath modulePath = getModulePath();
    try {
      ModuleDeserialization moduleDeserialization = new ModuleDeserialization(sourceLoader.getLibrary().getTypecheckerState());
      ChildGroup group = moduleDeserialization.readGroup(myModuleProto.getGroup(), modulePath);
      sourceLoader.getLibrary().onModuleLoaded(modulePath, group, false);

      for (ModuleProtos.ModuleCallTargets moduleCallTargets : myModuleProto.getModuleCallTargetsList()) {
        ModulePath module = new ModulePath(moduleCallTargets.getNameList());
        if (sourceLoader.getLibrary().containsModule(module) && !sourceLoader.loadBinary(module)) {
          return false;
        }
      }

      return moduleDeserialization.readModule(myModuleProto, sourceLoader.getModuleScopeProvider());
    } catch (DeserializationException e) {
      sourceLoader.getLibraryErrorReporter().report(new ExceptionError(e, modulePath));
      sourceLoader.getLibrary().onModuleLoaded(modulePath, null, false);
      return false;
    }
  }

  @Override
  public boolean persist(SourceLibrary library, ErrorReporter errorReporter) {
    ModulePath currentModulePath = getModulePath();
    Group group = library.getModuleGroup(currentModulePath);
    if (group == null) {
      errorReporter.report(LocationError.module(currentModulePath));
      return false;
    }

    try (OutputStream outputStream = getOutputStream()) {
      if (outputStream == null) {
        errorReporter.report(new PersistingError(currentModulePath));
        return false;
      }

      ModuleProtos.Module module = new ModuleSerialization(library.getTypecheckerState(), errorReporter).writeModule(group, currentModulePath);
      if (module == null) {
        return false;
      }

      module.writeTo(outputStream);
      return true;
    } catch (IOException e) {
      errorReporter.report(new ExceptionError(e, getModulePath()));
      return false;
    }
  }
}
