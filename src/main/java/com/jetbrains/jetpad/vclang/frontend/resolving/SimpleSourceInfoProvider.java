package com.jetbrains.jetpad.vclang.frontend.resolving;

import com.jetbrains.jetpad.vclang.frontend.Concrete;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.naming.FullName;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.provider.SourceInfoProvider;

import java.util.HashMap;
import java.util.Map;

public class SimpleSourceInfoProvider<SourceIdT extends SourceId> implements SourceInfoProvider<SourceIdT> {
  private final Map<Abstract.GlobalReferableSourceNode, SourceIdT> modules = new HashMap<>();
  private final Map<Abstract.GlobalReferableSourceNode, FullName> names = new HashMap<>();

  public void registerDefinition(Abstract.GlobalReferableSourceNode def, FullName name, SourceIdT source) {
    modules.put(def, source);
    names.put(def, name);
  }

  @Override
  public String fullNameFor(Abstract.GlobalReferableSourceNode definition) {
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
  public SourceIdT sourceOf(Abstract.GlobalReferableSourceNode definition) {
    return modules.get(definition);
  }

  @Override
  public Abstract.Precedence precedenceOf(Abstract.GlobalReferableSourceNode referable) {
    return referable instanceof Concrete.Definition ? ((Concrete.Definition) referable).getPrecedence() : Abstract.Precedence.DEFAULT;
  }

  @Override
  public String nameFor(Abstract.ReferableSourceNode referable) {
    return referable.getName();
  }
}
