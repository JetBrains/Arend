package com.jetbrains.jetpad.vclang.serialization;

import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class DefNamesIndicies {
  private static class Entry {
    ResolvedName defName;
    boolean isNew;

    public Entry(ResolvedName defName, boolean isNew) {
      this.defName = defName;
      this.isNew = isNew;
    }
  }

  final private Map<ResolvedName, Integer> myDefinitions = new HashMap<>();
  final private List<Entry> myDefinitionsList = new ArrayList<>();
  private int myCounter = 0;

  public int getDefNameIndex(ResolvedName defName, boolean isNew) {
    if (defName == null) {
      return -1;
    }

    Integer index = myDefinitions.get(defName);
    if (index == null) {
      myDefinitionsList.add(new Entry(defName, isNew));
      myDefinitions.put(defName, myCounter++);
      return myCounter - 1;
    } else {
      if (isNew)
        myDefinitionsList.get(index).isNew = true;
      return index;
    }
  }

  public void serialize(DataOutputStream stream, ResolvedName module) throws IOException {
    stream.writeInt(myDefinitionsList.size());
    for (Entry entry : myDefinitionsList) {
      stream.writeBoolean(entry.isNew);
      if (entry.isNew) {
        ModuleSerialization.serializeRelativeResolvedName(stream, entry.defName, module);
        if (entry.defName.name.name.equals("\\parent")) {
          ModuleSerialization.writeDefinition(stream, ((ClassDefinition) entry.defName.parent.getResolvedName().toDefinition()).getField("\\parent"));
        } else {
          ModuleSerialization.writeDefinition(stream, entry.defName.toDefinition());
        }
      } else {
        ModuleSerialization.serializeResolvedName(stream, entry.defName);
      }
    }
  }

  public void serializeHeader(DataOutputStream stream, ResolvedName module) throws IOException {
    Set<String> provided = new HashSet<>();
    Set<ResolvedName> dependencies = new HashSet<>();

    for (Entry entry : myDefinitionsList) {
      if (entry.isNew) {
        for (ResolvedName name = entry.defName; !name.equals(module); name = name.parent.getResolvedName()) {
          if (name.parent.getResolvedName().equals(module)) {
            provided.add(name.name.name);
          }
        }
      } else {
        boolean prelude = false;
        for (ResolvedName rn = entry.defName; rn.parent != null; rn = rn.parent.getResolvedName()) {
          if (rn.equals(Prelude.PRELUDE.getResolvedName())) {
            prelude = true;
            break;
          }
        }
        if (!prelude)
          dependencies.add(entry.defName);
      }
    }

    stream.writeInt(provided.size());
    for (String str : provided) {
      stream.writeUTF(str);
    }
    stream.writeInt(dependencies.size());
    for (ResolvedName dependency : dependencies) {
      ModuleSerialization.serializeResolvedName(stream, dependency);
    }
  }
}
