package com.jetbrains.jetpad.vclang.frontend.resolving;

import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.naming.FullName;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
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

  @Override
  public String fullNameFor(GlobalReferable definition) {
    FullName name = names.get(definition);
    return name != null ? name.toString() : definition.getName();
  }

  @Override
  public SourceIdT sourceOf(GlobalReferable definition) {
    return modules.get(definition);
  }

  @Override
  public Abstract.Precedence precedenceOf(GlobalReferable referable) {
    return referable instanceof Concrete.Definition ? ((Concrete.Definition) referable).getPrecedence() : Abstract.Precedence.DEFAULT;
  }

  @Override
  public String nameFor(Referable referable) {
    return referable.getName();
  }
}
