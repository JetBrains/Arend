package org.arend.typechecking.provider;

import org.arend.naming.reference.*;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public interface ConcreteProvider extends PartialConcreteProvider {
  @Nullable Concrete.ReferableDefinition getConcrete(GlobalReferable referable);
  @Nullable Concrete.FunctionDefinition getConcreteFunction(GlobalReferable referable);
  @Nullable Concrete.FunctionDefinition getConcreteInstance(GlobalReferable referable);
  @Nullable Concrete.ClassDefinition getConcreteClass(GlobalReferable referable);
  @Nullable Concrete.DataDefinition getConcreteData(GlobalReferable referable);

  @Nullable default TCReferable getTCReferable(GlobalReferable referable) {
    if (referable instanceof TCReferable) {
      return (TCReferable) referable;
    }
    Concrete.ReferableDefinition def = getConcrete(referable);
    return def == null ? null : def.getData();
  }

  @Override
  @Nullable
  default Concrete.ReferenceExpression getInstanceTypeReference(GlobalReferable instance) {
    Concrete.FunctionDefinition concreteInstance = getConcreteInstance(instance);
    Concrete.Expression type = concreteInstance == null ? null : concreteInstance.getResultType();
    return type == null ? null : type.getUnderlyingReferenceExpression();
  }

  @Override
  default boolean isInstance(GlobalReferable ref) {
    return getConcreteInstance(ref) != null;
  }

  @Override
  default boolean isUse(GlobalReferable ref) {
    Concrete.FunctionDefinition func = getConcreteFunction(ref);
    return func != null && func.getKind().isUse();
  }

  @Override
  default boolean isData(GlobalReferable ref) {
    return getConcreteData(ref) != null;
  }

  default boolean isClass(GlobalReferable ref) {
    return ref instanceof ClassReferable || ref.getUnderlyingReferable() instanceof ClassReferable;
  }

  @Override
  default boolean isFunction(GlobalReferable ref) {
    return getConcreteFunction(ref) != null;
  }

  default boolean isSubClassOf(TCReferable def, TCReferable ref) {
    if (def == ref) {
      return true;
    }

    Set<TCReferable> visitedClasses = new HashSet<>();
    Deque<TCReferable> toVisit = new ArrayDeque<>();
    toVisit.add(def);
    while (!toVisit.isEmpty()) {
      def = toVisit.pop();
      if (ref == def) {
        return true;
      }
      if (visitedClasses.add(def)) {
        Concrete.ClassDefinition classDef = getConcreteClass(def);
        if (classDef != null) {
          for (Concrete.ReferenceExpression superClass : classDef.getSuperClasses()) {
            if (superClass.getReferent() instanceof TCReferable) {
              toVisit.add((TCReferable) superClass.getReferent());
            }
          }
        }
      }
    }

    return false;
  }

  default Set<TCReferable> getClassFields(TCReferable classRef) {
    Set<TCReferable> fields = new HashSet<>();
    Set<TCReferable> visitedClasses = new HashSet<>();
    Deque<TCReferable> toVisit = new ArrayDeque<>();
    toVisit.add(classRef);

    while (!toVisit.isEmpty()) {
      classRef = toVisit.removeLast();
      if (!visitedClasses.add(classRef)) {
        continue;
      }

      Concrete.ClassDefinition classDef = getConcreteClass(classRef);
      if (classDef == null) {
        continue;
      }
      for (Concrete.ClassElement element : classDef.getElements()) {
        if (element instanceof Concrete.ClassField) {
          fields.add(((Concrete.ClassField) element).getData());
        }
      }

      List<Concrete.ReferenceExpression> superClasses = classDef.getSuperClasses();
      for (int i = superClasses.size() - 1; i >= 0; i--) {
        Referable ref = superClasses.get(i).getReferent();
        if (ref instanceof TCReferable) {
          toVisit.add((TCReferable) ref);
        }
      }
    }

    return fields;
  }
}
