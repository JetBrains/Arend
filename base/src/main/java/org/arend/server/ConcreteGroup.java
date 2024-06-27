package org.arend.server;

import org.arend.naming.reference.*;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteCompareVisitor;
import org.arend.term.group.Group;
import org.arend.term.group.Statement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public record ConcreteGroup(@NotNull LocatedReferable referable, @Nullable Concrete.Definition definition, @NotNull List<? extends ConcreteStatement> statements, @NotNull List<? extends ConcreteGroup> dynamicGroups, @NotNull List<? extends ParameterReferable> externalParameters) implements Group {
  private void copyDefinition(ConcreteGroup to, ConcreteGroup from, Map<TCDefReferable, TCDefReferable> renamed, Map<ConcreteLocatedReferable, Concrete.Definition> concreteUpdated) {
    if (from.definition == null || to.definition == null || !ConcreteCompareVisitor.compareReferables(to.definition.getData(), from.definition.getData())) {
      return;
    }

    renamed.put(to.definition.getData(), from.definition.getData());
    TCDefReferable ref = from.definition.getData();
    if (!(ref instanceof ConcreteLocatedReferable)) {
      throw new IllegalStateException();
    }
    concreteUpdated.put((ConcreteLocatedReferable) ref, to.definition);
    to.definition.setData(from.definition.getData());
    Map<TCDefReferable, TCDefReferable> subRenamed = to.copyReferablesFrom(from, concreteUpdated);

    if (!(to.definition instanceof Concrete.FunctionDefinition fun && fun.getBody() instanceof Concrete.CoelimFunctionBody body)) {
      return;
    }

    for (Concrete.CoClauseElement element : body.getCoClauseElements()) {
      if (element instanceof Concrete.CoClauseFunctionReference funcRef) {
        TCDefReferable newRef = subRenamed.get(funcRef.functionReference);
        if (newRef != null) {
          funcRef.functionReference = newRef;
        }
      }
    }
  }

  public Map<TCDefReferable, TCDefReferable> copyReferablesFrom(ConcreteGroup other, Map<ConcreteLocatedReferable, Concrete.Definition> concreteUpdated) {
    Map<String, ConcreteGroup> refMap = getSubgroupMap(other);
    Map<TCDefReferable, TCDefReferable> result = new HashMap<>();

    for (ConcreteStatement statement : statements) {
      ConcreteGroup subgroup = statement.group();
      if (subgroup != null) {
        ConcreteGroup otherSubgroup = refMap.remove(subgroup.referable.getRefName());
        if (otherSubgroup != null) {
          copyDefinition(subgroup, otherSubgroup, result, concreteUpdated);
        }
      }
    }

    for (ConcreteGroup subgroup : dynamicGroups) {
      if (subgroup.definition != null) {
        ConcreteGroup otherSubgroup = refMap.remove(subgroup.referable.getRefName());
        if (otherSubgroup != null) {
          copyDefinition(subgroup, otherSubgroup, result, concreteUpdated);
        }
      }
    }

    return result;
  }

  private static @NotNull Map<String, ConcreteGroup> getSubgroupMap(ConcreteGroup other) {
    Map<String, ConcreteGroup> refMap = new HashMap<>();
    for (ConcreteStatement statement : other.statements) {
      ConcreteGroup subgroup = statement.group();
      if (subgroup != null) {
        refMap.putIfAbsent(subgroup.referable.getRefName(), subgroup);
      }
    }

    for (ConcreteGroup subgroup : other.dynamicGroups) {
      refMap.putIfAbsent(subgroup.referable.getRefName(), subgroup);
    }

    return refMap;
  }

  @Override
  public @NotNull LocatedReferable getReferable() {
    return referable;
  }

  @Override
  public @NotNull List<? extends Statement> getStatements() {
    return statements;
  }

  @Override
  public @NotNull List<? extends InternalReferable> getInternalReferables() {
    return definition instanceof Concrete.DataDefinition ? getConstructors() : getFields();
  }

  @Override
  public @NotNull List<? extends InternalReferable> getConstructors() {
    if (definition instanceof Concrete.DataDefinition dataDef) {
      List<InternalReferable> result = new ArrayList<>();
      for (Concrete.ConstructorClause clause : dataDef.getConstructorClauses()) {
        for (Concrete.Constructor constructor : clause.getConstructors()) {
          result.add(new SimpleInternalReferable(constructor.getData(), true));
        }
      }
      return result;
    } else {
      return Collections.emptyList();
    }
  }

  @Override
  public @NotNull List<? extends InternalReferable> getFields() {
    if (definition instanceof Concrete.ClassDefinition classDef) {
      List<InternalReferable> result = new ArrayList<>();
      for (Concrete.ClassElement element : classDef.getElements()) {
        if (element instanceof Concrete.ClassField field) {
          result.add(new SimpleInternalReferable(field.getData(), !field.getData().isParameterField()));
        }
      }
      return result;
    } else {
      return Collections.emptyList();
    }
  }

  @Override
  public @NotNull List<? extends Group> getDynamicSubgroups() {
    return dynamicGroups;
  }

  @Override
  public @NotNull List<? extends ParameterReferable> getExternalParameters() {
    return externalParameters;
  }
}
