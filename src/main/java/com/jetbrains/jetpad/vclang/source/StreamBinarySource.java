package com.jetbrains.jetpad.vclang.source;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.library.PersistableSourceLibrary;
import com.jetbrains.jetpad.vclang.library.resolver.DefinitionLocator;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.error.ExceptionError;
import com.jetbrains.jetpad.vclang.module.serialization.DeserializationException;
import com.jetbrains.jetpad.vclang.module.serialization.ModuleDeserialization;
import com.jetbrains.jetpad.vclang.module.serialization.ModuleProtos;
import com.jetbrains.jetpad.vclang.module.serialization.ModuleSerialization;
import com.jetbrains.jetpad.vclang.source.error.LocationError;
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
   * @return an input stream from which the source will be loaded or null if some error occurred.
   */
  @Nullable
  protected abstract OutputStream getOutputStream() throws IOException;

  @Override
  public boolean load(SourceLoader sourceLoader) {
    ModulePath modulePath = getModulePath();
    ModuleProtos.Module moduleProto;
    try (InputStream inputStream = getInputStream()) {
      if (inputStream == null) {
        return false;
      }
      moduleProto = ModuleProtos.Module.parseFrom(inputStream);
    } catch (IOException e) {
      sourceLoader.getErrorReporter().report(new ExceptionError(e, modulePath));
      return false;
    }

    try {
      ModuleDeserialization moduleDeserialization = new ModuleDeserialization(sourceLoader.getLibrary().getTypecheckerState());
      Group group = moduleDeserialization.readGroup(moduleProto.getGroup());
      sourceLoader.getLibrary().onModuleLoaded(modulePath, group);
      return moduleDeserialization.readModule(moduleProto, sourceLoader.getModuleScopeProvider(), module -> !sourceLoader.getLibrary().containsModule(module) || sourceLoader.load(module), sourceLoader.getErrorReporter());
    } catch (DeserializationException e) {
      sourceLoader.getErrorReporter().report(new ExceptionError(e, modulePath));
      return false;
    }
  }

  @Override
  public boolean persist(PersistableSourceLibrary library, DefinitionLocator definitionLocator, ErrorReporter errorReporter) {
    try (OutputStream outputStream = getOutputStream()) {
      if (outputStream == null) {
        return false;
      }

      ModulePath currentModulePath = getModulePath();
      Group group = library.getModuleGroup(currentModulePath);
      if (group == null) {
        errorReporter.report(LocationError.module(currentModulePath));
        return false;
      }

      ModuleProtos.Module module = new ModuleSerialization(definitionLocator, library.getTypecheckerState(), errorReporter).writeModule(group);
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
