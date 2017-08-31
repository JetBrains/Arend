package com.jetbrains.jetpad.vclang.naming.resolving;

import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.naming.FullName;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.term.Precedence;
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

  // TODO[abstract]: What's the point of FullName here?
  public void registerGroup(Group group, FullName name, SourceIdT source) {
    registerDefinition(group.getReferable(), name, source);
    for (Group subGroup : group.getSubgroups()) {
      registerGroup(subGroup, new FullName(subGroup.getReferable().textRepresentation()), source);
    }
    for (GlobalReferable constructor : group.getConstructors()) {
      registerDefinition(constructor, new FullName(constructor.textRepresentation()), source);
    }
    for (Group subGroup : group.getDynamicSubgroups()) {
      registerGroup(subGroup, new FullName(name, subGroup.getReferable().textRepresentation()), source);
    }
    for (GlobalReferable field : group.getFields()) {
      registerDefinition(field, new FullName(name, field.textRepresentation()), source);
    }
  }

  @Override
  public String fullNameFor(GlobalReferable definition) {
    FullName name = names.get(definition);
    return name != null ? name.toString() : definition.textRepresentation();
  }

  @Override
  public SourceIdT sourceOf(GlobalReferable definition) {
    return modules.get(definition);
  }

  @Override
  public Precedence precedenceOf(GlobalReferable referable) {
    return referable instanceof Concrete.Definition ? ((Concrete.Definition) referable).getPrecedence() : Precedence.DEFAULT;
  }

  @Override
  public String nameFor(Referable referable) {
    return referable.textRepresentation();
  }
}
