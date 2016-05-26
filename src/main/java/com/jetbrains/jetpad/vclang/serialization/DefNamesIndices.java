package com.jetbrains.jetpad.vclang.serialization;

import com.jetbrains.jetpad.vclang.module.ModuleID;
import com.jetbrains.jetpad.vclang.module.SerializableModuleID;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.Preprelude;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.naming.ResolvedName;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class DefNamesIndices {
  final private Map<ResolvedName, Integer> myDefinitions = new HashMap<>();
  final private List<ResolvedName> myDefinitionsList = new ArrayList<>();
  private int myCounter = 0;

  public int getDefNameIndex(ResolvedName defName) {
    if (defName == null) {
      return -1;
    }

    Integer index = myDefinitions.get(defName);
    if (index == null) {
      myDefinitionsList.add(defName);
      myDefinitions.put(defName, myCounter++);
      return myCounter - 1;
    } else {
      return index;
    }
  }

  public void serialize(DataOutputStream stream, SerializableModuleID curModuleID) throws IOException {
    stream.writeInt(myDefinitionsList.size());
    for (ResolvedName rn : myDefinitionsList) {
      ModuleID moduleID = rn.getModuleID();
      stream.writeBoolean(moduleID.equals(curModuleID));
      if (moduleID.equals(curModuleID)) {
        ModuleSerialization.serializeResolvedName(stream, rn);
        if (rn.getName().equals("\\parent")) {
          ModuleSerialization.writeDefinition(stream, ((ClassDefinition) rn.getParent().toDefinition()).getField("\\parent"));
        } else {
          ModuleSerialization.writeDefinition(stream, rn.toDefinition());
        }
      } else {
        stream.writeInt(moduleID == Prelude.moduleID ? 0 : moduleID == Preprelude.moduleID ? 1 : 2);
        if (moduleID != Prelude.moduleID && moduleID != Preprelude.moduleID) {
          ((SerializableModuleID) moduleID).serialize(stream);
        }
        ModuleSerialization.serializeResolvedName(stream, rn);
      }
    }
  }

  public void serializeHeader(DataOutputStream stream, ModuleID curModuleID) throws IOException {
    Set<SerializableModuleID> dependencies = new HashSet<>();

    for (ResolvedName resolvedName : myDefinitionsList) {
      ModuleID moduleID = resolvedName.getModuleID();
      if (!moduleID.equals(Prelude.moduleID) && !moduleID.equals(Preprelude.moduleID) && !curModuleID.equals(moduleID)) {
        dependencies.add((SerializableModuleID) moduleID);
      }
    }

    stream.writeInt(dependencies.size());
    for (SerializableModuleID dependency : dependencies) {
      dependency.serialize(stream);
    }
  }
}
