package com.jetbrains.jetpad.vclang.serialization;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefNamesIndicies {
  private static class Entry {
    ResolvedName defName;
    int index;
    boolean isNew;

    public Entry(ResolvedName defName, int index, boolean isNew) {
      this.defName = defName;
      this.index = index;
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
      myDefinitionsList.add(new Entry(defName, myCounter, isNew));
      myDefinitions.put(defName, myCounter++);
      return myCounter - 1;
    } else {
      if (isNew)
        myDefinitionsList.get(index).isNew = true;
      return index;
    }
  }

  public void serialize(DataOutputStream stream) throws IOException {
    stream.writeInt(myDefinitionsList.size());
    for (Entry entry : myDefinitionsList) {
      stream.writeInt(entry.index);
      ModuleSerialization.serializeResolvedName(entry.defName, stream);
      stream.writeBoolean(entry.isNew);
      if (entry.isNew) {
        Definition definition = entry.defName.toDefinition();

        stream.writeBoolean(definition.getName().fixity == Abstract.Definition.Fixity.PREFIX);
        stream.writeUTF(definition.getName().name);
        stream.write(ModuleSerialization.getDefinitionCode(definition));
      }
    }
  }
}
