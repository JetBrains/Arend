package com.jetbrains.jetpad.vclang.serialization;

import com.jetbrains.jetpad.vclang.term.definition.Definition;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class DefinitionsIndices {
  final private Map<Definition, Integer> myDefinitions = new HashMap<>();
  final private List<Map.Entry<Definition, Integer>> myDefinitionsList = new ArrayList<>();
  private int myCounter = 0;

  public int getDefinitionIndex(Definition definition) {
    if (definition == null) return -1;
    Integer index = myDefinitions.get(definition);
    if (index == null) {
      getDefinitionIndex(definition.getParent());
      myDefinitionsList.add(new AbstractMap.SimpleEntry<>(definition, myCounter));
      myDefinitions.put(definition, myCounter++);
      return myCounter - 1;
    } else {
      return index;
    }
  }

  public void serialize(DataOutputStream stream) throws IOException {
    stream.writeInt(myDefinitionsList.size());
    for (Map.Entry<Definition, Integer> entry : myDefinitionsList) {
      stream.writeInt(entry.getValue());
      if (entry.getValue() != 0) {
        stream.writeInt(myDefinitions.get(entry.getKey().getParent()));
        stream.writeUTF(entry.getKey().getName());
      }
    }
  }
}
