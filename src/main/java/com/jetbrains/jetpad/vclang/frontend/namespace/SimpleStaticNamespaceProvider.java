package com.jetbrains.jetpad.vclang.frontend.namespace;

import com.jetbrains.jetpad.vclang.error.DummyErrorReporter;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.namespace.EmptyNamespace;
import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.naming.namespace.SimpleNamespace;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ProxyErrorReporter;

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
    GlobalReferable groupRef = group.getReferable();
    LocalErrorReporter localErrorReporter = new ProxyErrorReporter(groupRef, errorReporter);

    for (Group subgroup : group.getSubgroups()) {
      ns.addDefinition(subgroup.getReferable(), localErrorReporter);
      collect(subgroup, errorReporter);
    }
    for (Group subgroup : group.getDynamicSubgroups()) {
      ns.addDefinition(subgroup.getReferable(), localErrorReporter);
      collect(subgroup, errorReporter);
    }
    for (GlobalReferable constructor : group.getConstructors()) {
      ns.addDefinition(constructor, localErrorReporter);
    }
    for (GlobalReferable field : group.getFields()) {
      ns.addDefinition(field, localErrorReporter);
    }

    for (Group subgroup : group.getSubgroups()) {
      collectSubgroup(subgroup, ns);
    }
    for (Group subgroup : group.getDynamicSubgroups()) {
      collectSubgroup(subgroup, ns);
    }

    myNamespaces.put(groupRef, ns);
  }

  private void collectSubgroup(Group subgroup, SimpleNamespace ns) {
    for (GlobalReferable constructor : subgroup.getConstructors()) {
      ns.addDefinition(constructor, DummyErrorReporter.INSTANCE);
    }
    for (GlobalReferable field : subgroup.getFields()) {
      ns.addDefinition(field, DummyErrorReporter.INSTANCE);
    }
  }
}
