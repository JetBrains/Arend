package org.arend.naming.reference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public interface ClassReferable extends LocatedReferable {
  @Nonnull Collection<? extends ClassReferable> getSuperClassReferences();
  @Nonnull Collection<? extends Reference> getUnresolvedSuperClassReferences();
  @Nonnull Collection<? extends FieldReferable> getFieldReferables();
  @Nonnull Collection<? extends Referable> getImplementedFields();
  @Override @Nullable ClassReferable getUnderlyingReference();

  default @Override @Nullable ClassReferable getUnderlyingTypecheckable() {
    ClassReferable underlyingRef = getUnderlyingReference();
    return underlyingRef == null ? this : underlyingRef.isSynonym() ? null : underlyingRef;
  }

  default boolean isFieldSynonym() {
    return false;
  }

  default boolean isRenamed(FieldReferable fieldRef) {
    Set<ClassReferable> visitedClasses = new HashSet<>();
    Deque<ClassReferable> toVisit = new ArrayDeque<>();
    toVisit.add(this);
    while (!toVisit.isEmpty()) {
      ClassReferable classRef = toVisit.pop();
      if (!visitedClasses.add(classRef)) {
        continue;
      }
      for (FieldReferable classFieldRef : classRef.getFieldReferables()) {
        if (classFieldRef.getUnderlyingReference() == fieldRef) {
          return true;
        }
      }
      toVisit.addAll(classRef.getSuperClassReferences());
    }
    return false;
  }

  default boolean isSubClassOf(ClassReferable classRef) {
    if (this == classRef) {
      return true;
    }

    Set<ClassReferable> visitedClasses = new HashSet<>();
    Deque<ClassReferable> toVisit = new ArrayDeque<>();
    toVisit.add(this);
    while (!toVisit.isEmpty()) {
      ClassReferable ref = toVisit.pop();
      if (classRef == ref) {
        return true;
      }
      if (visitedClasses.add(ref)) {
        toVisit.addAll(ref.getSuperClassReferences());
      }
    }

    return false;
  }

  class Helper {
    public static Set<FieldReferable> getNotImplementedFields(ClassReferable classDef) {
      Set<FieldReferable> result = getAllFields(classDef, new HashSet<>(), new HashMap<>());
      removeImplemented(classDef, result, null);
      return result;
    }

    public static HashMap<FieldReferable, List<LocatedReferable>> getNotImplementedFields(ClassReferable classDef, List<Boolean> argumentsExplicitness, HashMap<ClassReferable, Set<FieldReferable>> superClassesFields) {
      Set<FieldReferable> fieldSet = getAllFields(classDef, new HashSet<>(), superClassesFields);
      removeImplemented(classDef, fieldSet, superClassesFields);
      if (!argumentsExplicitness.isEmpty()) {
        Iterator<FieldReferable> it = fieldSet.iterator();
        int i = 0;
        while (it.hasNext() && i < argumentsExplicitness.size()) {
          FieldReferable field = it.next();
          boolean isExplicit = field.isExplicitField();
          if (isExplicit) {
            while (i < argumentsExplicitness.size() && !argumentsExplicitness.get(i)) {
              i++;
            }
            if (i == argumentsExplicitness.size()) {
              break;
            }
          }

          for (Set<FieldReferable> fields : superClassesFields.values()) {
            fields.remove(field);
          }
          it.remove();

          if (isExplicit == argumentsExplicitness.get(i)) {
            i++;
          }
        }
      }

      Map<LocatedReferable, List<LocatedReferable>> renamings = getRenamings(classDef);
      HashMap<FieldReferable, List<LocatedReferable>> result = new LinkedHashMap<>();
      for (FieldReferable field : fieldSet) {
        List<LocatedReferable> renamedFields = renamings.get(field);
        result.put(field, renamedFields != null ? renamedFields : Collections.singletonList(field));
      }
      return result;
    }

    private static void removeImplemented(ClassReferable classDef, Set<FieldReferable> result, HashMap<ClassReferable, Set<FieldReferable>> superClassesFields) {
      Deque<ClassReferable> toVisit = new ArrayDeque<>();
      Set<ClassReferable> visitedClasses = new HashSet<>();
      toVisit.add(classDef);

      while (!toVisit.isEmpty()) {
        ClassReferable classRef = toVisit.pop().getUnderlyingTypecheckable();
        if (classRef == null || !visitedClasses.add(classRef)) {
          continue;
        }

        for (Referable fieldImpl : classRef.getImplementedFields()) {
          if (fieldImpl instanceof ClassReferable) {
            Set<FieldReferable> superClassFields = superClassesFields == null ? null : superClassesFields.remove(fieldImpl);
            if (superClassFields != null) {
              result.removeAll(superClassFields);
              for (Set<FieldReferable> fields : superClassesFields.values()) {
                fields.removeAll(superClassFields);
              }
            }
          } else if (fieldImpl instanceof LocatedReferable) {
            LocatedReferable field = ((LocatedReferable) fieldImpl).getUnderlyingTypecheckable();
            if (field instanceof FieldReferable) {
              result.remove(field);
              if (superClassesFields != null) {
                for (Set<FieldReferable> fields : superClassesFields.values()) {
                  fields.remove(field);
                }
              }
            }
          }
        }

        toVisit.addAll(classRef.getSuperClassReferences());
      }
    }

    private static Set<FieldReferable> getAllFields(ClassReferable classDef, Set<ClassReferable> visited, HashMap<ClassReferable, Set<FieldReferable>> superClassesFields) {
      if (!visited.add(classDef)) {
        return new LinkedHashSet<>();
      }

      Set<FieldReferable> result = new LinkedHashSet<>();
      ClassReferable underlyingClass = classDef.getUnderlyingReference();
      if (underlyingClass != null && !underlyingClass.isSynonym()) {
        result.addAll(getAllFields(underlyingClass, visited, superClassesFields));
      }

      for (ClassReferable superClass : classDef.getSuperClassReferences()) {
        Set<FieldReferable> superClassSet = getAllFields(superClass, visited, superClassesFields);
        superClassesFields.compute(superClass, (k,oldFields) -> {
          if (oldFields == null) {
            return superClassSet;
          } else {
            oldFields.retainAll(superClassSet);
            return oldFields;
          }
        });
        if (underlyingClass == null) {
          result.addAll(superClassSet);
        }
      }

      if (underlyingClass == null) {
        result.addAll(classDef.getFieldReferables());
      }

      return result;
    }

    public static Map<LocatedReferable, List<LocatedReferable>> getRenamings(ClassReferable classDef) {
      Deque<ClassReferable> toVisit = new ArrayDeque<>();
      Set<ClassReferable> visitedClasses = new HashSet<>();
      Map<LocatedReferable, List<LocatedReferable>> renamings = new HashMap<>();
      toVisit.add(classDef);

      while (!toVisit.isEmpty()) {
        ClassReferable classRef = toVisit.pop();
        if (!visitedClasses.add(classRef)) {
          continue;
        }

        for (FieldReferable field : classRef.getFieldReferables()) {
          LocatedReferable underlyingField = field.getUnderlyingReference();
          if (underlyingField != null) {
            renamings.computeIfAbsent(underlyingField, k -> new ArrayList<>(1)).add(field);
          }
        }

        toVisit.addAll(classRef.getSuperClassReferences());
      }

      return renamings;
    }
  }
}
