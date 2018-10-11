package org.arend.source;

import org.arend.error.ErrorReporter;
import org.arend.library.SourceLibrary;
import org.arend.library.error.LibraryError;
import org.arend.library.error.PartialModuleError;
import org.arend.module.ModulePath;
import org.arend.module.error.DeserializationError;
import org.arend.module.error.ExceptionError;
import org.arend.module.serialization.DeserializationException;
import org.arend.module.serialization.ModuleDeserialization;
import org.arend.module.serialization.ModuleProtos;
import org.arend.module.serialization.ModuleSerialization;
import org.arend.naming.reference.converter.ReferableConverter;
import org.arend.source.error.LocationError;
import org.arend.source.error.PersistingError;
import org.arend.term.group.ChildGroup;
import org.arend.term.group.Group;

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
    ChildGroup group = null;
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

      ReferableConverter referableConverter = sourceLoader.getReferableConverter();
      myModuleDeserialization = new ModuleDeserialization(moduleProto, library.getTypecheckerState(), referableConverter);

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
      sourceLoader.getLibraryErrorReporter().report(new DeserializationError(getModulePath(), e));
      if (!library.hasRawSources()) {
        library.onGroupLoaded(getModulePath(), null, false);
      }
      if (group != null) {
        library.unloadGroup(group);
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
          ChildGroup group = library.getModuleGroup(module);
          if (group != null) {
            library.unloadGroup(group);
          }
          return false;
        }
      }

      myModuleDeserialization.readModule(sourceLoader.getModuleScopeProvider(), library.getDependencyListener(), library.supportsTypechecking());
      library.onBinaryLoaded(getModulePath(), myModuleDeserialization.getModuleProto().getComplete());
      myModuleDeserialization = null;
      return true;
    } catch (DeserializationException e) {
      ModulePath modulePath = getModulePath();
      sourceLoader.getLibraryErrorReporter().report(new DeserializationError(modulePath, e));
      if (!library.hasRawSources()) {
        library.onGroupLoaded(modulePath, null, false);
      }
      ChildGroup group = library.getModuleGroup(modulePath);
      if (group != null) {
        library.unloadGroup(group);
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
