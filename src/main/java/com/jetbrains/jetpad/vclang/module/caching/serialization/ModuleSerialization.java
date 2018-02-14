package com.jetbrains.jetpad.vclang.module.caching.serialization;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.library.PersistableSourceLibrary;
import com.jetbrains.jetpad.vclang.library.resolver.DefinitionLocator;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.source.error.LocationError;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.term.Precedence;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.util.LongName;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ModuleSerialization {
  private final DefinitionLocator myLocator;
  private final TypecheckerState myState;
  private final ErrorReporter myErrorReporter;
  private final SimpleCallTargetIndexProvider myCallTargetsIndexProvider = new SimpleCallTargetIndexProvider();
  private final DefinitionSerialization myDefinitionSerialization = new DefinitionSerialization(myCallTargetsIndexProvider);
  private final Set<Integer> myCurrentDefinitions = new HashSet<>();

  public ModuleSerialization(DefinitionLocator definitionLocator, TypecheckerState state, ErrorReporter errorReporter) {
    myLocator = definitionLocator;
    myState = state;
    myErrorReporter = errorReporter;
  }

  public ModuleProtos.Module writeModule(Group group) {
    ModuleProtos.Module.Builder out = ModuleProtos.Module.newBuilder();

    // Serialize the group structure first in order to populate the call target tree
    out.setGroup(writeGroup(group));

    // Now write the call target tree
    Map<ModulePath, Map<String, CallTargetTree>> moduleCallTargets = new HashMap<>();
    for (Map.Entry<Definition, Integer> entry : myCallTargetsIndexProvider.getCallTargets()) {
      if (myCurrentDefinitions.contains(entry.getValue())) {
        continue;
      }

      GlobalReferable targetReferable = entry.getKey().getReferable();
      PersistableSourceLibrary library = myLocator.locate(targetReferable);
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

      Map<String, CallTargetTree> map = moduleCallTargets.computeIfAbsent(targetModulePath, k -> new HashMap<>());
      CallTargetTree tree = null;
      for (String name : targetName.toList()) {
        tree = map.computeIfAbsent(name, k -> new CallTargetTree(0));
        map = tree.subtreeMap;
      }
      if (tree == null) {
        throw new IllegalStateException();
      }
      tree.index = entry.getValue();
    }

    for (Map.Entry<ModulePath, Map<String, CallTargetTree>> entry : moduleCallTargets.entrySet()) {
      ModuleProtos.ModuleCallTargets.Builder builder = ModuleProtos.ModuleCallTargets.newBuilder();
      builder.addAllName(entry.getKey().toList());
      for (Map.Entry<String, CallTargetTree> treeEntry : entry.getValue().entrySet()) {
        builder.putCallTargetTree(treeEntry.getKey(), writeCallTargetTree(treeEntry.getValue()));
      }
      out.addModuleCallTargets(builder.build());
    }

    return out.build();
  }

  private ModuleProtos.Group writeGroup(Group group) {
    ModuleProtos.Group.Builder builder = ModuleProtos.Group.newBuilder();

    GlobalReferable referable = group.getReferable();
    builder.setReferable(writeReferable(referable));

    for (Group subgroup : group.getSubgroups()) {
      builder.addSubgroup(writeGroup(subgroup));
    }
    for (Group subgroup : group.getDynamicSubgroups()) {
      builder.addDynamicSubgroup(writeGroup(subgroup));
    }
    for (Group.InternalReferable internalReferable : group.getConstructors()) {
      builder.addInternalReferable(writeInternalReferable(internalReferable));
    }
    for (Group.InternalReferable internalReferable : group.getFields()) {
      builder.addInternalReferable(writeInternalReferable(internalReferable));
    }

    return builder.build();
  }

  private ModuleProtos.Referable writeReferable(GlobalReferable referable) {
    ModuleProtos.Referable.Builder builder = ModuleProtos.Referable.newBuilder();
    builder.setName(referable.textRepresentation());
    builder.setPrecedence(writePrecedence(referable.getPrecedence()));

    Definition typechecked = myState.getTypechecked(referable);
    if (typechecked != null && typechecked.status().headerIsOK()) {
      builder.setDefinition(myDefinitionSerialization.writeDefinition(typechecked));
      int index = myCallTargetsIndexProvider.getDefIndex(typechecked);
      builder.setIndex(index);
      myCurrentDefinitions.add(index);
    }

    return builder.build();
  }

  private ModuleProtos.InternalReferable writeInternalReferable(Group.InternalReferable internalReferable) {
    ModuleProtos.InternalReferable.Builder builder = ModuleProtos.InternalReferable.newBuilder();
    builder.setReferable(writeReferable(internalReferable.getReferable()));
    builder.setVisible(internalReferable.isVisible());
    return builder.build();
  }

  private ModuleProtos.Precedence writePrecedence(Precedence precedence) {
    ModuleProtos.Precedence.Builder builder = ModuleProtos.Precedence.newBuilder();
    switch (precedence.associativity) {
      case LEFT_ASSOC:
        builder.setAssoc(ModuleProtos.Precedence.Assoc.LEFT);
        break;
      case RIGHT_ASSOC:
        builder.setAssoc(ModuleProtos.Precedence.Assoc.RIGHT);
        break;
      case NON_ASSOC:
        builder.setAssoc(ModuleProtos.Precedence.Assoc.NON_ASSOC);
        break;
    }
    builder.setPriority(precedence.priority);
    builder.setInfix(precedence.isInfix);
    return builder.build();
  }

  private class CallTargetTree {
    Map<String, CallTargetTree> subtreeMap = new HashMap<>();
    int index;

    CallTargetTree(int index) {
      this.index = index;
    }
  }

  private ModuleProtos.CallTargetTree writeCallTargetTree(CallTargetTree tree) {
    ModuleProtos.CallTargetTree.Builder builder = ModuleProtos.CallTargetTree.newBuilder();
    builder.setIndex(tree.index);
    for (Map.Entry<String, CallTargetTree> entry : tree.subtreeMap.entrySet()) {
      builder.putSubtree(entry.getKey(), writeCallTargetTree(entry.getValue()));
    }
    return builder.build();
  }
}
