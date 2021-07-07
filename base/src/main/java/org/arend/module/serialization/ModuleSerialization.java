package org.arend.module.serialization;

import org.arend.core.definition.ClassField;
import org.arend.core.definition.Constructor;
import org.arend.core.definition.Definition;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.module.ModulePath;
import org.arend.module.ModuleLocation;
import org.arend.naming.reference.*;
import org.arend.naming.reference.converter.ReferableConverter;
import org.arend.source.error.LocationError;
import org.arend.term.group.Group;
import org.arend.typechecking.order.dependency.DependencyListener;

import java.util.*;

public class ModuleSerialization {
  private final ErrorReporter myErrorReporter;
  private final SimpleCallTargetIndexProvider myCallTargetIndexProvider = new SimpleCallTargetIndexProvider();
  private final DefinitionSerialization myDefinitionSerialization;
  private final Set<Integer> myCurrentDefinitions = new HashSet<>();
  private boolean myComplete;

  static final int VERSION = 8;

  public ModuleSerialization(ErrorReporter errorReporter, DependencyListener dependencyListener) {
    myErrorReporter = errorReporter;
    myDefinitionSerialization = new DefinitionSerialization(myCallTargetIndexProvider, dependencyListener);
  }

  public ModuleProtos.Module writeModule(Group group, ModulePath modulePath, ReferableConverter referableConverter) {
    ModuleProtos.Module.Builder out = ModuleProtos.Module.newBuilder();

    // Serialize the group structure first in order to populate the call target tree
    myComplete = true;
    out.setVersion(VERSION);
    out.setGroup(writeGroup(group, referableConverter));
    out.setComplete(myComplete);

    // Now write the call target tree
    Map<ModulePath, Map<String, CallTargetTree>> moduleCallTargets = new HashMap<>();
    for (Map.Entry<Object, Integer> entry : myCallTargetIndexProvider.getCallTargets()) {
      if (!(entry.getKey() instanceof TCReferable) || myCurrentDefinitions.contains(entry.getValue())) {
        continue;
      }

      TCReferable targetReferable = (TCReferable) entry.getKey();
      List<String> longName = new ArrayList<>();
      ModuleLocation targetModuleLocation = LocatedReferable.Helper.getLocation(targetReferable, longName);
      if (targetModuleLocation == null || longName.isEmpty()) {
        myErrorReporter.report(LocationError.definition(targetReferable, modulePath));
        return null;
      }

      Map<String, CallTargetTree> map = moduleCallTargets.computeIfAbsent(targetModuleLocation.getModulePath(), k -> new HashMap<>());
      CallTargetTree tree = null;
      for (String name : longName) {
        tree = map.computeIfAbsent(name, k -> new CallTargetTree(0));
        map = tree.subtreeMap;
      }
      assert tree != null;
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

  private ModuleProtos.Group writeGroup(Group group, ReferableConverter referableConverter) {
    ModuleProtos.Group.Builder builder = ModuleProtos.Group.newBuilder();

    // Write referable
    LocatedReferable referable = group.getReferable();
    DefinitionProtos.Referable.Builder refBuilder = DefinitionProtos.Referable.newBuilder();
    refBuilder.setName(referable instanceof ModuleReferable ? ((ModuleReferable) referable).path.getLastName() : referable.textRepresentation());
    refBuilder.setPrecedence(DefinitionSerialization.writePrecedence(referable.getPrecedence()));

    TCReferable tcReferable = referableConverter.toDataLocatedReferable(referable);
    Definition typechecked = tcReferable instanceof TCDefReferable ? ((TCDefReferable) tcReferable).getTypechecked() : null;
    if (typechecked != null && typechecked.status() == Definition.TypeCheckingStatus.NO_ERRORS && !(typechecked instanceof Constructor || typechecked instanceof ClassField)) {
      builder.setDefinition(myDefinitionSerialization.writeDefinition(typechecked));
      int index = myCallTargetIndexProvider.getDefIndex(typechecked);
      refBuilder.setIndex(index);
      myCurrentDefinitions.add(index);
    }
    if (tcReferable != null && (typechecked == null || typechecked.status() != Definition.TypeCheckingStatus.NO_ERRORS) && tcReferable.getKind() != GlobalReferable.Kind.OTHER) {
      myComplete = false;
    }
    builder.setReferable(refBuilder.build());

    // Write subgroups
    for (Group subgroup : group.getSubgroups()) {
      builder.addSubgroup(writeGroup(subgroup, referableConverter));
    }
    for (Group subgroup : group.getDynamicSubgroups()) {
      builder.addDynamicSubgroup(writeGroup(subgroup, referableConverter));
    }
    for (Group.InternalReferable internalReferable : group.getInternalReferables()) {
      if (!internalReferable.isVisible()) {
        TCReferable ref = referableConverter.toDataLocatedReferable(internalReferable.getReferable());
        Definition def = ref instanceof TCDefReferable ? ((TCDefReferable) ref).getTypechecked() : null;
        if (def != null) {
          builder.addInvisibleInternalReferable(myCallTargetIndexProvider.getDefIndex(def));
        }
      }
    }

    return builder.build();
  }

  private static class CallTargetTree {
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
