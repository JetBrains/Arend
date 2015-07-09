package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.module.Module;
import com.jetbrains.jetpad.vclang.module.ModuleError;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;

import java.util.*;

public class ClassDefinition extends Definition implements Abstract.ClassDefinition {
  private List<Definition> myPublicFields;
  private Map<String, Definition> myStaticFields;
  private Map<String, Definition> myPrivateFields;

  public ClassDefinition(String name, Definition parent) {
    super(name, parent, DEFAULT_PRECEDENCE, Fixity.PREFIX);
    hasErrors(false);
  }

  @Override
  public Expression getType() {
    return new UniverseExpression(getUniverse());
  }

  @Override
  public List<Definition> getFields() {
    return myPublicFields;
  }

  @Override
  public Definition findChild(String name) {
    return myStaticFields == null ? null : myStaticFields.get(name);
  }

  @Override
  public Collection<Definition> getChildren() {
    return myStaticFields == null ? null : myStaticFields.values();
  }

  public Definition getPublicField(String name) {
    if (myPublicFields == null) return null;
    for (Definition field : myPublicFields) {
      if (field.getName().equals(name)) {
        return field;
      }
    }
    return null;
  }

  public Definition getPrivateField(String name) {
    return myPrivateFields == null ? null : myPrivateFields.get(name);
  }

  @Override
  public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitClass(this, params);
  }

  public ClassDefinition getClass(String name, List<ModuleError> errors) {
    if (myPublicFields != null) {
      Definition definition = getPublicField(name);
      if (definition != null) {
        if (definition instanceof ClassDefinition) {
          return (ClassDefinition) definition;
        } else {
          errors.add(new ModuleError(new Module(this, name), "Name is already defined"));
          return null;
        }
      }
    }

    ClassDefinition result = new ClassDefinition(name, this);
    if (myPublicFields == null) {
      myPublicFields = new ArrayList<>();
    }
    myPublicFields.add(result);
    return result;
  }

  private boolean hasAbstracts() {
    if (myPublicFields == null) return false;
    for (Definition field : myPublicFields) {
      if (field.isAbstract()) return true;
    }
    return false;
  }

  public boolean addPublicField(Definition definition, List<ModuleError> errors) {
    Definition oldDefinition = getPublicField(definition.getName());
    if (oldDefinition != null && !(oldDefinition instanceof ClassDefinition && definition instanceof ClassDefinition && (!((ClassDefinition) oldDefinition).hasAbstracts() || !((ClassDefinition) definition).hasAbstracts()))) {
      errors.add(new ModuleError(new Module(this, definition.getName()), "Name is already defined"));
      return false;
    }

    if (myPublicFields == null) {
      myPublicFields = new ArrayList<>();
    }
    myPublicFields.add(definition);
    return true;
  }

  public void addPrivateField(Definition definition) {
    if (myPrivateFields == null) {
      myPrivateFields = new HashMap<>();
    }
    myPrivateFields.put(definition.getName(), definition);
  }

  public void addStaticField(Definition definition) {
    // TODO: check if definition is static & update dependencies and universe of the parent
    if (myStaticFields == null) {
      myStaticFields = new HashMap<>();
    }
    myStaticFields.put(definition.getName(), definition);
  }

  public boolean addField(Definition definition, List<ModuleError> errors) {
    if (!addPublicField(definition, errors)) return false;
    addPrivateField(definition);
    return true;
  }

  /*
  public boolean add(Definition definition, List<ModuleError> errors) {
    return add(definition, definition.getParent() == this || definition instanceof Constructor && definition.getParent() != null && definition.getParent().getParent() == this, errors);
  }

  public boolean add(Definition definition, boolean export, List<ModuleError> errors) {
    if (definition instanceof FunctionDefinition && definition.isAbstract()) {
      Universe max = getUniverse().max(definition.getUniverse());
      if (max == null) {
        String msg = "Universe " + definition.getUniverse() + " of the field is not compatible with universe " + getUniverse() + " of previous fields";
        errors.add(new ModuleError(new Module(this, definition.getName()), msg));
        return false;
      }

      Definition old = myAbstracts.putIfAbsent(definition.getName(), (FunctionDefinition) definition);
      if (old != null) {
        errors.add(new ModuleError(new Module(this, definition.getName()), "Name is already defined"));
        return false;
      }

      setUniverse(max);
    }

    List<Definition> children = myFields.get(definition.getName());
    if (children == null) {
      children = new ArrayList<>(1);
      children.add(definition);
      myFields.put(definition.getName(), children);
    } else {
      if (!children.contains(definition)) {
        children.add(definition);
      }
    }

    return true; // foundAny || !export || addExport(definition, errors);
  }

  public void addDependencies(Set<FunctionDefinition> dependencies) {
    boolean foundAny = false;
    if (dependencies != null) {
      for (Definition dependency : definition.getDependencies()) {
        boolean found = false;
        for (FunctionDefinition abstractFunction : myAbstracts.values()) {
          if (dependency == abstractFunction) {
            found = true;
            foundAny = true;
            break;
          }
        }

        if (!found) {
          if (getDependencies() == null) {
            setDependencies(new HashSet<Definition>());
          }
          getDependencies().add(dependency);
        }
      }
    }
  }

  public boolean addExport(Definition definition, List<ModuleError> errors) {
    Definition old = myExports.putIfAbsent(definition.getName(), definition);
    if (old != null) {
      errors.add(new ModuleError(new Module(this, definition.getName()), "Name is already exported"));
      return false;
    } else {
      return true;
    }
  }

  public void remove(Definition definition) {
    myExports.remove(definition.getName());
    List<Definition> definitions = myFields.get(definition.getName());
    if (definitions != null) {
      definitions.remove(definition);
    }
    if (definition instanceof FunctionDefinition && definition.isAbstract()) {
      myAbstracts.remove(definition.getName());
    }
  }
  */
}
