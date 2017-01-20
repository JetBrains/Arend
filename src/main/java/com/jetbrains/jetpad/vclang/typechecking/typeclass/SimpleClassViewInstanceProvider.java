package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.AbstractStatementVisitor;

import java.util.*;

public class SimpleClassViewInstanceProvider implements ClassViewInstanceProvider {
  private final Map<Abstract.Definition, Map<Abstract.ClassView, Set<Abstract.ClassViewInstance>>> myInstances = new HashMap<>();

  @Override
  public Set<Abstract.ClassViewInstance> getInstances(Abstract.Definition definition, Abstract.ClassView classView) {
    Set<Abstract.ClassViewInstance> instances = getInstances(definition).get(classView);
    return instances == null ? Collections.<Abstract.ClassViewInstance>emptySet() : instances;
  }

  private Map<Abstract.ClassView, Set<Abstract.ClassViewInstance>> getInstances(Abstract.Definition definition) {
    Abstract.Definition parent = definition.getParent();
    if (parent == null) {
      return Collections.emptyMap();
    }
    Map<Abstract.ClassView, Set<Abstract.ClassViewInstance>> result = myInstances.get(parent);
    return result == null ? new CollectInstancesDefinitionVisitor().collectInstances(parent) : result;
  }

  private static <T, S> void merge(Map<T, Set<S>> map1, Map<T, Set<S>> map2) {
    for (Map.Entry<T, Set<S>> entry : map2.entrySet()) {
      Set<S> set = map1.get(entry.getKey());
      if (set == null) {
        map1.put(entry.getKey(), entry.getValue());
      } else {
        set.addAll(entry.getValue());
      }
    }
  }

  private class CollectInstancesDefinitionVisitor implements AbstractDefinitionVisitor<Map<Abstract.ClassView, Set<Abstract.ClassViewInstance>>, Void>, AbstractStatementVisitor<Map<Abstract.ClassView, Set<Abstract.ClassViewInstance>>, Void> {
    private Map<Abstract.ClassView, Set<Abstract.ClassViewInstance>> collectInstances(Abstract.Definition definition) {
      Map<Abstract.ClassView, Set<Abstract.ClassViewInstance>> result = new HashMap<>();
      for (Map.Entry<Abstract.ClassView, Set<Abstract.ClassViewInstance>> entry : getInstances(definition).entrySet()) {
        result.put(entry.getKey(), new HashSet<>(entry.getValue()));
      }
      definition.accept(this, result);
      merge(result, getInstances(definition));
      myInstances.put(definition, result);
      return result;
    }

    private void addInstance(Abstract.ClassViewInstance instance, Map<Abstract.ClassView, Set<Abstract.ClassViewInstance>> result) {
      Abstract.ClassView classView = (Abstract.ClassView) instance.getClassView().getReferent();
      Set<Abstract.ClassViewInstance> instances = result.get(classView);
      if (instances == null) {
        instances = new HashSet<>();
        result.put(classView, instances);
      }
      instances.add(instance);
    }

    @Override
    public Void visitFunction(Abstract.FunctionDefinition def, Map<Abstract.ClassView, Set<Abstract.ClassViewInstance>> result) {
      for (Abstract.Statement statement : def.getStatements()) {
        if (statement instanceof Abstract.DefineStatement) {
          Abstract.Definition definition = ((Abstract.DefineStatement) statement).getDefinition();
          if (definition instanceof Abstract.ClassViewInstance) {
            addInstance((Abstract.ClassViewInstance) definition, result);
          }
        }
      }
      for (Abstract.Statement statement : def.getStatements()) {
        statement.accept(this, null);
      }
      return null;
    }

    @Override
    public Void visitClassField(Abstract.ClassField def, Map<Abstract.ClassView, Set<Abstract.ClassViewInstance>> result) {
      return null;
    }

    @Override
    public Void visitData(Abstract.DataDefinition def, Map<Abstract.ClassView, Set<Abstract.ClassViewInstance>> result) {
      return null;
    }

    @Override
    public Void visitConstructor(Abstract.Constructor def, Map<Abstract.ClassView, Set<Abstract.ClassViewInstance>> result) {
      return null;
    }

    @Override
    public Void visitClass(Abstract.ClassDefinition def, Map<Abstract.ClassView, Set<Abstract.ClassViewInstance>> result) {
      for (Abstract.Statement statement : def.getGlobalStatements()) {
        if (statement instanceof Abstract.DefineStatement) {
          Abstract.Definition definition = ((Abstract.DefineStatement) statement).getDefinition();
          if (definition instanceof Abstract.ClassViewInstance) {
            addInstance((Abstract.ClassViewInstance) definition, result);
          }
        }
      }
      for (Abstract.Statement statement : def.getGlobalStatements()) {
        statement.accept(this, null);
      }
      return null;
    }

    @Override
    public Void visitImplement(Abstract.Implementation def, Map<Abstract.ClassView, Set<Abstract.ClassViewInstance>> result) {
      return null;
    }

    @Override
    public Void visitClassView(Abstract.ClassView def, Map<Abstract.ClassView, Set<Abstract.ClassViewInstance>> result) {
      return null;
    }

    @Override
    public Void visitClassViewField(Abstract.ClassViewField def, Map<Abstract.ClassView, Set<Abstract.ClassViewInstance>> result) {
      return null;
    }

    @Override
    public Void visitClassViewInstance(Abstract.ClassViewInstance def, Map<Abstract.ClassView, Set<Abstract.ClassViewInstance>> result) {
      return null;
    }

    @Override
    public Void visitDefine(Abstract.DefineStatement stat, Map<Abstract.ClassView, Set<Abstract.ClassViewInstance>> result) {
      stat.getDefinition().accept(this, null);
      return null;
    }

    @Override
    public Void visitNamespaceCommand(Abstract.NamespaceCommandStatement stat, Map<Abstract.ClassView, Set<Abstract.ClassViewInstance>> result) {
      return null;
    }
  }
}
