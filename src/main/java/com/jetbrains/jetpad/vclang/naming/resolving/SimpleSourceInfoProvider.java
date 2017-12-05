package com.jetbrains.jetpad.vclang.naming.resolving;

import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.term.provider.SourceInfoProvider;
import com.jetbrains.jetpad.vclang.util.LongName;

import java.util.*;

public class SimpleSourceInfoProvider<SourceIdT extends SourceId> implements SourceInfoProvider<SourceIdT> {
  private final Map<GlobalReferable, SourceIdT> myModules = new HashMap<>();
  private final Map<GlobalReferable, String> myCacheIds = new HashMap<>();

  public void registerDefinition(GlobalReferable def, LongName longName, SourceIdT source) {
    myModules.put(def, source);
    myCacheIds.put(def, longName.toString());
  }

  private static LongName makeLongName(LongName parent, String name) {
    List<String> path = parent.toList();
    List<String> newPath = new ArrayList<>(path.size() + 1);
    newPath.addAll(path);
    newPath.add(name);
    return new LongName(newPath);
  }

  public void registerModule(Group group, SourceIdT source) {
    myModules.put(group.getReferable(), source);
    registerGroup(group, new LongName(Collections.emptyList()), source);
  }

  private void registerGroup(Group group, LongName name, SourceIdT source) {
    for (Group subGroup : group.getSubgroups()) {
      registerSubGroup(subGroup, makeLongName(name, subGroup.getReferable().textRepresentation()), source);
    }
    for (Group.InternalReferable internalRef : group.getConstructors()) {
      GlobalReferable constructor = internalRef.getReferable();
      registerDefinition(constructor, makeLongName(name, constructor.textRepresentation()), source);
    }
    for (Group subGroup : group.getDynamicSubgroups()) {
      registerSubGroup(subGroup, makeLongName(name, subGroup.getReferable().textRepresentation()), source);
    }
    for (Group.InternalReferable internalRef : group.getFields()) {
      GlobalReferable field = internalRef.getReferable();
      registerDefinition(field, makeLongName(name, field.textRepresentation()), source);
    }
  }

  private void registerSubGroup(Group group, LongName name, SourceIdT source) {
    registerDefinition(group.getReferable(), name, source);
    registerGroup(group, name, source);
  }

  @Override
  public String cacheIdFor(GlobalReferable definition) {
    return myCacheIds.get(definition);
  }

  @Override
  public SourceIdT sourceOf(GlobalReferable definition) {
    return myModules.get(definition);
  }
}
