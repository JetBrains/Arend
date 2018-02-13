package com.jetbrains.jetpad.vclang.source;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.library.PersistableSourceLibrary;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.caching.serialization.DefinitionDeserialization;
import com.jetbrains.jetpad.vclang.module.caching.serialization.DeserializationError;
import com.jetbrains.jetpad.vclang.module.caching.serialization.ModuleProtos;
import com.jetbrains.jetpad.vclang.module.caching.serialization.ModuleSerialization;
import com.jetbrains.jetpad.vclang.module.error.ExceptionError;
import com.jetbrains.jetpad.vclang.source.error.LocationError;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.term.Precedence;
import com.jetbrains.jetpad.vclang.util.Pair;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

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

    sourceLoader.getLibrary().registerModule(modulePath);

    boolean ok;
    try {
      DefinitionDeserialization<SourceIdT> defStateDeserialization = new DefinitionDeserialization<>(sourceId, myPersistenceProvider);
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

  private static Pair<Precedence, List<String>> fullNameFromNameId(String s) {
    boolean isInfix = s.charAt(0) == 'i';
    final Precedence.Associativity assoc;
    switch (s.charAt(1)) {
      case 'l':
        assoc = Precedence.Associativity.LEFT_ASSOC;
        break;
      case 'r':
        assoc = Precedence.Associativity.RIGHT_ASSOC;
        break;
      default:
        assoc = Precedence.Associativity.NON_ASSOC;
    }

    int sepIndex = s.indexOf(';');
    final byte priority = Byte.parseByte(s.substring(2, sepIndex));
    return new Pair<>(new Precedence(assoc, priority, isInfix), Arrays.asList(s.substring(sepIndex + 1).split("\\.")));
  }

  @Override
  public boolean persist(PersistableSourceLibrary library, ErrorReporter errorReporter) {
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

      ModuleProtos.Module module = new ModuleSerialization(currentModulePath, library, errorReporter).writeModule(group);
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
