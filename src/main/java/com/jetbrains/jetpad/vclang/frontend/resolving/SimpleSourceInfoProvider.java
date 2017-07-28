package com.jetbrains.jetpad.vclang.frontend.resolving;

import com.jetbrains.jetpad.vclang.frontend.Concrete;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.naming.FullName;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.SourceInfoProvider;

import java.util.HashMap;
import java.util.Map;

public class SimpleSourceInfoProvider<SourceIdT extends SourceId> implements SourceInfoProvider<SourceIdT> {
  private final Map<Abstract.Definition, SourceIdT> modules = new HashMap<>();
  private final Map<Abstract.Definition, FullName> names = new HashMap<>();

  public void registerDefinition(Abstract.Definition def, FullName name, SourceIdT source) {
    modules.put(def, source);
    names.put(def, name);
  }

  @Override
  public String nameFor(Abstract.Definition definition) {
    FullName name = names.get(definition);
    return name != null ? name.toString() : definition.getName();
  }

  @Override
  public String positionOf(Abstract.SourceNode sourceNode) {
    if (sourceNode instanceof Concrete.SourceNode) {
      Concrete.Position position = ((Concrete.SourceNode) sourceNode).getPosition();
      return position.line + ":" + position.column;
    } else {
      return null;
    }
  }

  @Override
  public String moduleOf(Abstract.SourceNode sourceNode) {
    if (sourceNode instanceof Concrete.SourceNode) {
      Concrete.Position position = ((Concrete.SourceNode) sourceNode).getPosition();
      return position.module != null ? position.module.toString() : null;
    } else {
      return null;
    }
  }

  @Override
  public SourceIdT sourceOf(Abstract.Definition definition) {
    return modules.get(definition);
  }
}
