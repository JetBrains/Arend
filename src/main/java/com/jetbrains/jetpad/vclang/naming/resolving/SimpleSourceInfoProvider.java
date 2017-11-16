package com.jetbrains.jetpad.vclang.naming.resolving;

import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.naming.FullName;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.term.provider.SourceInfoProvider;

import java.util.HashMap;
import java.util.Map;

public class SimpleSourceInfoProvider<SourceIdT extends SourceId> implements SourceInfoProvider<SourceIdT> {
  private final Map<GlobalReferable, SourceIdT> modules = new HashMap<>();
  private final Map<GlobalReferable, FullName> names = new HashMap<>();

  public void registerDefinition(GlobalReferable def, FullName name, SourceIdT source) {
    modules.put(def, source);
    names.put(def, name);
  }

  public void registerModule(Group group, SourceIdT source) {
    modules.put(group.getReferable(), source);
    registerGroup(group, null, source);
  }

  public void registerGroup(Group group, FullName name, SourceIdT source) {
    for (Group subGroup : group.getSubgroups()) {
      registerSubGroup(subGroup, new FullName(name, subGroup.getReferable().textRepresentation()), source);
    }
    for (GlobalReferable constructor : group.getConstructors()) {
      registerDefinition(constructor, new FullName(name, constructor.textRepresentation()), source);
    }
    for (Group subGroup : group.getDynamicSubgroups()) {
      registerSubGroup(subGroup, new FullName(name, subGroup.getReferable().textRepresentation()), source);
    }
    for (GlobalReferable field : group.getFields()) {
      registerDefinition(field, new FullName(name, field.textRepresentation()), source);
    }
  }

  // TODO[abstract]: What's the point of FullName here?
  private void registerSubGroup(Group group, FullName name, SourceIdT source) {
    registerDefinition(group.getReferable(), name, source);
    registerGroup(group, name, source);
  }

  @Override
  public FullName fullNameFor(GlobalReferable definition) {
    return names.get(definition);
  }

  @Override
  public SourceIdT sourceOf(GlobalReferable definition) {
    return modules.get(definition);
  }
}
