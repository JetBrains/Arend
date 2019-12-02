package org.arend.typechecking;

import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.ClassField;
import org.arend.core.expr.AbsExpression;
import org.arend.core.expr.PiExpression;
import org.arend.core.expr.visitor.FieldsCollector;
import org.arend.core.sort.Sort;

import java.util.*;

public class FieldDFS {
  private final ClassDefinition classDef;
  private final Map<ClassField, Boolean> state = new HashMap<>();
  private final Map<ClassField, Set<ClassField>> references = new HashMap<>();

  public FieldDFS(ClassDefinition classDef) {
    this.classDef = classDef;
  }

  public List<ClassField> findCycle(ClassField field) {
    List<ClassField> cycle = dfs(field);
    if (cycle != null) {
      Collections.reverse(cycle);
      for (ClassField dep : cycle) {
        references.remove(dep);
      }
    }
    return cycle;
  }

  private List<ClassField> dfs(ClassField field) {
    Boolean prevState = state.putIfAbsent(field, false);
    if (Boolean.TRUE.equals(prevState)) {
      return null;
    }
    if (Boolean.FALSE.equals(prevState)) {
      List<ClassField> cycle = new ArrayList<>();
      cycle.add(field);
      return cycle;
    }

    Set<ClassField> deps = references.computeIfAbsent(field, f -> {
      AbsExpression impl = classDef.getImplementation(field);
      PiExpression type = field.getType(Sort.STD);
      Set<ClassField> result = FieldsCollector.getFields(type.getCodomain(), type.getParameters(), classDef.getFields());
      if (impl != null) {
        FieldsCollector.getFields(impl.getExpression(), impl.getBinding(), classDef.getFields(), result);
      }
      return result;
    });

    for (ClassField dep : deps) {
      List<ClassField> cycle = dfs(dep);
      if (cycle != null) {
        if (cycle.get(0) != field) {
          cycle.add(field);
        }
        return cycle;
      }
    }

    state.put(field, true);
    return null;
  }

  public List<ClassField> checkDependencies(ClassField field, Collection<? extends ClassField> dependencies) {
    for (ClassField dependency : dependencies) {
      if (dependency == field) {
        return Collections.singletonList(field);
      }

      state.clear();
      state.put(field, false);
      List<ClassField> cycle = dfs(dependency);
      if (cycle != null) {
        Collections.reverse(cycle.subList(1, cycle.size()));
        return cycle;
      }
    }
    references.computeIfAbsent(field, f -> new HashSet<>()).addAll(dependencies);
    return null;
  }
}
