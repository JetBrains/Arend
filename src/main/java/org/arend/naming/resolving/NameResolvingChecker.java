package org.arend.naming.resolving;

import org.arend.error.Error;
import org.arend.naming.error.NamingError;
import org.arend.naming.reference.*;
import org.arend.naming.resolving.visitor.ExpressionResolveNameVisitor;
import org.arend.naming.scope.*;
import org.arend.term.NameRenaming;
import org.arend.term.NamespaceCommand;
import org.arend.term.group.Group;
import org.arend.typechecking.error.local.LocalError;
import org.arend.typechecking.typecheckable.provider.PartialConcreteProvider;
import org.arend.util.Pair;

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

  public void checkSuperClassOfSynonym(ClassReferable superClass, ClassReferable underlyingClass, Object cause) {
    superClass = superClass.getUnderlyingReference();
    if (superClass == null) {
      onError(new NamingError("Expected a class synonym", cause));
    } else if (underlyingClass != null) {
      if (!underlyingClass.isSubClassOf(superClass)) {
        onError(new NamingError("Expected a synonym of a superclass of '" + underlyingClass.textRepresentation() + "'", cause));
      }
    }
  }

  private void checkDefinition(LocatedReferable definition, Scope scope) {
    // Check classes
    if (definition instanceof ClassReferable) {
      ClassReferable classRef = (ClassReferable) definition;
      Reference underlyingClassRef = classRef.getUnresolvedUnderlyingReference();
      boolean isSynonym = underlyingClassRef != null;
      ClassReferable underlyingClass = null;
      if (isSynonym) {
        // Check the underlying class of a synonym
        underlyingClass = checkClass(underlyingClassRef, scope, true, true);
        if (underlyingClass != null && underlyingClass.isSynonym()) {
          onError(new NamingError("Expected a class, got a class synonym", underlyingClassRef.getData()));
          underlyingClass = null;
        }

        // Check field synonyms
        Collection<? extends LocatedReferable> fieldRefs = classRef.getFieldReferables();
        if (underlyingClass != null && !fieldRefs.isEmpty()) {
          Scope fieldsScope = new ClassFieldImplScope(underlyingClass, false);
          for (LocatedReferable fieldRef : fieldRefs) {
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
        if (isSynonym && resolvedRef != null) {
          checkSuperClassOfSynonym(resolvedRef, underlyingClass, superClassRef.getData());
        }
      }
    }

    // Check instances

    // The code is commented since we (currently) do not allow functions in types of instances
    // ClassReferable classRef = definition.getTypeClassReference();
    // if (classRef == null || myConcreteProvider.isRecord(classRef)) {
      Reference typeRef = myConcreteProvider.getInstanceTypeReference(definition);
      if (typeRef != null) {
        checkClass(typeRef, scope, true, false);
      }
    // }
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

  public static Map<String, Pair<LocatedReferable, ClassReferable>> collectClassFields(LocatedReferable referable) {
    Collection<? extends ClassReferable> superClasses = referable instanceof ClassReferable ? ((ClassReferable) referable).getSuperClassReferences() : Collections.emptyList();
    if (superClasses.isEmpty()) {
      return Collections.emptyMap();
    }

    Map<String, Pair<LocatedReferable, ClassReferable>> fields = new HashMap<>();
    Set<ClassReferable> visited = new HashSet<>();
    visited.add((ClassReferable) referable);
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

    return fields;
  }

  public void checkField(LocatedReferable field, Map<String, Pair<LocatedReferable, ClassReferable>> fields, LocatedReferable classRef) {
    if (field == null || fields.isEmpty()) {
      return;
    }

    String name = field.textRepresentation();
    if (!name.isEmpty() && !"_".equals(name)) {
      Pair<LocatedReferable, ClassReferable> oldField = fields.get(name);
      if (oldField != null) {
        onFieldNamesClash(oldField.proj1, oldField.proj2, field, (ClassReferable) classRef, (ClassReferable) classRef, Error.Level.WARNING);
      }
    }
  }

  public void checkGroup(Group group, Scope scope) {
    LocatedReferable groupRef = group.getReferable();
    Collection<? extends Group> subgroups = group.getSubgroups();
    Collection<? extends Group> dynamicSubgroups = group.getDynamicSubgroups();
    Collection<? extends NamespaceCommand> namespaceCommands = group.getNamespaceCommands();

    checkDefinition(groupRef, scope);

    Collection<? extends Group.InternalReferable> fields = group.getFields();
    if (!fields.isEmpty()) {
      Map<String, Pair<LocatedReferable, ClassReferable>> superFields = collectClassFields(groupRef);
      for (Group.InternalReferable internalRef : fields) {
        checkField(internalRef.getReferable(), superFields, groupRef);
      }
    }

    Map<String, LocatedReferable> referables = new HashMap<>();
    for (Group.InternalReferable internalRef : group.getConstructors()) {
      LocatedReferable ref = internalRef.getReferable();
      String name = ref.textRepresentation();
      if (!name.isEmpty() && !"_".equals(name)) {
        referables.putIfAbsent(name, ref);
      }
    }

    for (Group.InternalReferable internalRef : fields) {
      LocatedReferable ref = internalRef.getReferable();
      String name = ref.textRepresentation();
      if (!name.isEmpty() && !"_".equals(name)) {
        referables.putIfAbsent(name, ref);
      }
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

        if (isTopLevel) {
          checkUse(subgroup.getReferable());
        }
      }

      for (Group subgroup : dynamicSubgroups) {
        checkGroup(subgroup, makeScope(subgroup, scope));

        if (isTopLevel) {
          checkUse(subgroup.getReferable());
        }
      }
    }

    if (namespaceCommands.isEmpty()) {
      return;
    }

    for (NamespaceCommand cmd : namespaceCommands) {
      if (!isTopLevel && cmd.getKind() == NamespaceCommand.Kind.IMPORT) {
        onError(new NamingError("\\import is allowed only on the top level", cmd));
      } else {
        checkNamespaceCommand(cmd, referables.keySet());
      }
    }

    if (scope == null) {
      return;
    }

    List<Pair<NamespaceCommand, Set<String>>> namespaces = new ArrayList<>(namespaceCommands.size());
    for (NamespaceCommand cmd : namespaceCommands) {
      Collection<? extends Referable> elements = NamespaceCommandNamespace.resolveNamespace(cmd.getKind() == NamespaceCommand.Kind.IMPORT ? scope.getImportedSubscope() : scope, cmd).getElements();
      if (!elements.isEmpty()) {
        Set<String> names = new LinkedHashSet<>();
        for (Referable ref : elements) {
          names.add(ref.textRepresentation());
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

  public void checkNamespaceCommand(NamespaceCommand cmd, Set<String> defined) {
    if (defined == null) {
      return;
    }

    for (NameRenaming renaming : cmd.getOpenedReferences()) {
      String name = renaming.getName();
      if (name == null) {
        name = renaming.getOldReference().textRepresentation();
      }
      if (defined.contains(name)) {
        onError(new NamingError(Error.Level.WARNING, "Definition '" + name + "' is not imported since it is defined in this module", renaming));
      }
    }
  }

  private void checkUse(LocatedReferable ref) {
    if (myConcreteProvider.isUse(ref)) {
      onError(new NamingError("\\use is not allowed on the top level", ref));
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
      checkReference(oldRef, newRef, parentReferable);
    }
  }

  public boolean checkReference(LocatedReferable oldRef, LocatedReferable newRef, LocatedReferable parentReferable) {
    Error.Level level;
    if (parentReferable == null) {
      level = Error.Level.ERROR;
    } else {
      LocatedReferable oldParent = oldRef.getLocatedReferableParent();
      if (parentReferable.equals(oldParent) || oldParent != null && oldParent.equals(newRef.getLocatedReferableParent())) {
        return true;
      }
      level = Error.Level.WARNING;
    }

    onDefinitionNamesClash(oldRef, newRef, level);
    return false;
  }
}
