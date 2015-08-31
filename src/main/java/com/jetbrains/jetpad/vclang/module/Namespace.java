package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.NamespaceMember;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;

import java.util.*;

public class Namespace implements NamespaceMember {
  final private Utils.Name myName;
  private Namespace myParent;
  private Map<String, Namespace> myChildren;
  private Map<String, Definition> myDefinitions;

  public Namespace(Utils.Name name, Namespace parent) {
    myName = name;
    myParent = parent;
  }

  @Override
  public Namespace getNamespace() {
    return this;
  }

  @Override
  public Utils.Name getName() {
    return myName;
  }

  public String getFullName() {
    return myParent == null || myParent == RootModule.ROOT ? myName.name : myParent.getFullName() + "." + myName.name;
  }

  public Namespace getParent() {
    return myParent;
  }

  public void setParent(Namespace parent) {
    myParent = parent;
  }

  public Collection<Definition> getDefinitions() {
    return myDefinitions == null ? Collections.<Definition>emptyList() : myDefinitions.values();
  }

  public Collection<Namespace> getChildren() {
    return myChildren == null ? Collections.<Namespace>emptyList() : myChildren.values();
  }

  public Namespace getChild(Utils.Name name) {
    if (myChildren != null) {
      Namespace child = myChildren.get(name.name);
      if (child != null) {
        return child;
      }
    } else {
      myChildren = new HashMap<>();
    }

    Namespace child = new Namespace(name, this);
    myChildren.put(name.name, child);
    return child;
  }

  public Namespace findChild(String name) {
    return myChildren == null ? null : myChildren.get(name);
  }

  public void removeChild(String name) {
    if (myChildren != null) {
      myChildren.remove(name);
    }
  }

  public Namespace addChild(Namespace child) {
    if (myChildren == null) {
      myChildren = new HashMap<>();
      myChildren.put(child.myName.name, child);
      return null;
    } else {
      Namespace oldChild = myChildren.get(child.myName.name);
      if (oldChild != null) {
        return oldChild;
      } else {
        myChildren.put(child.myName.name, child);
        return null;
      }
    }
  }

  public Definition getDefinition(String name) {
    return myDefinitions == null ? null : myDefinitions.get(name);
  }

  public NamespaceMember getMember(String name) {
    NamespaceMember member = getDefinition(name);
    if (member != null) {
      return member;
    }
    return findChild(name);
  }

  public NamespaceMember locateName(String name) {
    for (Namespace namespace = this; namespace != null; namespace = namespace.getParent()) {
      NamespaceMember member = namespace.getMember(name);
      if (member != null) {
        return member;
      }
    }
    return null;
  }

  public Definition addDefinition(Definition definition) {
    if (myDefinitions == null) {
      myDefinitions = new HashMap<>();
      myDefinitions.put(definition.getName().name, definition);
      return null;
    } else {
      Definition oldDefinition = myDefinitions.get(definition.getName().name);
      if (oldDefinition != null) {
        return oldDefinition;
      } else {
        myDefinitions.put(definition.getName().name, definition);
        return null;
      }
    }
  }

  public void removeDefinition(Definition definition) {
    if (myDefinitions != null) {
      Definition removed = myDefinitions.remove(definition.getName().name);
      if (removed != definition) {
        myDefinitions.put(definition.getName().name, removed);
      }
    }
  }

  public NamespaceMember addMember(NamespaceMember member) {
    if (member instanceof Namespace) {
      return addChild((Namespace) member);
    }
    if (member instanceof Definition) {
      return addDefinition((Definition) member);
    }
    throw new IllegalStateException();
  }

  public void removeMember(NamespaceMember member) {
    if (member instanceof Namespace) {
      removeChild(member.getName().name);
    } else
    if (member instanceof Definition) {
      removeDefinition((Definition) member);
    } else {
      throw new IllegalStateException();
    }
  }

  public void clear() {
    myChildren = null;
    myDefinitions = null;
  }

  @Override
  public String toString() {
    return getFullName();
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (!(other instanceof Namespace)) return false;
    if (myParent != ((Namespace) other).myParent) return false;
    if (myName == null) return ((Namespace) other).myName == null;
    return myName.name.equals(((Namespace) other).myName.name);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(new Object[]{myParent, myName == null ? null : myName.name});
  }
}
