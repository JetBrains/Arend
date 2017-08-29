package com.jetbrains.jetpad.vclang.frontend.namespace;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.naming.namespace.SimpleNamespace;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Group;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class SimpleStaticNamespaceProvider implements StaticNamespaceProvider {
  private final Map<GlobalReferable, Namespace> myNamespaces = new HashMap<>();

  @Nonnull
  @Override
  public Namespace forReferable(GlobalReferable referable) {
    return myNamespaces.get(referable);
  }

  public void collect(Group group, ErrorReporter errorReporter) {
    SimpleNamespace ns = new SimpleNamespace();
    for (Group subgroup : group.getSubgroups()) {
      ns.addDefinition(subgroup.getReferable(), errorReporter);
      for (Group subSubgroup : subgroup.getDynamicSubgroups()) {
        ns.addDefinition(subSubgroup.getReferable(), errorReporter);
      }
      collect(subgroup, errorReporter);
    }
    for (Group subgroup : group.getDynamicSubgroups()) {
      collect(subgroup, errorReporter);
    }
    myNamespaces.put(group.getReferable(), ns);
  }
}
