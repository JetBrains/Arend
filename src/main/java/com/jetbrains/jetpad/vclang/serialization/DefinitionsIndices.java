package com.jetbrains.jetpad.vclang.serialization;

import com.jetbrains.jetpad.vclang.term.definition.Definition;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class DefinitionsIndices {
  final private Map<Definition, Integer> myDefinitions = new HashMap<>();
  final private List<Map.Entry<Definition, Integer>> myDefinitionsList = new ArrayList<>();
  private int myCounter = 2;

  public int getDefinitionIndex(Definition definition) {
    if (definition == null) return -1;
    Integer index = myDefinitions.get(definition);
    if (index == null) {
      getDefinitionIndex(definition.getModule());
      myDefinitionsList.add(new AbstractMap.SimpleEntry<>(definition, myCounter));
      myDefinitions.put(definition, myCounter++);
      return myCounter - 1;
    } else {
      return index;
    }
  }

  public void serialize(DataOutputStream stream) throws IOException {
    for (Map.Entry<Definition, Integer> entry : myDefinitionsList) {
      stream.writeInt(entry.getValue());
      writeDefinition(stream, entry.getKey().getModule());
      stream.writeInt(0);
      stream.writeInt(entry.getKey().getName().length());
      stream.write(entry.getKey().getName().getBytes());
    }
  }

  private void writeDefinition(DataOutputStream stream, Definition definition) throws IOException {
    if (definition == null) return;
    writeDefinition(stream, definition.getModule());
    stream.writeInt(myDefinitions.get(definition));
  }
}
