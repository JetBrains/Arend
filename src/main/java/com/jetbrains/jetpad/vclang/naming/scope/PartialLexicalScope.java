package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.term.NamespaceCommand;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.function.Predicate;

public class PartialLexicalScope implements Scope {
  protected final Scope parent;
  private final Group myGroup;
  private final NamespaceCommand myCommand;

  public PartialLexicalScope(Scope parent, Group group) {
    this.parent = parent;
    myGroup = group;
    myCommand = null;
  }

  public PartialLexicalScope(Scope parent, Group group, NamespaceCommand cmd) {
    this.parent = parent;
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
        referable = ((UnresolvedReference) referable).resolve(new LexicalScope(parent, myGroup, cmd), null);
      }
      if (referable instanceof GlobalReferable) {
        Collection<? extends Referable> refs = cmd.getSubgroupReferences();
      }
    }
    */

    return null;
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

  @Nullable
  @Override
  public Scope resolveNamespace(String name) {
    for (Group subgroup : myGroup.getSubgroups()) {
      if (subgroup.getReferable().textRepresentation().equals(name)) {
        return new PartialLexicalScope(EmptyScope.INSTANCE, subgroup);
      }
    }
    for (Group subgroup : myGroup.getDynamicSubgroups()) {
      if (subgroup.getReferable().textRepresentation().equals(name)) {
        return new PartialLexicalScope(EmptyScope.INSTANCE, subgroup);
      }
    }

    return null;
  }

  public static Scope forNamespaceCommand(String name, Collection<? extends String> path, Collection<? extends String> subgroups, boolean isHiding) {
    return EmptyScope.INSTANCE;
  }
}
