package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.module.ModuleID;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Definition;

import java.util.*;

public class Namespace {
  final private ResolvedName myResolvedName;
  private Map<String, NamespaceMember> myMembers;

  private Namespace(String name, Namespace parent) {
    myResolvedName = new DefinitionResolvedName(parent, name);
  }

  public Namespace(ModuleID moduleID) {
    myResolvedName = new ModuleResolvedName(moduleID);
  }

  public ResolvedName getResolvedName() {
    return myResolvedName;
  }

  public String getName() {
    return myResolvedName.getName();
  }

  public Namespace getParent() {
    ResolvedName parentName = myResolvedName.getParent();
    if (parentName == null) return null;
    return parentName.toNamespace();
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
      myMembers.put(child.getName(), result);
      return result;
    } else {
      NamespaceMember oldMember = myMembers.get(child.getName());
      if (oldMember != null) {
        return null;
      } else {
        NamespaceMember result = new NamespaceMember(child, null, null);
        myMembers.put(child.getName(), result);
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
      NamespaceMember oldMember = myMembers.get(definition.getName());
      if (oldMember != null) {
        if (oldMember.abstractDefinition != null) {
          return null;
        } else {
          oldMember.abstractDefinition = definition;
          return oldMember;
        }
      }
    }

    NamespaceMember result = new NamespaceMember(new Namespace(definition.getName(), this), definition, null);
    myMembers.put(definition.getName(), result);
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
    return myResolvedName.getFullName();
  }

  @Override
  public boolean equals(Object other) {
    return this == other || other instanceof NamespaceMember && ((NamespaceMember) other).getResolvedName().equals(myResolvedName);
  }

  @Override
  public int hashCode() {
    return myResolvedName.hashCode();
  }
}
