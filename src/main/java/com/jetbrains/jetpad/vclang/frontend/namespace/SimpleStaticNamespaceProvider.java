package com.jetbrains.jetpad.vclang.frontend.namespace;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.namespace.EmptyNamespace;
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
    Namespace ns = myNamespaces.get(referable);
    return ns == null ? EmptyNamespace.INSTANCE : ns;
  }

  public void collect(Group group, ErrorReporter errorReporter) {
    SimpleNamespace ns = new SimpleNamespace();

    for (Group subgroup : group.getSubgroups()) {
      ns.addDefinition(subgroup.getReferable(), errorReporter);
      for (GlobalReferable constructor : subgroup.getConstructors()) {
        ns.addDefinition(constructor, errorReporter);
      }
      for (Group subgroup1 : subgroup.getDynamicSubgroups()) {
        ns.addDefinition(subgroup1.getReferable(), errorReporter);
      }
      for (GlobalReferable field : subgroup.getFields()) {
        ns.addDefinition(field, errorReporter);
      }
      collect(subgroup, errorReporter);
    }

    for (GlobalReferable constructor : group.getConstructors()) {
      ns.addDefinition(constructor, errorReporter);
    }
    for (Group subgroup : group.getDynamicSubgroups()) {
      ns.addDefinition(subgroup.getReferable(), errorReporter);
      collect(subgroup, errorReporter);
    }
    for (GlobalReferable field : group.getFields()) {
      ns.addDefinition(field, errorReporter);
    }
    myNamespaces.put(group.getReferable(), ns);
  }
}
