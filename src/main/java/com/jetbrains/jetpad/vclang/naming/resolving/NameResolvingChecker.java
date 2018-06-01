package com.jetbrains.jetpad.vclang.naming.resolving;

import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.naming.reference.ClassReferable;
import com.jetbrains.jetpad.vclang.naming.reference.LocatedReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.scope.EmptyScope;
import com.jetbrains.jetpad.vclang.naming.scope.LexicalScope;
import com.jetbrains.jetpad.vclang.naming.scope.MergeScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.NameRenaming;
import com.jetbrains.jetpad.vclang.term.NamespaceCommand;
import com.jetbrains.jetpad.vclang.term.group.Group;
import com.jetbrains.jetpad.vclang.util.Pair;

import java.util.*;

public abstract class NameResolvingChecker {
  private final boolean myRecursively;

  protected NameResolvingChecker(boolean recursively) {
    myRecursively = recursively;
  }

  public void definitionNamesClash(LocatedReferable ref1, LocatedReferable ref2, Error.Level level) {

  }

  public void fieldNamesClash(LocatedReferable ref1, ClassReferable superClass1, LocatedReferable ref2, ClassReferable superClass2, ClassReferable currentClass, Error.Level level) {

  }

  public void namespacesClash(NamespaceCommand cmd1, NamespaceCommand cmd2, String name, Error.Level level) {

  }

  public void namespaceDefinitionNameClash(NameRenaming renaming, LocatedReferable ref, Error.Level level) {

  }

  public void nonTopLevelImport(NamespaceCommand command) {

  }

  public static Scope makeScope(Group group, Scope parentScope) {
    if (parentScope == null) {
      return null;
    }

    if (group.getNamespaceCommands().isEmpty()) {
      return new MergeScope(LexicalScope.insideOf(group, EmptyScope.INSTANCE), parentScope);
    } else {
      return LexicalScope.insideOf(group, parentScope);
    }
  }

  public void checkGroup(Group group, Scope scope, boolean isTopLevel) {
    LocatedReferable groupRef = group.getReferable();
    Collection<? extends ClassReferable> superClasses = groupRef instanceof ClassReferable ? ((ClassReferable) groupRef).getSuperClassReferences() : Collections.emptyList();
    Collection<? extends Group> subgroups = group.getSubgroups();
    Collection<? extends Group> dynamicSubgroups = group.getDynamicSubgroups();
    Collection<? extends NamespaceCommand> namespaceCommands = group.getNamespaceCommands();

    Map<String, LocatedReferable> referables = new HashMap<>();
    Map<String, Pair<LocatedReferable, ClassReferable>> fields = Collections.emptyMap();

    if (!superClasses.isEmpty()) {
      fields = new HashMap<>();

      Set<ClassReferable> visited = new HashSet<>();
      visited.add((ClassReferable) groupRef);
      Deque<ClassReferable> toVisit = new ArrayDeque<>(superClasses);
      while (!toVisit.isEmpty()) {
        ClassReferable superClass = toVisit.pop();
        if (!visited.add(superClass)) {
          continue;
        }

        for (LocatedReferable fieldRef : superClass.getFieldReferables()) {
          String name = fieldRef.textRepresentation();
          if (!name.isEmpty() && !"_".equals(name)) {
            // Pair<LocatedReferable, ClassReferable> oldField =
              fields.putIfAbsent(name, new Pair<>(fieldRef, superClass));
            // if (oldField != null && !superClass.equals(oldField.proj2)) {
            //   fieldNamesClash(oldField.proj1, oldField.proj2, fieldRef, superClass, (ClassReferable) groupRef, Error.Level.ERROR);
            // }
          }
        }

        toVisit.addAll(superClass.getSuperClassReferences());
      }
    }

    for (Group.InternalReferable internalRef : group.getConstructors()) {
      checkReference(internalRef.getReferable(), referables, null);
    }

    for (Group.InternalReferable internalRef : group.getFields()) {
      LocatedReferable field = internalRef.getReferable();
      String name = field.textRepresentation();
      if (!name.isEmpty() && !"_".equals(name)) {
        Pair<LocatedReferable, ClassReferable> oldField = fields.get(name);
        if (oldField != null) {
          assert groupRef instanceof ClassReferable;
          fieldNamesClash(oldField.proj1, oldField.proj2, field, (ClassReferable) groupRef, (ClassReferable) groupRef, Error.Level.WARNING);
        }
      }

      checkReference(field, referables, null);
    }

    for (Group subgroup : subgroups) {
      checkReference(subgroup.getReferable(), referables, null);
    }

    for (Group subgroup : dynamicSubgroups) {
      checkReference(subgroup.getReferable(), referables, null);
    }

    checkSubgroup(dynamicSubgroups, referables, groupRef);

    checkSubgroup(subgroups, referables, groupRef);

    if (myRecursively) {
      for (Group subgroup : subgroups) {
        checkGroup(subgroup, makeScope(subgroup, scope), false);
      }

      for (Group subgroup : dynamicSubgroups) {
        checkGroup(subgroup, makeScope(subgroup, scope), false);
      }
    }

    if (namespaceCommands.isEmpty()) {
      return;
    }

    for (NamespaceCommand cmd : namespaceCommands) {
      if (!isTopLevel && cmd.getKind() == NamespaceCommand.Kind.IMPORT) {
        nonTopLevelImport(cmd);
      }

      for (NameRenaming renaming : cmd.getOpenedReferences()) {
        String name = renaming.getName();
        if (name == null) {
          name = renaming.getOldReference().textRepresentation();
        }
        LocatedReferable ref = referables.get(name);
        if (ref != null) {
          namespaceDefinitionNameClash(renaming, ref, Error.Level.WARNING);
        }
      }
    }

    if (scope == null) {
      return;
    }

    List<Pair<NamespaceCommand, Set<String>>> namespaces = new ArrayList<>(namespaceCommands.size());
    for (NamespaceCommand cmd : namespaceCommands) {
      Scope cmdNamespace = Scope.Utils.resolveNamespace(scope, cmd.getPath());
      if (cmdNamespace != null) {
        Set<String> names = new LinkedHashSet<>();
        for (Referable ref : cmdNamespace.getElements()) {
          names.add(ref.textRepresentation());
        }
        for (Referable ref : cmd.getHiddenReferences()) {
          names.remove(ref.textRepresentation());
        }
        namespaces.add(new Pair<>(cmd, names));
      }
    }

    for (int i = 0; i < namespaces.size(); i++) {
      Pair<NamespaceCommand, Set<String>> pair = namespaces.get(i);
      for (String name : pair.proj2) {
        if (referables.containsKey(name)) {
          continue;
        }

        for (int j = i + 1; j < namespaces.size(); j++) {
          if (namespaces.get(j).proj2.contains(name)) {
            namespacesClash(pair.proj1, namespaces.get(j).proj1, name, Error.Level.WARNING);
          }
        }
      }
    }
  }

  private void checkSubgroup(Collection<? extends Group> subgroups, Map<String, LocatedReferable> referables, LocatedReferable parentReferable) {
    for (Group subgroup : subgroups) {
      for (Group.InternalReferable internalReferable : subgroup.getFields()) {
        if (internalReferable.isVisible()) {
          checkReference(internalReferable.getReferable(), referables, parentReferable);
        }
      }
      for (Group.InternalReferable internalReferable : subgroup.getConstructors()) {
        if (internalReferable.isVisible()) {
          checkReference(internalReferable.getReferable(), referables, parentReferable);
        }
      }
    }
  }

  private void checkReference(LocatedReferable newRef, Map<String, LocatedReferable> referables, LocatedReferable parentReferable) {
    String name = newRef.textRepresentation();
    if (name.isEmpty() || "_".equals(name)) {
      return;
    }

    LocatedReferable oldRef = referables.putIfAbsent(name, newRef);
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
      definitionNamesClash(oldRef, newRef, level);
    }
  }
}
