package com.jetbrains.jetpad.vclang.serialization;

import com.jetbrains.jetpad.vclang.module.source.ModuleSourceId;
import com.jetbrains.jetpad.vclang.module.source.SerializableModuleSourceId;
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

  public void serialize(DataOutputStream stream, SerializableModuleSourceId curModuleID) throws IOException {
    // FIXME[serial]
    /*
    stream.writeInt(myDefinitionsList.size());
    for (ResolvedName rn : myDefinitionsList) {
      ModuleID moduleID = rn.getModuleID();
      stream.writeBoolean(moduleID.equals(curModuleID));
      if (moduleID.equals(curModuleID)) {
        ModuleSerialization.serializeResolvedName(stream, rn);
        if (rn.getName().equals("\\parent")) {
          ModuleSerialization.writeDefinition(stream, ((ClassDefinition) rn.getParent().toDefinition()).getEnclosingThisField());
        } else {
          ModuleSerialization.writeDefinition(stream, rn.toDefinition());
        }
      } else {
        stream.writeInt(moduleID == moduleID ? 0 : 1);
        if (moduleID != moduleID) {
          ((SerializableModuleSourceId) moduleID).serialize(stream);
        }
        ModuleSerialization.serializeResolvedName(stream, rn);
      }
    }
    */
  }

  public void serializeHeader(DataOutputStream stream, ModuleSourceId curModuleID) throws IOException {
    Set<SerializableModuleSourceId> dependencies = new HashSet<>();

    for (ResolvedName resolvedName : myDefinitionsList) {
      // FIXME[serial]
      /*
      ModuleID moduleID = resolvedName.getModuleID();
      if (!moduleID.equals(moduleID) && !curModuleID.equals(moduleID)) {
        dependencies.add((SerializableModuleSourceId) moduleID);
      }
      */
    }

    stream.writeInt(dependencies.size());
    for (SerializableModuleSourceId dependency : dependencies) {
      dependency.serialize(stream);
    }
  }
}
