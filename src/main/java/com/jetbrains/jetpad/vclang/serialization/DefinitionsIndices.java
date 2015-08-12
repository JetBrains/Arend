package com.jetbrains.jetpad.vclang.serialization;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.NamespaceMember;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class DefinitionsIndices {
  final private Map<NamespaceMember, Integer> myDefinitions = new HashMap<>();
  final private List<Map.Entry<NamespaceMember, Integer>> myDefinitionsList = new ArrayList<>();
  private int myCounter = 1;

  public int getDefinitionIndex(NamespaceMember definition) {
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
    for (Map.Entry<NamespaceMember, Integer> entry : myDefinitionsList) {
      stream.writeInt(entry.getValue());
      if (entry.getKey().getParent() != null) {
        stream.writeInt(myDefinitions.get(entry.getKey().getParent()));
        stream.writeBoolean(entry.getKey().getName().fixity == Abstract.Definition.Fixity.PREFIX);
        stream.writeUTF(entry.getKey().getName().name);
        stream.write(ModuleSerialization.getDefinitionCode(entry.getKey()));
      } else {
        stream.writeInt(0);
      }
    }
  }
}
