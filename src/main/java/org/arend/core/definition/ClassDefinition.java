package org.arend.core.definition;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.*;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.naming.reference.TCClassReferable;

import java.util.*;

public class ClassDefinition extends Definition {
  private final Set<ClassDefinition> mySuperClasses = new LinkedHashSet<>();
  private final LinkedHashSet<ClassField> myFields = new LinkedHashSet<>();
  private final List<ClassField> myPersonalFields = new ArrayList<>();
  private final Map<ClassField, AbsExpression> myImplemented = new HashMap<>();
  private final Map<ClassField, PiExpression> myOverridden = new HashMap<>();
  private ClassField myCoercingField;
  private Sort mySort = Sort.PROP;
  private boolean myRecord = false;
  private final CoerceData myCoerce = new CoerceData(this);
  private Set<ClassField> myGoodThisFields = Collections.emptySet();
  private Set<ClassField> myTypeClassParameters = Collections.emptySet();
  private ParametersLevels<ParametersLevel> myParametersLevels = new ParametersLevels<>();

  public ClassDefinition(TCClassReferable referable) {
    super(referable, TypeCheckingStatus.HEADER_NEEDS_TYPE_CHECKING);
  }

  @Override
  public TCClassReferable getReferable() {
    return (TCClassReferable) super.getReferable();
  }

  public boolean isRecord() {
    return myRecord;
  }

  public void setRecord() {
    myRecord = true;
  }

  public ClassField getClassifyingField() {
    return myCoercingField;
  }

  public void setClassifyingField(ClassField coercingField) {
    myCoercingField = coercingField;
  }

  public static class ParametersLevel extends org.arend.core.definition.ParametersLevel {
    public final List<ClassField> fields;

    public ParametersLevel(DependentLink parameters, int level, List<ClassField> fields) {
      super(parameters, level);
      this.fields = fields;
    }

    @Override
    public boolean hasEquivalentDomain(org.arend.core.definition.ParametersLevel another) {
      return another instanceof ParametersLevel && fields.equals(((ParametersLevel) another).fields) && super.hasEquivalentDomain(another);
    }
  }

  @Override
  public List<? extends ParametersLevel> getParametersLevels() {
    return myParametersLevels.getList();
  }

  public void addParametersLevel(ParametersLevel parametersLevel) {
    myParametersLevels.add(parametersLevel);
  }

  public Integer getUseLevel(Map<ClassField,Expression> implemented, Binding thisBinding) {
    loop:
    for (ParametersLevel parametersLevel : myParametersLevels.getList()) {
      if (parametersLevel.fields.size() != implemented.size()) {
        continue;
      }
      List<Expression> expressions = new ArrayList<>();
      for (ClassField field : parametersLevel.fields) {
        Expression expr = implemented.get(field);
        if (expr == null || expr.findBinding(thisBinding)) {
          continue loop;
        }
        expressions.add(expr);
      }

      if (parametersLevel.checkExpressionsTypes(expressions)) {
        return parametersLevel.level;
      }
    }
    return null;
  }

  public Sort computeSort(Map<ClassField,Expression> implemented, Binding thisBinding) {
    Integer hLevel = getUseLevel(implemented, thisBinding);
    if (hLevel != null && hLevel == -1) {
      return Sort.PROP;
    }

    ClassCallExpression thisClass = new ClassCallExpression(this, Sort.STD, Collections.emptyMap(), mySort, hasUniverses());
    Sort sort = Sort.PROP;

    for (ClassField field : myFields) {
      if (field.isProperty() || myImplemented.containsKey(field) || implemented.containsKey(field)) {
        continue;
      }

      PiExpression fieldType = field.getType(Sort.STD);
      if (fieldType.getCodomain().isInstance(ErrorExpression.class)) {
        continue;
      }

      Expression type = fieldType
        .applyExpression(new ReferenceExpression(ExpressionFactory.parameter("this", thisClass)))
        .normalize(NormalizeVisitor.Mode.WHNF)
        .getType();
      Sort sort1 = type == null ? null : type.toSort();
      if (sort1 != null) {
        sort = sort.max(sort1);
      }
    }

    return hLevel == null ? sort : new Sort(sort.getPLevel(), new Level(hLevel));
  }

  public void updateSort() {
    mySort = computeSort(Collections.emptyMap(), null);
  }

  public Sort getSort() {
    return mySort;
  }

  public void setSort(Sort sort) {
    mySort = sort;
  }

  @Override
  public CoerceData getCoerceData() {
    return myCoerce;
  }

  public boolean isSubClassOf(ClassDefinition classDefinition) {
    if (this.equals(classDefinition)) return true;
    for (ClassDefinition superClass : mySuperClasses) {
      if (superClass.isSubClassOf(classDefinition)) return true;
    }
    return false;
  }

  public Set<? extends ClassDefinition> getSuperClasses() {
    return mySuperClasses;
  }

  public void addSuperClass(ClassDefinition superClass) {
    mySuperClasses.add(superClass);
  }

  public Set<? extends ClassField> getFields() {
    return myFields;
  }

  public List<? extends ClassField> getPersonalFields() {
    return myPersonalFields;
  }

  public int getNumberOfNotImplementedFields() {
    return myFields.size() - myImplemented.size();
  }

  public void addField(ClassField field) {
    myFields.add(field);
  }

  public void addPersonalField(ClassField field) {
    myPersonalFields.add(field);
  }

  public void addFields(Collection<? extends ClassField> fields) {
    myFields.addAll(fields);
  }

  public boolean isImplemented(ClassField field) {
    return myImplemented.containsKey(field);
  }

  public Set<Map.Entry<ClassField, AbsExpression>> getImplemented() {
    return myImplemented.entrySet();
  }

  public Set<? extends ClassField> getImplementedFields() {
    return myImplemented.keySet();
  }

  public AbsExpression getImplementation(ClassField field) {
    return myImplemented.get(field);
  }

  public AbsExpression implementField(ClassField field, AbsExpression impl) {
    return myImplemented.putIfAbsent(field, impl);
  }

  public Set<? extends ClassField> getGoodThisFields() {
    return myGoodThisFields;
  }

  public boolean isGoodField(ClassField field) {
    return myGoodThisFields.contains(field);
  }

  public void setGoodThisFields(Set<ClassField> goodThisFields) {
    myGoodThisFields = goodThisFields;
  }

  public Set<? extends ClassField> getTypeClassFields() {
    return myTypeClassParameters;
  }

  public boolean isTypeClassField(ClassField field) {
    return myTypeClassParameters.contains(field);
  }

  public void setTypeClassFields(Set<ClassField> typeClassFields) {
    myTypeClassParameters = typeClassFields;
  }

  @Override
  public Expression getTypeWithParams(List<? super DependentLink> params, Sort sortArgument) {
    return new UniverseExpression(mySort.subst(sortArgument.toLevelSubstitution()));
  }

  @Override
  public ClassCallExpression getDefCall(Sort sortArgument, List<Expression> args) {
    return new ClassCallExpression(this, sortArgument, Collections.emptyMap(), mySort.subst(sortArgument.toLevelSubstitution()), hasUniverses());
  }

  public void clear() {
    mySuperClasses.clear();
    myFields.clear();
    myPersonalFields.clear();
    myImplemented.clear();
    myCoercingField = null;
  }

  @Override
  public void fill() {
    for (ClassField field : myPersonalFields) {
      field.fill();
    }
  }
}
