package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.naming.error.DuplicateDefinitionError;
import com.jetbrains.jetpad.vclang.naming.error.DuplicateInstanceError;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.util.Pair;

import java.util.*;

public class SimpleNamespace implements Namespace {
  private final Map<String, Abstract.Definition> myNames = new HashMap<>();
  private Map<Pair<Abstract.Definition, Abstract.Definition>, Abstract.ClassViewInstance> myInstances = Collections.emptyMap();

  public SimpleNamespace() {
  }

  public SimpleNamespace(SimpleNamespace other) {
    myNames.putAll(other.myNames);
  }

  public SimpleNamespace(Abstract.Definition def) {
    this();
    addDefinition(def);
  }

  public void addDefinition(Abstract.Definition def) {
    addDefinition(def.getName(), def);
  }

  public void addDefinition(String name, final Abstract.Definition def) {
    final Abstract.Definition prev = myNames.put(name, def);
    if (!(prev == null || prev == def)) {
      throw new InvalidNamespaceException() {
        @Override
        public GeneralError toError() {
          return new DuplicateDefinitionError(prev, def);
        }
      };
    }
  }

  public void addInstance(Abstract.ClassViewInstance instance) {
    Abstract.Definition classView = instance.getClassView().getReferent();
    addInstance(new Pair<>(classView, instance.getClassifyingDefinition()), instance);
    if (instance.isDefault()) {
      addInstance(new Pair<>(((Abstract.ClassView) classView).getUnderlyingClassDefCall().getReferent(), instance.getClassifyingDefinition()), instance);
    }
  }

  private void addInstance(Pair<Abstract.Definition, Abstract.Definition> pair, final Abstract.ClassViewInstance instance) {
    if (myInstances.isEmpty()) {
      myInstances = new HashMap<>();
    }
    final Abstract.ClassViewInstance prev = myInstances.put(pair, instance);
    if (!(prev == null || prev == instance)) {
      throw new InvalidNamespaceException() {
        @Override
        public GeneralError toError() {
          return new DuplicateInstanceError(prev, instance);
        }
      };
    }
  }

  public void addAll(SimpleNamespace other) {
    for (Map.Entry<String, Abstract.Definition> entry : other.myNames.entrySet()) {
      addDefinition(entry.getKey(), entry.getValue());
    }
    for (Map.Entry<Pair<Abstract.Definition, Abstract.Definition>, Abstract.ClassViewInstance> entry : other.myInstances.entrySet()) {
      addInstance(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public Set<String> getNames() {
    return myNames.keySet();
  }

  Set<Map.Entry<String, Abstract.Definition>> getEntrySet() {
    return myNames.entrySet();
  }

  @Override
  public Abstract.Definition resolveName(String name) {
    return myNames.get(name);
  }

  @Override
  public Collection<? extends Abstract.ClassViewInstance> getInstances() {
    return myInstances.values();
  }

  @Override
  public Abstract.ClassViewInstance resolveInstance(Abstract.ClassView classView, Abstract.Definition classifyingDefinition) {
    return myInstances.get(new Pair<Abstract.Definition, Abstract.Definition>(classView, classifyingDefinition));
  }

  @Override
  public Abstract.ClassViewInstance resolveInstance(Abstract.ClassDefinition classDefinition, Abstract.Definition classifyingDefinition) {
    return myInstances.get(new Pair<Abstract.Definition, Abstract.Definition>(classDefinition, classifyingDefinition));
  }
}
