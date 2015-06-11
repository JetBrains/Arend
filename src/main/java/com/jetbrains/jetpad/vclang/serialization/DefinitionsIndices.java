package com.jetbrains.jetpad.vclang.serialization;

import com.jetbrains.jetpad.vclang.term.definition.Definition;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

import static com.jetbrains.jetpad.vclang.serialization.ModuleSerialization.getDefinitionCode;

public class DefinitionsIndices {
  final private Map<Definition, Integer> myDefinitions = new HashMap<>();
  final private List<Map.Entry<Definition, Integer>> myDefinitionsList = new ArrayList<>();
  private int myCounter = 1;

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
      stream.writeInt(entry.getKey().getParent() == null ? 0 : myDefinitions.get(entry.getKey().getParent()));
      stream.writeUTF(entry.getKey().getName());
      stream.write(getDefinitionCode(entry.getKey()));
    }
  }
}
