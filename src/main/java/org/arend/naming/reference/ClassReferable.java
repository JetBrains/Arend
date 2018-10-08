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

  default @Nonnull ClassReferable getUnderlyingTypecheckable() {
    ClassReferable underlyingRef = getUnderlyingReference();
    return underlyingRef != null ? underlyingRef : this;
  }

  @Override
  default boolean isFieldSynonym() {
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
      Set<FieldReferable> result = getAllFields(classDef, new HashSet<>(), new HashMap<>()).keySet();
      removeImplemented(classDef, result, null);
      return result;
    }

    public static HashMap<FieldReferable, List<LocatedReferable>> getNotImplementedFields(ClassReferable classDef, List<Boolean> argumentsExplicitness, HashMap<ClassReferable, Set<FieldReferable>> superClassesFields) {
      HashMap<FieldReferable, List<LocatedReferable>> result = getAllFields(classDef, new HashSet<>(), superClassesFields);
      removeImplemented(classDef, result.keySet(), superClassesFields);
      if (!argumentsExplicitness.isEmpty()) {
        Iterator<FieldReferable> it = result.keySet().iterator();
        int i = 0;
        while (it.hasNext() && i < argumentsExplicitness.size()) {
          FieldReferable field = it.next();
          boolean isExplicit = field.isExplicitField();
          if (isExplicit) {
            while (!argumentsExplicitness.get(i) && i < argumentsExplicitness.size()) {
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
      return result;
    }

    private static void removeImplemented(ClassReferable classDef, Set<FieldReferable> result, HashMap<ClassReferable, Set<FieldReferable>> superClassesFields) {
      Deque<ClassReferable> toVisit = new ArrayDeque<>();
      Set<ClassReferable> visitedClasses = new HashSet<>();
      toVisit.add(classDef);

      while (!toVisit.isEmpty()) {
        ClassReferable classRef = toVisit.pop();
        ClassReferable underlyingClass = classRef.getUnderlyingReference();
        if (underlyingClass != null) {
          classRef = underlyingClass;
        }
        if (!visitedClasses.add(classRef)) {
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
            LocatedReferable field = ((LocatedReferable) fieldImpl).getUnderlyingReference();
            if (field == null) {
              field = (LocatedReferable) fieldImpl;
            }
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

    private static HashMap<FieldReferable, List<LocatedReferable>> getAllFields(ClassReferable classDef, Set<ClassReferable> visited, HashMap<ClassReferable, Set<FieldReferable>> superClassesFields) {
      if (!visited.add(classDef)) {
        return new HashMap<>();
      }

      HashMap<FieldReferable, List<LocatedReferable>> result = new LinkedHashMap<>();
      ClassReferable underlyingClass = classDef.getUnderlyingReference();
      if (underlyingClass != null) {
        HashMap<FieldReferable, List<LocatedReferable>> underlyingClassResult = getAllFields(underlyingClass, visited, superClassesFields);
        for (Map.Entry<FieldReferable, List<LocatedReferable>> entry : underlyingClassResult.entrySet()) {
          result.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).addAll(entry.getValue());
        }
      }

      for (ClassReferable superClass : classDef.getSuperClassReferences()) {
        HashMap<FieldReferable, List<LocatedReferable>> superClassMap = getAllFields(superClass, visited, superClassesFields);
        superClassesFields.compute(superClass, (k,oldFields) -> {
          if (oldFields == null) {
            return superClassMap.keySet();
          } else {
            oldFields.retainAll(superClassMap.keySet());
            return oldFields;
          }
        });
        for (Map.Entry<FieldReferable, List<LocatedReferable>> entry : superClassMap.entrySet()) {
          if (underlyingClass == null) {
            result.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).addAll(entry.getValue());
          } else {
            List<LocatedReferable> fields = result.get(entry.getKey());
            if (fields != null) {
              fields.addAll(entry.getValue());
            }
          }
        }
      }

      Map<LocatedReferable, List<LocatedReferable>> renamings = underlyingClass == null ? Collections.emptyMap() : getRenamings(classDef);
      for (FieldReferable field : (underlyingClass == null ? classDef : underlyingClass).getFieldReferables()) {
        List<LocatedReferable> fields = underlyingClass == null ? result.computeIfAbsent(field, k -> new ArrayList<>()) : result.get(field);
        if (fields != null) {
          List<LocatedReferable> fields2 = renamings.get(field);
          if (fields2 != null) {
            fields.addAll(fields2);
          } else {
            fields.add(field);
          }
        }
      }

      return result;
    }

    private static Map<LocatedReferable, List<LocatedReferable>> getRenamings(ClassReferable classDef) {
      Deque<ClassReferable> toVisit = new ArrayDeque<>();
      Set<ClassReferable> visitedClasses = new HashSet<>();
      Map<LocatedReferable, List<LocatedReferable>> renamings = new HashMap<>();
      toVisit.add(classDef);

      while (!toVisit.isEmpty()) {
        ClassReferable classRef = toVisit.pop();
        ClassReferable underlyingClass = classRef.getUnderlyingReference();
        if (underlyingClass != null) {
          classRef = underlyingClass;
        }
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
