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
import com.jetbrains.jetpad.vclang.term.group.Group;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Represents a source that loads a binary module from an {@link InputStream} and persists it to an {@link OutputStream}.
 */
public abstract class StreamBinarySource implements PersistableSource {
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
  public boolean load(SourceLoader sourceLoader) {
    ModulePath modulePath = getModulePath();
    ModuleProtos.Module moduleProto;
    try (InputStream inputStream = getInputStream()) {
      if (inputStream == null) {
        sourceLoader.getLibrary().onModuleLoaded(modulePath, null);
        return false;
      }
      moduleProto = ModuleProtos.Module.parseFrom(inputStream);
    } catch (IOException e) {
      sourceLoader.getErrorReporter().report(new ExceptionError(e, modulePath));
      sourceLoader.getLibrary().onModuleLoaded(modulePath, null);
      return false;
    }

    try {
      ModuleDeserialization moduleDeserialization = new ModuleDeserialization(sourceLoader.getLibrary().getTypecheckerState());
      Group group = moduleDeserialization.readGroup(moduleProto.getGroup(), modulePath);
      sourceLoader.getLibrary().onModuleLoaded(modulePath, group);
      return moduleDeserialization.readModule(moduleProto, sourceLoader.getModuleScopeProvider(), module -> !sourceLoader.getLibrary().containsModule(module) || sourceLoader.load(module), sourceLoader.getErrorReporter());
    } catch (DeserializationException e) {
      sourceLoader.getErrorReporter().report(new ExceptionError(e, modulePath));
      sourceLoader.getLibrary().onModuleLoaded(modulePath, null);
      return false;
    }
  }

  @Override
  public boolean persist(SourceLibrary library, ErrorReporter errorReporter) {
    try (OutputStream outputStream = getOutputStream()) {
      ModulePath currentModulePath = getModulePath();
      if (outputStream == null) {
        errorReporter.report(new PersistingError(currentModulePath));
        return false;
      }

      Group group = library.getModuleGroup(currentModulePath);
      if (group == null) {
        errorReporter.report(LocationError.module(currentModulePath));
        return false;
      }

      ModuleProtos.Module module = new ModuleSerialization(library.getTypecheckerState(), errorReporter).writeModule(group);
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
