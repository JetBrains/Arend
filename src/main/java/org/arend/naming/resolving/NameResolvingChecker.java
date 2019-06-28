package org.arend.naming.resolving;

import org.arend.error.Error;
import org.arend.naming.error.NamingError;
import org.arend.naming.reference.*;
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

  private void checkDefinition(LocatedReferable definition, Scope scope) {
    // Check classes
    if (definition instanceof ClassReferable) {
      for (Reference superClassRef : ((ClassReferable) definition).getUnresolvedSuperClassReferences()) {
        checkClass(superClassRef, scope, false);
      }
    }

    // Check instances

    // The code is commented since we (currently) do not allow functions in types of instances
    // ClassReferable classRef = definition.getTypeClassReference();
    // if (classRef == null || myConcreteProvider.isRecord(classRef)) {
      Reference typeRef = myConcreteProvider.getInstanceTypeReference(definition);
      if (typeRef != null) {
        checkClass(typeRef, scope, true);
      }
    // }
  }

  private void checkClass(Reference classRef, Scope scope, boolean checkNotRecord) {
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

    if (!(ref instanceof ErrorReference) && !(ok && ref instanceof ClassReferable && (!checkNotRecord || !((ClassReferable) ref).isRecord()))) {
      onError(new NamingError(ok && ref instanceof ClassReferable  ? "Expected a class, got a record" : "Expected a class", classRef.getData()));
    }
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
    for (Group.InternalReferable internalRef : group.getInternalReferables()) {
      LocatedReferable ref = internalRef.getReferable();
      String name = ref.textRepresentation();
      if (!name.isEmpty() && !"_".equals(name)) {
        referables.putIfAbsent(name, ref);
      }
    }

    for (Group subgroup : subgroups) {
      checkReference(subgroup.getReferable(), referables, Error.Level.ERROR);
    }

    for (Group subgroup : dynamicSubgroups) {
      checkReference(subgroup.getReferable(), referables, Error.Level.ERROR);
    }

    checkSubgroups(dynamicSubgroups, referables);

    checkSubgroups(subgroups, referables);

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

    List<Pair<NamespaceCommand, Map<String, Referable>>> namespaces = new ArrayList<>(namespaceCommands.size());
    for (NamespaceCommand cmd : namespaceCommands) {
      Collection<? extends Referable> elements = NamespaceCommandNamespace.resolveNamespace(cmd.getKind() == NamespaceCommand.Kind.IMPORT ? scope.getImportedSubscope() : scope, cmd).getElements();
      if (!elements.isEmpty()) {
        Map<String, Referable> map = new LinkedHashMap<>();
        for (Referable element : elements) {
          map.put(element.textRepresentation(), element);
        }
        namespaces.add(new Pair<>(cmd, map));
      }
    }

    for (int i = 0; i < namespaces.size(); i++) {
      Pair<NamespaceCommand, Map<String, Referable>> pair = namespaces.get(i);
      for (Map.Entry<String, Referable> entry : pair.proj2.entrySet()) {
        if (referables.containsKey(entry.getKey())) {
          continue;
        }

        for (int j = i + 1; j < namespaces.size(); j++) {
          Referable ref = namespaces.get(j).proj2.get(entry.getKey());
          if (ref != null && !ref.equals(entry.getValue())) {
            onNamespacesClash(pair.proj1, namespaces.get(j).proj1, ref.textRepresentation(), Error.Level.WARNING);
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

  private void checkSubgroups(Collection<? extends Group> subgroups, Map<String, LocatedReferable> referables) {
    for (Group subgroup : subgroups) {
      for (Group.InternalReferable internalReferable : subgroup.getInternalReferables()) {
        if (internalReferable.isVisible()) {
          checkReference(internalReferable.getReferable(), referables, Error.Level.WARNING);
        }
      }
    }
  }

  private void checkReference(LocatedReferable newRef, Map<String, LocatedReferable> referables, Error.Level level) {
    String name = newRef.textRepresentation();
    if (name.isEmpty() || "_".equals(name)) {
      return;
    }

    LocatedReferable oldRef = referables.putIfAbsent(name, newRef);
    if (oldRef != null) {
      onDefinitionNamesClash(oldRef, newRef, level);
    }
  }
}
