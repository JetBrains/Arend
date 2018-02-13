package com.jetbrains.jetpad.vclang.module.caching.serialization;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.library.PersistableSourceLibrary;
import com.jetbrains.jetpad.vclang.library.resolver.DefinitionLocator;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.source.error.LocationError;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.util.LongName;

import java.util.ArrayList;
import java.util.List;

public class ModuleSerialization {
  private final ModulePath myCurrentModule;
  private final DefinitionLocator myLocator;
  private final PersistableSourceLibrary myLibrary;
  private final TypecheckerState myState;
  private final ErrorReporter myErrorReporter;

  public ModuleSerialization(ModulePath currentModule, DefinitionLocator definitionLocator, PersistableSourceLibrary library, ErrorReporter errorReporter) {
    myCurrentModule = currentModule;
    myLocator = definitionLocator;
    myLibrary = library;
    myState = library.getTypecheckerState();
    myErrorReporter = errorReporter;
  }

  public ModuleProtos.Module writeModule(Group group) {
    ModuleProtos.Module.Builder out = ModuleProtos.Module.newBuilder();
    SimpleCallTargetIndexProvider callTargetsIndexProvider = new SimpleCallTargetIndexProvider();
    // Serialize the module first in order to populate the call-target registry
    DefinitionSerialization defStateSerialization = new DefinitionSerialization(callTargetsIndexProvider);
    ModuleProtos.Module.DefinitionState.Builder builder = ModuleProtos.Module.DefinitionState.newBuilder();
    writeDefinitionState(defStateSerialization, builder, group);
    out.setDefinitionState(builder.build());
    // now write the call-target registry
    List<ModuleProtos.Module.DefinitionReference> defRefs = new ArrayList<>();
    for (Definition callTarget : callTargetsIndexProvider.getCallTargets()) {
      ModuleProtos.Module.DefinitionReference.Builder entry = ModuleProtos.Module.DefinitionReference.newBuilder();
      GlobalReferable targetReferable = callTarget.getReferable();
      PersistableSourceLibrary library = myLocator.resolve(targetReferable);
      if (library == null) {
        myErrorReporter.report(LocationError.definition(targetReferable));
        return null;
      }

      ModulePath targetModulePath = library.getDefinitionModule(targetReferable);
      LongName targetName = library.getDefinitionFullName(targetReferable);
      if (targetModulePath == null || targetName == null) {
        myErrorReporter.report(LocationError.definition(targetReferable));
        return null;
      }
      if (!myCurrentModule.equals(targetModulePath)) {
        entry.setSourceUrl(targetModulePath.toString());
      }
      entry.setDefinitionId(DefinitionSerialization.getNameIdFor(callTarget.getReferable(), targetName.toString()));
      defRefs.add(entry.build());
    }
    out.addAllReferredDefinition(defRefs);
    return out.build();
  }

  private void writeDefinitionState(DefinitionSerialization defStateSerialization, ModuleProtos.Module.DefinitionState.Builder builder, Group group) {
    GlobalReferable referable = group.getReferable();
    Definition typechecked = myState.getTypechecked(referable);
    if (typechecked.status().headerIsOK()) {
      LongName name = myLibrary.getDefinitionFullName(referable);
      if (name == null) {
        myErrorReporter.report(LocationError.definition(referable));
      } else {
        builder.putDefinition(DefinitionSerialization.getNameIdFor(referable, name.toString()), defStateSerialization.writeDefinition(typechecked));
      }
    }

    for (Group subgroup : group.getSubgroups()) {
      writeDefinitionState(defStateSerialization, builder, subgroup);
    }
    for (Group subgroup : group.getDynamicSubgroups()) {
      writeDefinitionState(defStateSerialization, builder, subgroup);
    }
  }
}
