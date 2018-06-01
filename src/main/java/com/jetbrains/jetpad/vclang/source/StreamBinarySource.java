package com.jetbrains.jetpad.vclang.source;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.library.SourceLibrary;
import com.jetbrains.jetpad.vclang.library.error.LibraryError;
import com.jetbrains.jetpad.vclang.library.error.PartialModuleError;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.error.ExceptionError;
import com.jetbrains.jetpad.vclang.module.serialization.DeserializationException;
import com.jetbrains.jetpad.vclang.module.serialization.ModuleDeserialization;
import com.jetbrains.jetpad.vclang.module.serialization.ModuleProtos;
import com.jetbrains.jetpad.vclang.module.serialization.ModuleSerialization;
import com.jetbrains.jetpad.vclang.naming.reference.converter.ReferableConverter;
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
  private ModuleDeserialization myModuleDeserialization;

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
  public boolean preload(SourceLoader sourceLoader) {
    SourceLibrary library = sourceLoader.getLibrary();
    ModulePath modulePath = getModulePath();
    try (InputStream inputStream = getInputStream()) {
      if (inputStream == null) {
        return false;
      }

      ModuleProtos.Module moduleProto = ModuleProtos.Module.parseFrom(inputStream);
      boolean isComplete = moduleProto.getComplete();
      if (!isComplete && !library.hasRawSources()) {
        sourceLoader.getLibraryErrorReporter().report(new PartialModuleError(getModulePath()));
        return false;
      }

      for (ModuleProtos.ModuleCallTargets moduleCallTargets : moduleProto.getModuleCallTargetsList()) {
        ModulePath module = new ModulePath(moduleCallTargets.getNameList());
        if (library.containsModule(module) && !sourceLoader.preloadBinary(module)) {
          return false;
        }
      }

      ReferableConverter referableConverter = library.getReferableConverter();
      myModuleDeserialization = new ModuleDeserialization(moduleProto, library.getTypecheckerState(), referableConverter);

      ChildGroup group;
      if (referableConverter == null) {
        group = myModuleDeserialization.readGroup(modulePath);
        library.onGroupLoaded(modulePath, group, false);
      } else {
        group = library.getModuleGroup(modulePath);
        if (group == null) {
          sourceLoader.getLibraryErrorReporter().report(LibraryError.moduleNotFound(modulePath, library.getName()));
          library.onGroupLoaded(modulePath, null, false);
          return false;
        }
        myModuleDeserialization.readDefinitions(group);
      }

      return true;
    } catch (IOException | DeserializationException e) {
      sourceLoader.getLibraryErrorReporter().report(new ExceptionError(e, getModulePath(), true));
      if (!library.hasRawSources()) {
        library.onGroupLoaded(getModulePath(), null, false);
      }
      return false;
    }
  }

  @Override
  public boolean load(SourceLoader sourceLoader) {
    SourceLibrary library = sourceLoader.getLibrary();
    try {
      for (ModuleProtos.ModuleCallTargets moduleCallTargets : myModuleDeserialization.getModuleProto().getModuleCallTargetsList()) {
        ModulePath module = new ModulePath(moduleCallTargets.getNameList());
        if (library.containsModule(module) && !sourceLoader.fillInBinary(module)) {
          return false;
        }
      }

      myModuleDeserialization.readModule(sourceLoader.getModuleScopeProvider(), library.getDependencyListener());
      library.onBinaryLoaded(getModulePath(), myModuleDeserialization.getModuleProto().getComplete());
      myModuleDeserialization = null;
      return true;
    } catch (DeserializationException e) {
      sourceLoader.getLibraryErrorReporter().report(new ExceptionError(e, getModulePath(), true));
      if (!library.hasRawSources()) {
        library.onGroupLoaded(getModulePath(), null, false);
      }
      return false;
    }
  }

  @Override
  public boolean persist(SourceLibrary library, ReferableConverter referableConverter, ErrorReporter errorReporter) {
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

      ModuleProtos.Module module = new ModuleSerialization(library.getTypecheckerState(), errorReporter).writeModule(group, currentModulePath, referableConverter);
      if (module == null) {
        return false;
      }

      module.writeTo(outputStream);
      return true;
    } catch (Exception e) {
      errorReporter.report(new ExceptionError(e, getModulePath(), false));
      return false;
    }
  }
}
