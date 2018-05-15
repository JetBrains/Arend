package com.jetbrains.jetpad.vclang.naming.resolving;

import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.naming.reference.LocatedReferable;
import com.jetbrains.jetpad.vclang.term.group.Group;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class DuplicateNameChecker {
  public void duplicateName(LocatedReferable ref1, LocatedReferable ref2, Error.Level level) {

  }

  public boolean checkGroup(Group group) {
    Collection<? extends Group> subgroups = group.getSubgroups();
    Collection<? extends Group> dynamicSubgroups = group.getDynamicSubgroups();
    Collection<? extends Group.InternalReferable> fields = group.getFields();
    Collection<? extends Group.InternalReferable> constructors = group.getConstructors();
    if (subgroups.isEmpty() && dynamicSubgroups.isEmpty() && fields.isEmpty() && constructors.isEmpty()) {
      return true;
    }

    Map<String, LocatedReferable> referables = new HashMap<>();

    for (Group.InternalReferable internalRef : constructors) {
      checkReference(internalRef.getReferable(), referables, null);
    }

    for (Group.InternalReferable internalRef : fields) {
      checkReference(internalRef.getReferable(), referables, null);
    }

    for (Group subgroup : subgroups) {
      checkReference(subgroup.getReferable(), referables, null);
    }

    for (Group subgroup : dynamicSubgroups) {
      checkReference(subgroup.getReferable(), referables, null);
    }

    checkSubgroup(dynamicSubgroups, referables, group.getReferable());

    checkSubgroup(subgroups, referables, group.getReferable());

    return true;
  }

  private void checkSubgroup(Collection<? extends Group> subgroups, Map<String, LocatedReferable> referables, LocatedReferable parentReferable) {
    for (Group subgroup : subgroups) {
      for (Group.InternalReferable internalReferable : subgroup.getFields()) {
        checkReference(internalReferable.getReferable(), referables, parentReferable);
      }
      for (Group.InternalReferable internalReferable : subgroup.getConstructors()) {
        checkReference(internalReferable.getReferable(), referables, parentReferable);
      }
    }
  }

  private void checkReference(LocatedReferable newRef, Map<String, LocatedReferable> referables, LocatedReferable parentReferable) {
    LocatedReferable oldRef = referables.putIfAbsent(newRef.textRepresentation(), newRef);
    if (oldRef != null) {
      Error.Level level;
      if (parentReferable == null) {
        level = Error.Level.ERROR;
      } else {
        LocatedReferable oldParent = oldRef.getLocatedReferableParent();
        if (parentReferable.equals(oldParent) || oldParent != null && oldParent.equals(newRef.getLocatedReferableParent())) {
          return;
        }
        level = Error.Level.WARNING;
      }
      duplicateName(oldRef, newRef, level);
    }
  }
}
