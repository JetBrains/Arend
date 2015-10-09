package com.jetbrains.jetpad.vclang.serialization;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Definition;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefinitionsIndices {
  private static class Entry {
    // TODO: Replace Definition with ResolvedName maybe?
    Definition definition;
    int index;
    boolean isNew;

    public Entry(Definition definition, int index, boolean isNew) {
      this.definition = definition;
      this.index = index;
      this.isNew = isNew;
    }
  }

  final private Map<Definition, Integer> myDefinitions = new HashMap<>();
  final private List<Entry> myDefinitionsList = new ArrayList<>();
  private int myCounter = 1;

  public int getDefinitionIndex(Definition definition, boolean isNew) {
    if (definition == null) {
      return -1;
    }
    Integer index = myDefinitions.get(definition);
    if (index == null) {
      // TODO
      // getDefinitionIndex(definition.getParentNamespace(), false);
      myDefinitionsList.add(new Entry(definition, myCounter, isNew));
      myDefinitions.put(definition, myCounter++);
      return myCounter - 1;
    } else {
      return index;
    }
  }

  public void serialize(DataOutputStream stream) throws IOException {
    stream.writeInt(myDefinitionsList.size());
    for (Entry entry : myDefinitionsList) {
      stream.writeInt(entry.index);
      if (entry.definition.getParentNamespace() != null) {
        // TODO
        // stream.writeInt(myDefinitions.get(entry.definition.getParentNamespace()));
        stream.writeBoolean(entry.definition.getName().fixity == Abstract.Definition.Fixity.PREFIX);
        stream.writeUTF(entry.definition.getName().name);
        stream.write(ModuleSerialization.getDefinitionCode(entry.definition));
        /*
        if (!(entry.definition instanceof Namespace)) {
          stream.writeBoolean(entry.isNew);
        }
        */
      } else {
        stream.writeInt(0);
      }
    }
  }
}
