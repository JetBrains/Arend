package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.AbstractStatementVisitor;

import java.util.*;

public class SimpleClassViewInstanceProvider implements ClassViewInstanceProvider {
  private final Map<ClassViewInstanceKey<Abstract.Definition, Abstract.ClassView>, Set<Abstract.ClassViewInstance>> myInstances = new HashMap<>();

  @Override
  public Set<Abstract.ClassViewInstance> getInstances(Abstract.Definition definition, Abstract.ClassView classView) {
    Abstract.Definition parent = definition.getParent();
    if (parent == null) {
      return Collections.emptySet();
    }
    Set<Abstract.ClassViewInstance> result = myInstances.get(new ClassViewInstanceKey<>(parent, classView));
    return result == null ? Collections.<Abstract.ClassViewInstance>emptySet() : result;
  }

  private class CollectInstancesDefinitionVisitor implements AbstractDefinitionVisitor<Void, Void>, AbstractStatementVisitor<Void, Void> {
    private void addInstance(Abstract.Definition definition, Abstract.ClassViewInstance instance) {
      ClassViewInstanceKey<Abstract.Definition, Abstract.ClassView> key = new ClassViewInstanceKey<>(definition, null);
      Set<Abstract.ClassViewInstance> instances = myInstances.get(key);
      if (instances == null) {
        instances = new HashSet<>();
        myInstances.put(key, instances);
      }
      instances.add(instance);
    }

    @Override
    public Void visitFunction(Abstract.FunctionDefinition def, Void params) {
      for (Abstract.Statement statement : def.getStatements()) {
        if (statement instanceof Abstract.DefineStatement) {
          Abstract.Definition definition = ((Abstract.DefineStatement) statement).getDefinition();
          if (definition instanceof Abstract.ClassViewInstance) {
            addInstance(def, (Abstract.ClassViewInstance) definition);
          }
        }
      }
      for (Abstract.Statement statement : def.getStatements()) {
        statement.accept(this, null);
      }
      return null;
    }

    @Override
    public Void visitClassField(Abstract.ClassField def, Void params) {
      return null;
    }

    @Override
    public Void visitData(Abstract.DataDefinition def, Void params) {
      return null;
    }

    @Override
    public Void visitConstructor(Abstract.Constructor def, Void params) {
      return null;
    }

    @Override
    public Void visitClass(Abstract.ClassDefinition def, Void params) {
      for (Abstract.Statement statement : def.getGlobalStatements()) {
        if (statement instanceof Abstract.DefineStatement) {
          Abstract.Definition definition = ((Abstract.DefineStatement) statement).getDefinition();
          if (definition instanceof Abstract.ClassViewInstance) {
            addInstance(def, (Abstract.ClassViewInstance) definition);
          }
        }
      }
      for (Abstract.Statement statement : def.getGlobalStatements()) {
        statement.accept(this, null);
      }
      return null;
    }

    @Override
    public Void visitImplement(Abstract.Implementation def, Void params) {
      return null;
    }

    @Override
    public Void visitClassView(Abstract.ClassView def, Void params) {
      return null;
    }

    @Override
    public Void visitClassViewField(Abstract.ClassViewField def, Void params) {
      return null;
    }

    @Override
    public Void visitClassViewInstance(Abstract.ClassViewInstance def, Void params) {
      return null;
    }

    @Override
    public Void visitDefine(Abstract.DefineStatement stat, Void params) {
      stat.getDefinition().accept(this, null);
      return null;
    }

    @Override
    public Void visitNamespaceCommand(Abstract.NamespaceCommandStatement stat, Void params) {
      return null;
    }
  }
}
