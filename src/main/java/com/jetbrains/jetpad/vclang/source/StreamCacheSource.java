package com.jetbrains.jetpad.vclang.source;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.library.PersistableSourceLibrary;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.caching.serialization.*;
import com.jetbrains.jetpad.vclang.module.error.ExceptionError;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Precedence;
import com.jetbrains.jetpad.vclang.util.LongName;
import com.jetbrains.jetpad.vclang.util.Pair;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
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
  public boolean persist(PersistableSourceLibrary library, ErrorReporter errorReporter) {
    try (OutputStream outputStream = getOutputStream(errorReporter)) {
      if (outputStream == null) {
        return false;
      }

      ModulePath currentModulePath = getModulePath();

      ModuleProtos.Module.Builder out = ModuleProtos.Module.newBuilder();
      SimpleCallTargetIndexProvider callTargetsIndexProvider = new SimpleCallTargetIndexProvider();
      // Serialize the module first in order to populate the call-target registry
      DefinitionStateSerialization defStateSerialization = new DefinitionStateSerialization(myPersistenceProvider, callTargetsIndexProvider);
      out.setDefinitionState(defStateSerialization.writeDefinitionState(localState));
      // now write the call-target registry
      List<ModuleProtos.Module.DefinitionReference> defRefs = new ArrayList<>();
      for (Definition callTarget : callTargetsIndexProvider.getCallTargets()) {
        ModuleProtos.Module.DefinitionReference.Builder entry = ModuleProtos.Module.DefinitionReference.newBuilder();
        ModulePath targetModulePath = library.getDefinitionModule(callTarget.getReferable());
        LongName targetName = library.getDefinitionFullName(callTarget.getReferable());
        if (!currentModulePath.equals(targetModulePath)) {
          entry.setSourceUrl(targetModulePath.toString());
        }
        entry.setDefinitionId(getNameIdFor(callTarget.getReferable(), targetName));
        defRefs.add(entry.build());
      }
      out.addAllReferredDefinition(defRefs);

      out.build().writeTo(outputStream);
      return true;
    } catch (IOException e) {
      errorReporter.report(new ExceptionError(e, getModulePath()));
      return false;
    }
  }

  private static String getNameIdFor(GlobalReferable referable, LongName name) {
    Precedence precedence = referable.getPrecedence();
    char fixityChar = precedence.isInfix ? 'i' : 'n';
    final char assocChr;
    switch (precedence.associativity) {
      case LEFT_ASSOC:
        assocChr = 'l';
        break;
      case RIGHT_ASSOC:
        assocChr = 'r';
        break;
      default:
        assocChr = 'n';
    }
    return "" + fixityChar + assocChr + precedence.priority + ';' + name;
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
}
