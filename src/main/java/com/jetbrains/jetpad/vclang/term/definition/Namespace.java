package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.module.Module;
import com.jetbrains.jetpad.vclang.module.ModuleError;

import java.util.*;

public class Namespace {
  final Definition myOwner;

  private List<Definition> myPublicMembers;
  private Map<String, Definition> myPrivateMembers;
  private Map<String, Definition> myStaticMembers;

  public Namespace(Definition owner) {
    myOwner = owner;
  }

  public Definition getOwner() {
    return myOwner;
  }

  public Collection<Definition> getPrivateMembers() {
    return myPrivateMembers.values();
  }

  public Definition getPrivateMember(String name) {
    return myPrivateMembers == null ? null : myPrivateMembers.get(name);
  }

  public void addPrivateMember(Definition definition) {
    if (myPrivateMembers == null) {
      myPrivateMembers = new HashMap<>();
    }
    myPrivateMembers.put(definition.getName().name, definition);
  }

  public List<Definition> getPublicMembers() {
    return myPublicMembers;
  }

  public Definition getPublicMember(String name) {
    if (myPublicMembers == null) return null;
    for (Definition field : myPublicMembers) {
      if (field.getName().name.equals(name)) {
        return field;
      }
    }
    return null;
  }

  public boolean addPublicMember(Definition definition, List<ModuleError> errors) {
    Definition oldDefinition = getPublicMember(definition.getName().name);
    if (oldDefinition != null && !(oldDefinition instanceof ClassDefinition && definition instanceof ClassDefinition && (!((ClassDefinition) oldDefinition).hasAbstracts() || !((ClassDefinition) definition).hasAbstracts()))) {
      errors.add(new ModuleError(myOwner.getEnclosingModule(), "Name " + myOwner.getFullNestedMemberName(definition.getName().name) + " is already defined"));
      return false;
    }

    if (myPublicMembers == null) {
      myPublicMembers = new ArrayList<>();
    }
    myPublicMembers.add(definition);
    return true;
  }

  public Collection<Definition> getStaticMembers() {
    return myStaticMembers == null ? null : myStaticMembers.values();
  }

  public Definition getStaticMember(String name) {
    return myStaticMembers == null ? null : myStaticMembers.get(name);
  }

  public boolean checkDepsAddStaticMember(Definition definition, List<ModuleError> errors) {
    if (definition.isAbstract()) {
      Universe max = myOwner.getUniverse().max(definition.getUniverse());
      if (max == null) {
        String msg = "Universe " + definition.getUniverse() + " of the field " + myOwner.getFullNestedMemberName(definition.getName().getPrefixName()) + "is not compatible with universe " + myOwner.getUniverse() + " of previous fields";
        errors.add(new ModuleError(myOwner.getEnclosingModule(), msg));
        return false;
      }
      myOwner.setUniverse(max);
      return true;
    }

    boolean isStatic = true;
    if (definition.getDependencies() != null) {
      for (Definition dependency : definition.getDependencies()) {
        if (myPublicMembers.contains(dependency)) {
          isStatic = false;
        } else {
          myOwner.addDependency(dependency);
        }
      }
    }

    if (isStatic) {
      if (myStaticMembers == null) {
        myStaticMembers = new HashMap<>();
      }
      myStaticMembers.put(definition.getName().name, definition);
    }
    return true;
  }

  public ClassDefinition getClass(String name, List<ModuleError> errors) {
    if (myPublicMembers != null) {
      Definition definition = getPublicMember(name);
      if (definition != null) {
        if (definition instanceof ClassDefinition) {
          return (ClassDefinition) definition;
        } else {
          errors.add(new ModuleError(myOwner.getEnclosingModule(), "Name " + myOwner.getFullNestedMemberName(name) + " is already defined"));
          return null;
        }
      }
    }

    ClassDefinition result = new ClassDefinition(name, myOwner, !(myOwner instanceof ClassDefinition) || ((ClassDefinition) myOwner).isLocal());
    result.hasErrors(true);
    if (myPublicMembers == null) {
      myPublicMembers = new ArrayList<>();
    }
    myPublicMembers.add(result);
    if (myPrivateMembers == null) {
      myPrivateMembers = new HashMap<>();
    }
    myPrivateMembers.put(result.getName().name, result);
    return result;
  }

  public boolean addMember(Definition definition, List<ModuleError> errors) {
    if (!addPublicMember(definition, errors)) return false;
    addPrivateMember(definition);
    return true;
  }

  public void removeMember(Definition definition) {
    if (myPublicMembers != null) {
      myPublicMembers.remove(definition);
    }
    if (myPrivateMembers != null) {
      myPrivateMembers.remove(definition.getName().name);
    }
  }
}
