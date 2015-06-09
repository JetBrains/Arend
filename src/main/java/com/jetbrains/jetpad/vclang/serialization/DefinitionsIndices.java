package com.jetbrains.jetpad.vclang.serialization;

import com.jetbrains.jetpad.vclang.term.definition.Definition;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DefinitionsIndices {
  final private Map<Definition, Integer> myDefinitions = new HashMap<>();
  private int myCounter = 0;

  public int getDefinitionIndex(Definition definition) {
    Integer index = myDefinitions.get(definition);
    if (index == null) {
      myDefinitions.put(definition, myCounter++);
      return myCounter - 1;
    } else {
      return index;
    }
  }

  public void serialize(DataOutputStream stream) throws IOException {

  }
}
