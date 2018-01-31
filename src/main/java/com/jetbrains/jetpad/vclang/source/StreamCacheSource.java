package com.jetbrains.jetpad.vclang.source;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.library.SourceLoader;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.caching.serialization.DefinitionStateDeserialization;
import com.jetbrains.jetpad.vclang.module.caching.serialization.DefinitionStateSerialization;
import com.jetbrains.jetpad.vclang.module.caching.serialization.DeserializationError;
import com.jetbrains.jetpad.vclang.module.caching.serialization.ModuleProtos;
import com.jetbrains.jetpad.vclang.module.error.ExceptionError;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Represents a source that loads cache from an {@link InputStream}.
 */
public abstract class StreamCacheSource implements Source {
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

    sourceLoader.getLibrary().registerModule(modulePath);

    boolean ok;
    try {
      DefinitionStateDeserialization<SourceIdT> defStateDeserialization = new DefinitionStateDeserialization<>(sourceId, myPersistenceProvider);
      defStateDeserialization.readStubs(moduleProto.getDefinitionState(), localState);

      myStubsLoaded.add(sourceId);
      ReadCalltargets calltargets = new ReadCalltargets(sourceId, moduleProto.getReferredDefinitionList());
      defStateDeserialization.fillInDefinitions(moduleProto.getDefinitionState(), localState, calltargets);
      ok = true;
    } catch (DeserializationError e) {
      sourceLoader.getErrorReporter().report(new ExceptionError(e, modulePath));
      ok = false;
    }

    if (ok) {
      return true;
    } else {
      sourceLoader.getLibrary().unregisterModule(modulePath);
      return false;
    }
  }

  @Override
  public boolean persist(ErrorReporter errorReporter) {
    try (OutputStream outputStream = getOutputStream(errorReporter)) {
      if (outputStream == null) {
        return false;
      }

      ModuleProtos.Module.Builder out = ModuleProtos.Module.newBuilder();
      final WriteCalltargets calltargets = new WriteCalltargets(sourceId);
      // Serialize the module first in order to populate the call-target registry
      DefinitionStateSerialization defStateSerialization = new DefinitionStateSerialization(myPersistenceProvider, calltargets);
      out.setDefinitionState(defStateSerialization.writeDefinitionState(localState));
      // now write the call-target registry
      out.addAllReferredDefinition(calltargets.write());

      out.setVersion(myVersionTracker.getCurrentVersion(sourceId));
      out.build().writeTo(outputStream);
      return true;
    } catch (IOException e) {
      errorReporter.report(new ExceptionError(e, getModulePath()));
      return false;
    }
  }
}
