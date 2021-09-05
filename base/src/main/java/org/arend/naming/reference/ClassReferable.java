package org.arend.naming.reference;

import org.arend.term.abs.Abstract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public interface ClassReferable extends LocatedReferable {
  boolean isRecord();
  @Nullable Abstract.LevelParameters getPLevelParameters();
  @Nullable Abstract.LevelParameters getHLevelParameters();
  @NotNull List<? extends ClassReferable> getSuperClassReferences();
  boolean hasLevels(int index);
  @NotNull Collection<? extends FieldReferable> getFieldReferables();
  @NotNull Collection<? extends Referable> getImplementedFields();
  @NotNull Collection<? extends GlobalReferable> getDynamicReferables();

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

    public static HashSet<FieldReferable> getNotImplementedFields(ClassReferable classDef, List<Boolean> argumentsExplicitness, boolean withTailImplicits, HashMap<ClassReferable, Set<FieldReferable>> superClassesFields) {
      HashSet<FieldReferable> fieldSet = new LinkedHashSet<>(getAllFields(classDef, new HashSet<>(), superClassesFields));
      removeImplemented(classDef, fieldSet, superClassesFields);

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

      // Remove tail implicit parameters (only classes)
      if (withTailImplicits) {
        while (it.hasNext()) {
          FieldReferable field = it.next();
          if (field.isExplicitField() || !field.isParameterField()) {
            break;
          }
          ClassReferable typeClass = field instanceof TypedReferable ? ((TypedReferable) field).getTypeClassReference() : null;
          if (typeClass == null || typeClass.isRecord()) {
            break;
          }

          for (Set<FieldReferable> fields : superClassesFields.values()) {
            fields.remove(field);
          }
          it.remove();
        }
      }

      return fieldSet;
    }

    private static void removeImplemented(ClassReferable classDef, Set<FieldReferable> result, HashMap<ClassReferable, Set<FieldReferable>> superClassesFields) {
      Deque<ClassReferable> toVisit = new ArrayDeque<>();
      Set<ClassReferable> visitedClasses = new HashSet<>();
      toVisit.add(classDef);

      while (!toVisit.isEmpty()) {
        ClassReferable classRef = toVisit.pop();
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
          } else if (fieldImpl instanceof FieldReferable) {
            LocatedReferable field = (FieldReferable) fieldImpl;
            result.remove(field);
            if (superClassesFields != null) {
              for (Set<FieldReferable> fields : superClassesFields.values()) {
                fields.remove(field);
              }
            }
          }
        }

        toVisit.addAll(classRef.getSuperClassReferences());
      }
    }

    private static Set<FieldReferable> getAllFields(ClassReferable classDef, Set<ClassReferable> visited, HashMap<ClassReferable, Set<FieldReferable>> superClassesFields) {
      if (!visited.add(classDef)) {
        Set<FieldReferable> fieldSet = superClassesFields.get(classDef);
        return fieldSet != null ? fieldSet : new LinkedHashSet<>();
      }

      Set<FieldReferable> result = new LinkedHashSet<>();

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
        result.addAll(superClassSet);
      }

      result.addAll(classDef.getFieldReferables());
      return result;
    }
  }
}
