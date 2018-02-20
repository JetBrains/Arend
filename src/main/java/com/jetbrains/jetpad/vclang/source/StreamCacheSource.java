package com.jetbrains.jetpad.vclang.source;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.library.PersistableSourceLibrary;
import com.jetbrains.jetpad.vclang.library.resolver.DefinitionLocator;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.caching.serialization.DeserializationException;
import com.jetbrains.jetpad.vclang.module.caching.serialization.ModuleDeserialization;
import com.jetbrains.jetpad.vclang.module.caching.serialization.ModuleProtos;
import com.jetbrains.jetpad.vclang.module.caching.serialization.ModuleSerialization;
import com.jetbrains.jetpad.vclang.module.error.ExceptionError;
import com.jetbrains.jetpad.vclang.source.error.LocationError;
import com.jetbrains.jetpad.vclang.term.group.Group;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Represents a source that loads cache from an {@link InputStream} and persists it to an {@link OutputStream}.
 */
public abstract class StreamCacheSource implements PersistableSource {
  /**
   * Gets an input stream from which the source will be loaded.
   *
   * @param errorReporter a reporter for I/O exceptions and other errors.
   *
   * @return an input stream from which the source will be loaded or null if some error occurred.
   */
  @Nullable
  protected abstract InputStream getInputStream(ErrorReporter errorReporter);

  /**
   * Gets an output stream to which the source will be persisted.
   *
   * @param errorReporter a reporter for I/O exceptions and other errors.
   *
   * @return an input stream from which the source will be loaded or null if some error occurred.
   */
  @Nullable
  protected abstract OutputStream getOutputStream(ErrorReporter errorReporter);

  @Override
  public boolean load(SourceLoader sourceLoader) {
    ModulePath modulePath = getModulePath();
    ModuleProtos.Module moduleProto;
    try (InputStream inputStream = getInputStream(sourceLoader.getErrorReporter())) {
      if (inputStream == null) {
        return false;
      }
      moduleProto = ModuleProtos.Module.parseFrom(inputStream);
    } catch (IOException e) {
      sourceLoader.getErrorReporter().report(new ExceptionError(e, modulePath));
      return false;
    }

    try {
      ModuleDeserialization moduleDeserialization = new ModuleDeserialization(sourceLoader.getModuleScopeProvider(), sourceLoader.getLibrary().getTypecheckerState(), sourceLoader.getErrorReporter());
      Group group = moduleDeserialization.readModule(moduleProto, module -> !sourceLoader.getLibrary().containsModule(module) || sourceLoader.load(module));
      if (group != null) {
        sourceLoader.getLibrary().onModuleLoaded(modulePath, group);
        return true;
      } else {
        return false;
      }
    } catch (DeserializationException e) {
      sourceLoader.getErrorReporter().report(new ExceptionError(e, modulePath));
      return false;
    }
  }

  @Override
  public boolean persist(PersistableSourceLibrary library, DefinitionLocator definitionLocator, ErrorReporter errorReporter) {
    try (OutputStream outputStream = getOutputStream(errorReporter)) {
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
