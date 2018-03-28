package com.jetbrains.jetpad.vclang.module.serialization;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.LocatedReferable;
import com.jetbrains.jetpad.vclang.source.error.LocationError;
import com.jetbrains.jetpad.vclang.term.group.Group;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;

import java.util.*;

public class ModuleSerialization {
  private final TypecheckerState myState;
  private final ErrorReporter myErrorReporter;
  private final SimpleCallTargetIndexProvider myCallTargetIndexProvider = new SimpleCallTargetIndexProvider();
  private final DefinitionSerialization myDefinitionSerialization = new DefinitionSerialization(myCallTargetIndexProvider);
  private final Set<Integer> myCurrentDefinitions = new HashSet<>();

  public ModuleSerialization(TypecheckerState state, ErrorReporter errorReporter) {
    myState = state;
    myErrorReporter = errorReporter;
  }

  public ModuleProtos.Module writeModule(Group group, ModulePath modulePath) {
    ModuleProtos.Module.Builder out = ModuleProtos.Module.newBuilder();

    // Serialize the group structure first in order to populate the call target tree
    out.setGroup(writeGroup(group));

    // Now write the call target tree
    Map<ModulePath, Map<String, CallTargetTree>> moduleCallTargets = new HashMap<>();
    for (Map.Entry<Definition, Integer> entry : myCallTargetIndexProvider.getCallTargets()) {
      if (myCurrentDefinitions.contains(entry.getValue())) {
        continue;
      }

      GlobalReferable targetReferable = entry.getKey().getReferable();
      List<String> longName = new ArrayList<>();
      ModulePath targetModulePath = targetReferable instanceof LocatedReferable ? LocatedReferable.Helper.getLocation((LocatedReferable) targetReferable, longName) : null;
      if (targetModulePath == null || longName.isEmpty()) {
        myErrorReporter.report(LocationError.definition(targetReferable, modulePath));
        return null;
      }

      Map<String, CallTargetTree> map = moduleCallTargets.computeIfAbsent(targetModulePath, k -> new HashMap<>());
      CallTargetTree tree = null;
      for (String name : longName) {
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
        builder.addCallTargetTree(writeCallTargetTree(treeEntry.getKey(), treeEntry.getValue()));
      }
      out.addModuleCallTargets(builder.build());
    }

    return out.build();
  }

  private ModuleProtos.Group writeGroup(Group group) {
    ModuleProtos.Group.Builder builder = ModuleProtos.Group.newBuilder();

    // Write referable
    GlobalReferable referable = group.getReferable();
    DefinitionProtos.Referable.Builder refBuilder = DefinitionProtos.Referable.newBuilder();
    refBuilder.setName(referable.textRepresentation());
    refBuilder.setPrecedence(DefinitionSerialization.writePrecedence(referable.getPrecedence()));

    Definition typechecked = myState.getTypechecked(referable);
    if (typechecked != null && typechecked.status().headerIsOK()) {
      builder.setDefinition(myDefinitionSerialization.writeDefinition(typechecked));
      int index = myCallTargetIndexProvider.getDefIndex(typechecked);
      refBuilder.setIndex(index);
      myCurrentDefinitions.add(index);
    }
    builder.setReferable(refBuilder.build());

    // Write subgroups
    for (Group subgroup : group.getSubgroups()) {
      builder.addSubgroup(writeGroup(subgroup));
    }
    for (Group subgroup : group.getDynamicSubgroups()) {
      builder.addDynamicSubgroup(writeGroup(subgroup));
    }
    for (Group.InternalReferable internalReferable : group.getConstructors()) {
      if (!internalReferable.isVisible()) {
        builder.addInvisibleInternalReferable(myCallTargetIndexProvider.getDefIndex(myState.getTypechecked(internalReferable.getReferable())));
      }
    }
    for (Group.InternalReferable internalReferable : group.getFields()) {
      if (!internalReferable.isVisible()) {
        builder.addInvisibleInternalReferable(myCallTargetIndexProvider.getDefIndex(myState.getTypechecked(internalReferable.getReferable())));
      }
    }

    return builder.build();
  }

  private class CallTargetTree {
    Map<String, CallTargetTree> subtreeMap = new HashMap<>();
    int index;

    CallTargetTree(int index) {
      this.index = index;
    }
  }

  private ModuleProtos.CallTargetTree writeCallTargetTree(String name, CallTargetTree tree) {
    ModuleProtos.CallTargetTree.Builder builder = ModuleProtos.CallTargetTree.newBuilder();
    builder.setName(name);
    builder.setIndex(tree.index);
    for (Map.Entry<String, CallTargetTree> entry : tree.subtreeMap.entrySet()) {
      builder.addSubtree(writeCallTargetTree(entry.getKey(), entry.getValue()));
    }
    return builder.build();
  }
}
