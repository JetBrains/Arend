package org.arend.core.definition;

import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.context.param.TypedDependentLink;
import org.arend.core.expr.*;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.type.TypeExpression;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.naming.reference.TCClassReferable;

import java.util.*;

public class ClassDefinition extends Definition {
  private final Set<ClassDefinition> mySuperClasses;
  private final LinkedHashSet<ClassField> myFields;
  private final List<ClassField> myPersonalFields;
  private final Map<ClassField, LamExpression> myImplemented;
  private ClassField myCoercingField;
  private Sort mySort;
  private boolean myRecord = false;
  private final CoerceData myCoerce = new CoerceData(this);
  private Map<Set<ClassField>, Level> myLevels = new HashMap<>();

  public ClassDefinition(TCClassReferable referable) {
    super(referable, TypeCheckingStatus.HEADER_HAS_ERRORS);
    mySuperClasses = new LinkedHashSet<>();
    myFields = new LinkedHashSet<>();
    myPersonalFields = new ArrayList<>();
    myImplemented = new HashMap<>();
    mySort = Sort.PROP;
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

  public Map<? extends Set<ClassField>, ? extends Level> getLevels() {
    return myLevels;
  }

  public void setLevels(Map<Set<ClassField>,Level> levels) {
    myLevels = levels;
  }

  public void addLevel(Set<ClassField> fields, Level level) {
    myLevels.put(fields, level);
  }

  public Sort computeSort(Map<ClassField,Expression> implemented) {
    ClassCallExpression thisClass = new ClassCallExpression(this, Sort.STD, Collections.emptyMap(), mySort, hasUniverses());
    Sort sort = Sort.PROP;

    for (ClassField field : myFields) {
      if (myImplemented.containsKey(field) || implemented.containsKey(field)) {
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

    Level hLevel = myLevels.get(implemented.keySet());
    return hLevel == null ? sort : hLevel.isProp() ? Sort.PROP : new Sort(sort.getPLevel(), hLevel);
  }

  public void updateSort() {
    mySort = computeSort(Collections.emptyMap());
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

  public Set<Map.Entry<ClassField, LamExpression>> getImplemented() {
    return myImplemented.entrySet();
  }

  public Set<? extends ClassField> getImplementedFields() {
    return myImplemented.keySet();
  }

  public LamExpression getImplementation(ClassField field) {
    return myImplemented.get(field);
  }

  public LamExpression implementField(ClassField field, LamExpression impl) {
    return myImplemented.putIfAbsent(field, impl);
  }

  public void removeImplementation(ClassField field) {
    myImplemented.computeIfPresent(field, (f,i) -> new LamExpression(i.getResultSort(), i.getParameters(), new ErrorExpression(null, null)));
  }

  public DependentLink getClassFieldParameters(Sort sortArgument) {
    Map<ClassField, Expression> implementations = new HashMap<>();
    Expression newExpr = new NewExpression(new ClassCallExpression(this, sortArgument, implementations, Sort.PROP, false));
    if (myFields.isEmpty()) {
      return EmptyDependentLink.getInstance();
    }

    DependentLink first = null, last = null;
    for (ClassField field : myFields) {
      PiExpression piExpr = field.getType(sortArgument);
      Expression type = piExpr.applyExpression(newExpr);
      DependentLink link = new TypedDependentLink(true, field.getName(), type instanceof Type ? (Type) type : new TypeExpression(type, piExpr.getResultSort()), EmptyDependentLink.getInstance());
      if (last != null) {
        last.setNext(link);
      }
      last = link;
      if (first == null) {
        first = link;
      }
      implementations.put(field, new ReferenceExpression(link));
    }

    return first;
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
}
