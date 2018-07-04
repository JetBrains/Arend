package com.jetbrains.jetpad.vclang.naming.resolving;

import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.naming.error.NamingError;
import com.jetbrains.jetpad.vclang.naming.reference.*;
import com.jetbrains.jetpad.vclang.naming.resolving.visitor.ExpressionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.naming.scope.*;
import com.jetbrains.jetpad.vclang.term.NameRenaming;
import com.jetbrains.jetpad.vclang.term.NamespaceCommand;
import com.jetbrains.jetpad.vclang.term.group.Group;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalError;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider.PartialConcreteProvider;
import com.jetbrains.jetpad.vclang.util.Pair;

import java.util.*;

@SuppressWarnings("SameParameterValue")
public abstract class NameResolvingChecker {
  private final boolean myRecursively;
  private boolean myTopLevel;
  private final PartialConcreteProvider myConcreteProvider;

  protected NameResolvingChecker(boolean recursively, boolean isTopLevel, PartialConcreteProvider concreteProvider) {
    myRecursively = recursively;
    myTopLevel = isTopLevel;
    myConcreteProvider = concreteProvider;
  }

  protected void onDefinitionNamesClash(LocatedReferable ref1, LocatedReferable ref2, Error.Level level) {

  }

  protected void onFieldNamesClash(LocatedReferable ref1, ClassReferable superClass1, LocatedReferable ref2, ClassReferable superClass2, ClassReferable currentClass, Error.Level level) {

  }

  protected void onNamespacesClash(NamespaceCommand cmd1, NamespaceCommand cmd2, String name, Error.Level level) {

  }

  protected void onError(LocalError error) {

  }

  protected void checkDefinition(LocatedReferable definition, Scope scope) {
    // Check classes
    if (definition instanceof ClassReferable) {
      ClassReferable classRef = (ClassReferable) definition;
      Reference underlyingClassRef = classRef.getUnresolvedUnderlyingReference();
      boolean isSynonym = underlyingClassRef != null;
      ClassReferable underlyingClass = null;
      if (isSynonym) {
        // Check the underlying class of a synonym
        underlyingClass = checkClass(underlyingClassRef, scope, true, true);
        if (underlyingClass != null && underlyingClass.getUnresolvedUnderlyingReference() != null) {
          onError(new NamingError("Expected a class, got a class synonym", underlyingClassRef.getData()));
          underlyingClass = null;
        }

        // Check field synonyms
        Collection<? extends LocatedReferable> fieldRefs = classRef.getFieldReferables();
        if (underlyingClass != null && !fieldRefs.isEmpty()) {
          Scope fieldsScope = new ClassFieldImplScope(underlyingClass, false);
          for (LocatedReferable fieldRef : classRef.getFieldReferables()) {
            Reference underlyingFieldRef = fieldRef.getUnresolvedUnderlyingReference();
            if (underlyingFieldRef != null) {
              Referable ref = ExpressionResolveNameVisitor.resolve(underlyingFieldRef.getReferent(), fieldsScope, true);
              if (ref instanceof ErrorReference) {
                onError(((ErrorReference) ref).getError());
              }
            }
          }
        }
      }

      // Check super classes
      for (Reference superClassRef : classRef.getUnresolvedSuperClassReferences()) {
        ClassReferable resolvedRef = checkClass(superClassRef, scope, false, isSynonym);
        // Check super classes of a synonym
        if (isSynonym && resolvedRef != null) {
          resolvedRef = resolvedRef.getUnderlyingReference();
          if (resolvedRef == null) {
            onError(new NamingError("Expected a class synonym", superClassRef.getData()));
          } else if (underlyingClass != null) {
            if (!isSubClassOf(underlyingClass, resolvedRef)) {
              onError(new NamingError("Expected a synonym of a superclass of '" + underlyingClass + "'", superClassRef.getData()));
            }
          }
        }
      }
    }

    // Check instances
    Reference classRef = myConcreteProvider.getInstanceClassReference(definition);
    if (classRef != null) {
      checkClass(classRef, scope, true, false);
    }

    Collection<PartialConcreteProvider.InstanceParameter> parameters = myConcreteProvider.getInstanceParameterReferences(definition);
    if (parameters != null) {
      for (PartialConcreteProvider.InstanceParameter parameter : parameters) {
        if (parameter.isExplicit) {
          onError(new NamingError("Instances can have only implicit parameters", parameter.data));
        } else if (parameter.referable == null) {
          onError(new NamingError("Expected a class", parameter.data));
        }
        if (parameter.referable != null) {
          boolean isRecord = parameter.referable instanceof ClassReferable && myConcreteProvider.isRecord((ClassReferable) parameter.referable);
          if (!(parameter.referable instanceof ClassReferable) || isRecord) {
            onError(new NamingError(isRecord ? "Expected a class, got a record" : "Expected a class", parameter.data));
          }
        }
      }
    }
  }

  private boolean isSubClassOf(ClassReferable subClass, ClassReferable superClass) {
    if (subClass == superClass) {
      return true;
    }

    Set<ClassReferable> visitedClasses = new HashSet<>();
    Deque<ClassReferable> toVisit = new ArrayDeque<>();
    toVisit.add(subClass);
    while (!toVisit.isEmpty()) {
      ClassReferable classRef = toVisit.pop();
      if (classRef == superClass) {
        return true;
      }
      if (visitedClasses.add(classRef)) {
        toVisit.addAll(classRef.getSuperClassReferences());
      }
    }

    return false;
  }

  private ClassReferable checkClass(Reference classRef, Scope scope, boolean checkNotRecord, boolean reportUnresolved) {
    boolean ok = true;
    Referable ref = classRef.getReferent();
    while (ref instanceof RedirectingReferable) {
      ref = ((RedirectingReferable) ref).getOriginalReferable();
    }
    if (ref instanceof UnresolvedReference) {
      if (((UnresolvedReference) ref).resolveArgument(scope) != null) {
        ok = false;
      }
      ref = ((UnresolvedReference) ref).resolve(scope);
      while (ref instanceof RedirectingReferable) {
        ref = ((RedirectingReferable) ref).getOriginalReferable();
      }
    }

    if (ref instanceof ErrorReference) {
      if (reportUnresolved) {
        onError(((ErrorReference) ref).getError());
      }
    } else if (!(ok && ref instanceof ClassReferable && (!checkNotRecord || !myConcreteProvider.isRecord((ClassReferable) ref)))) {
      onError(new NamingError(ok && ref instanceof ClassReferable  ? "Expected a class, got a record" : "Expected a class", classRef.getData()));
    }

    return ref instanceof ClassReferable ? (ClassReferable) ref : null;
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

  public void checkGroup(Group group, Scope scope) {
    LocatedReferable groupRef = group.getReferable();
    Collection<? extends ClassReferable> superClasses = groupRef instanceof ClassReferable ? ((ClassReferable) groupRef).getSuperClassReferences() : Collections.emptyList();
    Collection<? extends Group> subgroups = group.getSubgroups();
    Collection<? extends Group> dynamicSubgroups = group.getDynamicSubgroups();
    Collection<? extends NamespaceCommand> namespaceCommands = group.getNamespaceCommands();

    checkDefinition(groupRef, scope);

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
          onFieldNamesClash(oldField.proj1, oldField.proj2, field, (ClassReferable) groupRef, (ClassReferable) groupRef, Error.Level.WARNING);
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

    boolean isTopLevel = myTopLevel;
    if (myRecursively) {
      myTopLevel = false;
      for (Group subgroup : subgroups) {
        checkGroup(subgroup, makeScope(subgroup, scope));
      }

      for (Group subgroup : dynamicSubgroups) {
        checkGroup(subgroup, makeScope(subgroup, scope));
      }
    }

    if (namespaceCommands.isEmpty()) {
      return;
    }

    for (NamespaceCommand cmd : namespaceCommands) {
      if (!isTopLevel && cmd.getKind() == NamespaceCommand.Kind.IMPORT) {
        onError(new NamingError("\\import is allowed only on the top level", cmd));
      }

      for (NameRenaming renaming : cmd.getOpenedReferences()) {
        String name = renaming.getName();
        if (name == null) {
          name = renaming.getOldReference().textRepresentation();
        }
        LocatedReferable ref = referables.get(name);
        if (ref != null) {
          onError(new NamingError(Error.Level.WARNING, "Definition '" + ref.textRepresentation() + "' is not imported since it is defined in this module", renaming));
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
            onNamespacesClash(pair.proj1, namespaces.get(j).proj1, name, Error.Level.WARNING);
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
      onDefinitionNamesClash(oldRef, newRef, level);
    }
  }
}
