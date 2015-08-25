package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Namespace implements NamespaceMember {
  final private Utils.Name myName;
  private Namespace myParent;
  private Map<String, Namespace> myChildren;
  private Map<String, Definition> myMembers;

  public Namespace(Utils.Name name, Namespace parent) {
    myName = name;
    myParent = parent;
  }

  @Override
  public Utils.Name getName() {
    return myName;
  }

  public String getFullName() {
    return myParent == null || myParent.getParent() == null ? myName.name : myParent.getFullName() + "." + myName.name;
  }

  @Override
  public Namespace getParent() {
    return myParent;
  }

  public void setParent(Namespace parent) {
    myParent = parent;
  }

  public boolean isLocal() {
    return myParent == null ? myName == null : myParent.isLocal();
  }

  public Collection<Definition> getMembers() {
    return myMembers == null ? Collections.<Definition>emptyList() : myMembers.values();
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

  public Definition getMember(String name) {
    return myMembers == null ? null : myMembers.get(name);
  }

  public Definition addMember(Definition member) {
    if (myMembers == null) {
      myMembers = new HashMap<>();
      myMembers.put(member.getName().name, member);
      return null;
    } else {
      Definition oldMember = myMembers.get(member.getName().name);
      if (oldMember != null) {
        return oldMember;
      } else {
        myMembers.put(member.getName().name, member);
        return null;
      }
    }
  }

  public void removeMember(Definition member) {
    if (myMembers != null) {
      Definition removed = myMembers.remove(member.getName().name);
      if (removed != member) {
        myMembers.put(member.getName().name, removed);
      }
    }
  }

  public NamespaceMember addMember(NamespaceMember member) {
    if (member instanceof Namespace) {
      return addChild((Namespace) member);
    }
    if (member instanceof Definition) {
      return addMember((Definition) member);
    }
    throw new IllegalStateException();
  }

  public void removeMember(NamespaceMember member) {
    if (member instanceof Namespace) {
      removeChild(member.getName().name);
    } else
    if (member instanceof Definition) {
      removeMember((Definition) member);
    } else {
      throw new IllegalStateException();
    }
  }
}
