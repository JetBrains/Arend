package org.arend.source;

import com.google.protobuf.CodedInputStream;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.module.ModulePath;
import org.arend.ext.typechecking.DefinitionListener;
import org.arend.extImpl.SerializableKeyRegistryImpl;
import org.arend.library.LibraryManager;
import org.arend.library.SourceLibrary;
import org.arend.library.error.LibraryError;
import org.arend.library.error.PartialModuleError;
import org.arend.module.ModuleLocation;
import org.arend.module.error.DeserializationError;
import org.arend.module.error.ExceptionError;
import org.arend.ext.serialization.DeserializationException;
import org.arend.module.scopeprovider.ModuleScopeProvider;
import org.arend.module.serialization.ModuleDeserialization;
import org.arend.module.serialization.ModuleProtos;
import org.arend.module.serialization.ModuleSerialization;
import org.arend.naming.reference.converter.ReferableConverter;
import org.arend.prelude.PreludeLibrary;
import org.arend.source.error.LocationError;
import org.arend.source.error.PersistingError;
import org.arend.term.group.ChildGroup;
import org.arend.term.group.Group;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a source that loads a binary module from an {@link InputStream} and persists it to an {@link OutputStream}.
 */
public abstract class StreamBinarySource implements PersistableBinarySource {
  private ModuleDeserialization myModuleDeserialization;
  private SerializableKeyRegistryImpl myKeyRegistry;
  private DefinitionListener myDefinitionListener;
  private int myPass = 0;
  private final List<ModulePath> myDependencies = new ArrayList<>();

  @Override
  public void setKeyRegistry(SerializableKeyRegistryImpl keyRegistry) {
    myKeyRegistry = keyRegistry;
  }

  @Override
  public void setDefinitionListener(DefinitionListener definitionListener) {
    myDefinitionListener = definitionListener;
  }

  @NotNull
  @Override
  public abstract ModulePath getModulePath();

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
  public @NotNull List<? extends ModulePath> getDependencies() {
    return myDependencies;
  }

  public static Group getGroup(InputStream inputStream, LibraryManager libraryManager, SourceLibrary library) throws IOException, DeserializationException {
    CodedInputStream codedInputStream = CodedInputStream.newInstance(inputStream);
    codedInputStream.setRecursionLimit(Integer.MAX_VALUE);
    ModuleProtos.Module moduleProto = ModuleProtos.Module.parseFrom(codedInputStream);

    ModuleDeserialization moduleDeserialization = new ModuleDeserialization(moduleProto, library.getReferableConverter(), null, libraryManager.getDefinitionListener(), false);

    ChildGroup group = moduleDeserialization.readGroup(new ModuleLocation(library, ModuleLocation.LocationKind.GENERATED, new ModulePath()));

    ModuleScopeProvider moduleScopeProvider = libraryManager.getAvailableModuleScopeProvider(library);
    moduleDeserialization.readModule(moduleScopeProvider, library.getDependencyListener());

    return group;
  }

  @Override
  public @NotNull LoadResult load(SourceLoader sourceLoader) {
    SourceLibrary library = sourceLoader.getLibrary();
    ModulePath modulePath = getModulePath();

    if (myPass == 0) {
      try (InputStream inputStream = getInputStream()) {
        if (inputStream == null) {
          sourceLoader.getLibraryErrorReporter().report(LibraryError.moduleLoading(modulePath, library.getName()));
          return LoadResult.FAIL;
        }

        CodedInputStream codedInputStream = CodedInputStream.newInstance(inputStream);
        codedInputStream.setRecursionLimit(Integer.MAX_VALUE);
        ModuleProtos.Module moduleProto = ModuleProtos.Module.parseFrom(codedInputStream);

        for (ModuleProtos.ModuleCallTargets moduleCallTargets : moduleProto.getModuleCallTargetsList()) {
          myDependencies.add(new ModulePath(moduleCallTargets.getNameList()));
        }

        boolean isComplete = moduleProto.getComplete();
        if (!isComplete && !library.hasRawSources()) {
          sourceLoader.getLibraryErrorReporter().report(new PartialModuleError(modulePath));
          return LoadResult.FAIL;
        }

        ReferableConverter referableConverter = sourceLoader.getReferableConverter();
        myModuleDeserialization = new ModuleDeserialization(moduleProto, referableConverter, myKeyRegistry, myDefinitionListener, library instanceof PreludeLibrary);

        if (referableConverter == null) {
          ChildGroup group = myModuleDeserialization.readGroup(new ModuleLocation(library, ModuleLocation.LocationKind.SOURCE, modulePath));
          library.groupLoaded(modulePath, group, false, false);
        } else {
          ChildGroup group = library.getModuleGroup(modulePath, false);
          if (group == null) {
            sourceLoader.getLibraryErrorReporter().report(LibraryError.moduleNotFound(modulePath, library.getName()));
            library.groupLoaded(modulePath, null, false, false);
            return LoadResult.FAIL;
          }
          myModuleDeserialization.readDefinitions(group);
        }

        myPass = 1;
        return LoadResult.CONTINUE;
      } catch (IOException | DeserializationException e) {
        loadingFailed(sourceLoader, modulePath, e);
        return LoadResult.FAIL;
      }
    }

    try {
      myModuleDeserialization.readModule(sourceLoader.getModuleScopeProvider(false), library.getDependencyListener());
      library.binaryLoaded(modulePath, myModuleDeserialization.getModuleProto().getComplete());
      myModuleDeserialization = null;
      return LoadResult.SUCCESS;
    } catch (DeserializationException e) {
      loadingFailed(sourceLoader, modulePath, e);
      return LoadResult.FAIL;
    }
  }

  private void loadingFailed(SourceLoader sourceLoader, ModulePath modulePath, Exception e) {
    sourceLoader.getLibraryErrorReporter().report(new DeserializationError(modulePath, e));
    if (!sourceLoader.getLibrary().hasRawSources()) {
      sourceLoader.getLibrary().groupLoaded(modulePath, null, false, false);
    }
  }

  @Override
  public boolean persist(SourceLibrary library, ReferableConverter referableConverter, ErrorReporter errorReporter) {
    ModulePath currentModulePath = getModulePath();
    Group group = library.getModuleGroup(currentModulePath, false);
    if (group == null) {
      errorReporter.report(LocationError.module(currentModulePath));
      return false;
    }

    try (OutputStream outputStream = getOutputStream()) {
      if (outputStream == null) {
        errorReporter.report(new PersistingError(currentModulePath));
        return false;
      }

      ModuleProtos.Module module = new ModuleSerialization(errorReporter, library.getDependencyListener()).writeModule(group, currentModulePath, referableConverter);
      if (module == null) {
        return false;
      }

      module.writeTo(outputStream);
      return true;
    } catch (Exception e) {
      errorReporter.report(new ExceptionError(e, "persisting", currentModulePath));
      return false;
    }
  }
}
