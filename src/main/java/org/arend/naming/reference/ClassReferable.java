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
      return getNotImplementedFields(classDef, new HashSet<>(), new HashMap<>()).keySet();
    }

    public static HashMap<FieldReferable, List<LocatedReferable>> getNotImplementedFields(ClassReferable classDef, List<Boolean> argumentsExplicitness, HashMap<ClassReferable, Set<FieldReferable>> superClassesFields) {
        HashMap<FieldReferable, List<LocatedReferable>> result = getNotImplementedFields(classDef, new HashSet<>(), superClassesFields);
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

    private static HashMap<FieldReferable, List<LocatedReferable>> getNotImplementedFields(ClassReferable classDef, Set<ClassReferable> visited, HashMap<ClassReferable, Set<FieldReferable>> superClassesFields) {
      if (!visited.add(classDef)) {
        return new HashMap<>();
      }

      HashMap<FieldReferable, List<LocatedReferable>> result = new LinkedHashMap<>();
      for (ClassReferable superClass : classDef.getSuperClassReferences()) {
        HashMap<FieldReferable, List<LocatedReferable>> superClassMap = getNotImplementedFields(superClass, visited, superClassesFields);
        superClassesFields.compute(superClass, (k,oldFields) -> {
          if (oldFields == null) {
            return superClassMap.keySet();
          } else {
            oldFields.retainAll(superClassMap.keySet());
            return oldFields;
          }
        });
        for (Map.Entry<FieldReferable, List<LocatedReferable>> entry : superClassMap.entrySet()) {
          result.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).addAll(entry.getValue());
        }
      }

      ClassReferable underlyingClass = classDef.getUnderlyingReference();
      Map<LocatedReferable, List<LocatedReferable>> renamings;
      if (underlyingClass == null) {
        renamings = Collections.emptyMap();
      } else {
        renamings = new HashMap<>();
        for (FieldReferable field : classDef.getFieldReferables()) {
          LocatedReferable underlyingField = field.getUnderlyingReference();
          if (underlyingField != null) {
            renamings.computeIfAbsent(underlyingField, k -> new ArrayList<>(1)).add(field);
          }
        }
      }
      for (FieldReferable field : (underlyingClass == null ? classDef : underlyingClass).getFieldReferables()) {
        List<LocatedReferable> fields = result.computeIfAbsent(field, k -> new ArrayList<>());
        List<LocatedReferable> fields2 = renamings.get(field);
        if (fields2 != null) {
          fields.addAll(fields2);
        } else {
          fields.add(field);
        }
      }

      for (Referable fieldImpl : classDef.getImplementedFields()) {
        if (fieldImpl instanceof ClassReferable) {
          Set<FieldReferable> superClassFields = superClassesFields.remove(fieldImpl);
          if (superClassFields != null) {
            result.keySet().removeAll(superClassFields);
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
            for (Set<FieldReferable> fields : superClassesFields.values()) {
              fields.remove(field);
            }
          }
        }
      }

      return result;
    }
  }
}
