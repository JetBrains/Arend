package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.UnresolvedReference;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.term.NameRenaming;
import com.jetbrains.jetpad.vclang.term.NamespaceCommand;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class LexicalScope implements Scope {
  private final Scope myParent;
  private final Group myGroup;
  private boolean myIgnoreExports;
  private final NamespaceCommand myCommand;

  private LexicalScope(Scope parent, Group group, boolean ignoreExports, NamespaceCommand cmd) {
    myParent = parent;
    myGroup = group;
    myCommand = cmd;
    myIgnoreExports = ignoreExports;
  }

  public static LexicalScope insideOf(Group group, Scope parent) {
    return new LexicalScope(parent, group, true, null);
  }

  public static LexicalScope opened(Group group) {
    return new LexicalScope(null, group, false, null);
  }

  public static LexicalScope exported(Group group) {
    return new LexicalScope(null, group, true, null);
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

    if (myParent == null && myIgnoreExports) {
      return null;
    }

    for (NamespaceCommand cmd : myGroup.getNamespaceCommands()) {
      if (myCommand == cmd) {
        break;
      }
      NamespaceCommand.Kind kind = cmd.getKind();
      if (myIgnoreExports && kind == NamespaceCommand.Kind.EXPORT || myParent == null && kind != NamespaceCommand.Kind.EXPORT) {
        continue;
      }

      boolean isUsing = cmd.isUsing();
      Collection<? extends NameRenaming> opened = cmd.getOpenedReferences();
      Collection<? extends Referable> hidden = cmd.getHiddenReferences();
      List<? extends String> path = cmd.getPath();
      Scope scope = Scope.Utils.resolveNamespace(new LexicalScope(myParent, myGroup, myIgnoreExports, cmd), path);
      if (scope == null || opened.isEmpty() && !isUsing) {
        continue;
      }

      if (opened.isEmpty() && hidden.isEmpty()) {
        Referable ref = scope.find(pred);
        if (ref != null) {
          return ref;
        }
      } else {
        testLoop:
        for (Referable testRef : scope.getElements()) {
          for (Referable hiddenRef : hidden) {
            if (hiddenRef instanceof UnresolvedReference && testRef.textRepresentation().equals(hiddenRef.textRepresentation()) || !(hiddenRef instanceof UnresolvedReference) && testRef.equals(hiddenRef)) {
              continue testLoop;
            }
          }

          boolean isOpened = isUsing && !(testRef instanceof GlobalReferable && ((GlobalReferable) testRef).isModule());
          if (!isOpened) {
            for (NameRenaming renaming : opened) {
              Referable openedRef = renaming.getOldReference();
              if (openedRef instanceof UnresolvedReference && testRef.textRepresentation().equals(openedRef.textRepresentation()) || !(openedRef instanceof UnresolvedReference) && testRef.equals(openedRef)) {
                GlobalReferable newRef = renaming.getNewReferable();
                if (newRef != null) {
                  if (pred.test(newRef)) {
                    return newRef;
                  } else {
                    continue testLoop;
                  }
                }

                isOpened = true;
                break;
              }
            }
          }

          if (isOpened && pred.test(testRef)) {
            return testRef;
          }
        }
      }
    }

    return myParent == null ? null : myParent.find(pred);
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
        return LexicalScope.opened(subgroup);
      }
    }
    for (Group subgroup : myGroup.getDynamicSubgroups()) {
      if (subgroup.getReferable().textRepresentation().equals(name)) {
        return LexicalScope.opened(subgroup);
      }
    }

    return null;
  }
}
