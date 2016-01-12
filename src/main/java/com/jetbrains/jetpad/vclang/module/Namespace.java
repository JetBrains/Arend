package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.NamespaceMember;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;

import java.util.*;

public class Namespace {
  final private String myName;
  private Namespace myParent;
  private Map<String, NamespaceMember> myMembers;

  private Namespace(String name, Namespace parent) {
    myName = name;
    myParent = parent;
  }

  public Namespace(String name) {
    myName = name;
    myParent = null;
  }

  public ResolvedName getResolvedName() {
    return new ResolvedName(myParent, myName);
  }

  public String getName() {
    return myName;
  }

  public String getFullName() {
    return myParent == null || myParent == RootModule.ROOT ? myName : myParent.getFullName() + "." + myName;
  }

  public Namespace getParent() {
    return myParent;
  }

  public void setParent(Namespace parent) {
    myParent = parent;
  }

  public Collection<NamespaceMember> getMembers() {
    return myMembers == null ? Collections.<NamespaceMember>emptyList() : myMembers.values();
  }

  public Namespace getChild(String name) {
    NamespaceMember member = getMember(name);
    if (member != null) {
      return member.namespace;
    }

    return addChild(new Namespace(name, this)).namespace;
  }

  public Namespace findChild(String name) {
    NamespaceMember member = getMember(name);
    return member == null ? null : member.namespace;
  }

  public NamespaceMember addChild(Namespace child) {
    if (myMembers == null) {
      myMembers = new HashMap<>();
      NamespaceMember result = new NamespaceMember(child, null, null);
      myMembers.put(child.myName, result);
      return result;
    } else {
      NamespaceMember oldMember = myMembers.get(child.myName);
      if (oldMember != null) {
        return null;
      } else {
        NamespaceMember result = new NamespaceMember(child, null, null);
        myMembers.put(child.myName, result);
        return result;
      }
    }
  }

  public NamespaceMember getMember(String name) {
    return myMembers == null ? null : myMembers.get(name);
  }

  public Definition getDefinition(String name) {
    NamespaceMember member = getMember(name);
    return member == null ? null : member.definition;
  }

  public Abstract.Definition getAbstractDefinition(String name) {
    NamespaceMember member = getMember(name);
    return member == null ? null : member.abstractDefinition;
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

  public NamespaceMember addAbstractDefinition(Abstract.Definition definition) {
    if (myMembers == null) {
      myMembers = new HashMap<>();
    } else {
      NamespaceMember oldMember = myMembers.get(definition.getName().name);
      if (oldMember != null) {
        if (oldMember.abstractDefinition != null) {
          return null;
        } else {
          oldMember.abstractDefinition = definition;
          return oldMember;
        }
      }
    }

    NamespaceMember result = new NamespaceMember(new Namespace(definition.getName().name, this), definition, null);
    myMembers.put(definition.getName().name, result);
    return result;
  }

  public NamespaceMember addDefinition(Definition definition) {
    if (myMembers == null) {
      myMembers = new HashMap<>();
    } else {
      NamespaceMember oldMember = myMembers.get(definition.getName());
      if (oldMember != null) {
        if (oldMember.definition != null) {
          return null;
        } else {
          oldMember.definition = definition;
          return oldMember;
        }
      }
    }

    NamespaceMember result = new NamespaceMember(new Namespace(definition.getName(), this), null, definition);
    myMembers.put(definition.getName(), result);
    return result;
  }

  public NamespaceMember addMember(NamespaceMember member) {
    if (myMembers == null) {
      myMembers = new HashMap<>();
    } else {
      NamespaceMember oldMember = myMembers.get(member.namespace.getName());
      if (oldMember != null) {
        if (oldMember.definition != null || oldMember.abstractDefinition != null) {
          return oldMember;
        } else {
          oldMember.abstractDefinition = member.abstractDefinition;
          oldMember.definition = member.definition;
          return null;
        }
      }
    }

    myMembers.put(member.namespace.getName(), member);
    return null;
  }

  public NamespaceMember addMember(String name) {
    if (myMembers == null) {
      myMembers = new HashMap<>();
    } else {
      NamespaceMember oldMember = myMembers.get(name);
      if (oldMember != null) {
        return oldMember;
      }
    }

    NamespaceMember member = new NamespaceMember(new Namespace(name, this), null, null);
    myMembers.put(name, member);
    return member;
  }

  public NamespaceMember removeMember(NamespaceMember member) {
    return myMembers.remove(member.namespace.getName());
  }

  public void clear() {
    myMembers = null;
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
    return myName.equals(((Namespace) other).myName);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(new Object[]{ myParent, myName });
  }
}
