package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.term.NamespaceCommand;

import java.util.function.Predicate;

public class LexicalScope implements Scope {
  private final Scope myParent;
  private final Group myGroup;
  private final NamespaceCommand myCommand;

  public LexicalScope(Scope parent, Group group) {
    myParent = parent;
    myGroup = group;
    myCommand = null;
  }

  public LexicalScope(Scope parent, Group group, NamespaceCommand cmd) {
    myParent = parent;
    myGroup = group;
    myCommand = cmd;
  }

  @Override
  public Referable find(Predicate<Referable> pred) {
    for (Group subgroup : myGroup.getSubgroups()) {
      Referable ref = testSubgroup(subgroup, pred);
      if (ref != null) {
        return ref;
      }
    }
    for (Group subgroup : myGroup.getDynamicSubgroups()) {
      Referable ref = testSubgroup(subgroup, pred);
      if (ref != null) {
        return ref;
      }
    }

    /* TODO[abstract]
    for (NamespaceCommand cmd : myGroup.getNamespaceCommands()) {
      if (myCommand == cmd) {
        break;
      }

      Referable referable = cmd.getGroupReference();
      if (referable instanceof UnresolvedReference) {
        referable = ((UnresolvedReference) referable).resolve(new LexicalScope(myParent, myGroup, cmd), null);
      }
      if (referable instanceof GlobalReferable) {
        Collection<? extends Referable> refs = cmd.getSubgroupReferences();
      }
    }
    */

    return myParent.find(pred);
  }

  private static Referable testSubgroup(Group group, Predicate<Referable> pred) {
    for (GlobalReferable referable : group.getConstructors()) {
      if (pred.test(referable)) {
        return referable;
      }
    }

    for (GlobalReferable referable : group.getFields()) {
      if (pred.test(referable)) {
        return referable;
      }
    }

    Referable ref = group.getReferable();
    if (pred.test(ref)) {
      return ref;
    }

    return null;
  }
}
