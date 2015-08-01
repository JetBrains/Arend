package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.module.Module;
import com.jetbrains.jetpad.vclang.module.ModuleError;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;

import java.util.*;

public class ClassDefinition extends Definition implements Abstract.ClassDefinition {
  private List<Definition> myPublicFields;
  private Map<String, Definition> myStaticFields;
  private Map<String, Definition> myPrivateFields;
  private boolean myIsLocal;

  public ClassDefinition(String name, Definition parent) {
    this(name, parent, false);
  }

  public ClassDefinition(String name, Definition parent, boolean isLocal) {
    super(new Utils.Name(name, Fixity.PREFIX), parent, DEFAULT_PRECEDENCE);
    myIsLocal = isLocal;
    hasErrors(false);
  }

  public boolean isLocal() {
    return myIsLocal;
  }

  @Override
  public Expression getType() {
    return new UniverseExpression(getUniverse());
  }

  @Override
  public List<Definition> getPublicFields() {
    return myPublicFields;
  }

  @Override
  public Definition getStaticField(String name) {
    return myStaticFields == null ? null : myStaticFields.get(name);
  }

  @Override
  public Collection<Definition> getStaticFields() {
    return myStaticFields == null ? null : myStaticFields.values();
  }

  public Definition getPublicField(String name) {
    if (myPublicFields == null) return null;
    for (Definition field : myPublicFields) {
      if (field.getName().name.equals(name)) {
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

  private Module getEnclosingModule() {
    for (Definition def = this;; def = def.getParent()) {
      if (def instanceof ClassDefinition && !((ClassDefinition) def).isLocal())
        return new Module((ClassDefinition)def.getParent(), def.getName().name);
    }
  }

  private String getFullNestedName(String name) {
    String result = name;
    for (Definition def = this; !(def instanceof ClassDefinition) || ((ClassDefinition) def).isLocal(); def = def.getParent()) {
      result = def.getName() + "." + result;
    }
    return result;
  }

  public ClassDefinition getClass(String name, List<ModuleError> errors) {
    if (myPublicFields != null) {
      Definition definition = getPublicField(name);
      if (definition != null) {
        if (definition instanceof ClassDefinition) {
          return (ClassDefinition) definition;
        } else {
          if (!isLocal()) {
            errors.add(new ModuleError(new Module(this, name), "Name is already defined"));
          } else {
            errors.add(new ModuleError(getEnclosingModule(), "Name " + getFullNestedName(name) + " is already defined"));
          }
          return null;
        }
      }
    }

    ClassDefinition result = new ClassDefinition(name, this, myIsLocal);
    result.hasErrors(true);
    if (myPublicFields == null) {
      myPublicFields = new ArrayList<>();
    }
    myPublicFields.add(result);
    if (myPrivateFields == null) {
      myPrivateFields = new HashMap<>();
    }
    myPrivateFields.put(result.getName().name, result);
    return result;
  }

  public boolean hasAbstracts() {
    if (myPublicFields == null) return false;
    for (Definition field : myPublicFields) {
      if (field.isAbstract()) return true;
    }
    return false;
  }

  public boolean addPublicField(Definition definition, List<ModuleError> errors) {
    Definition oldDefinition = getPublicField(definition.getName().name);
    if (oldDefinition != null && !(oldDefinition instanceof ClassDefinition && definition instanceof ClassDefinition && (!((ClassDefinition) oldDefinition).hasAbstracts() || !((ClassDefinition) definition).hasAbstracts()))) {
      errors.add(new ModuleError(new Module(this, definition.getName().name), "Name is already defined"));
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
    myPrivateFields.put(definition.getName().name, definition);
  }

  public boolean addStaticField(Definition definition, List<ModuleError> errors) {
    if (definition.isAbstract()) {
      Universe max = getUniverse().max(definition.getUniverse());
      if (max == null) {
        String msg = "Universe " + definition.getUniverse() + " of the field is not compatible with universe " + getUniverse() + " of previous fields";
        errors.add(new ModuleError(new Module(this, definition.getName().getPrefixName()), msg));
        return false;
      }
      setUniverse(max);
      return true;
    }

    boolean isStatic = true;
    if (definition.getDependencies() != null) {
      for (Definition dependency : definition.getDependencies()) {
        if (myPublicFields.contains(dependency)) {
          isStatic = false;
        } else {
          addDependency(dependency);
        }
      }
    }
    if (isStatic) {
      if (myStaticFields == null) {
        myStaticFields = new HashMap<>();
      }
      myStaticFields.put(definition.getName().name, definition);
    }

    return true;
  }

  public boolean addField(Definition definition, List<ModuleError> errors) {
    if (!addPublicField(definition, errors)) return false;
    addPrivateField(definition);
    return true;
  }

  public void removeField(Definition definition) {
    if (myPublicFields != null) {
      myPublicFields.remove(definition);
    }
    if (myPrivateFields != null) {
      myPrivateFields.remove(definition.getName().name);
    }
  }
}
