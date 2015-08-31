package com.jetbrains.jetpad.vclang.serialization;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.NamespaceMember;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefinitionsIndices {
  private static class Entry {
    NamespaceMember member;
    int index;
    boolean isNew;

    public Entry(NamespaceMember member, int index, boolean isNew) {
      this.member = member;
      this.index = index;
      this.isNew = isNew;
    }
  }

  final private Map<NamespaceMember, Integer> myDefinitions = new HashMap<>();
  final private List<Entry> myDefinitionsList = new ArrayList<>();
  private int myCounter = 1;

  public int getDefinitionIndex(NamespaceMember definition, boolean isNew) {
    if (definition == null) return -1;
    Integer index = myDefinitions.get(definition);
    if (index == null) {
      getDefinitionIndex(definition.getNamespace().getParent(), false);
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
      if (entry.member.getNamespace().getParent() != null) {
        stream.writeInt(myDefinitions.get(entry.member.getNamespace().getParent()));
        stream.writeBoolean(entry.member.getName().fixity == Abstract.Definition.Fixity.PREFIX);
        stream.writeUTF(entry.member.getName().name);
        stream.write(ModuleSerialization.getDefinitionCode(entry.member));
        if (!(entry.member instanceof Namespace)) {
          stream.writeBoolean(entry.isNew);
        }
      } else {
        stream.writeInt(0);
      }
    }
  }
}
