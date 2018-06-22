package com.jetbrains.jetpad.vclang.typechecking.visitor;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.reference.*;
import com.jetbrains.jetpad.vclang.term.Precedence;
import com.jetbrains.jetpad.vclang.term.concrete.BaseConcreteExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.concrete.ConcreteDefinitionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ProxyErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.local.TypecheckingError;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider.ConcreteProvider;

import java.util.*;

public class DesugarVisitor extends BaseConcreteExpressionVisitor<Void> implements ConcreteDefinitionVisitor<Void, Void> {
  private final ConcreteProvider myConcreteProvider;
  private final LocalErrorReporter myErrorReporter;

  private DesugarVisitor(ConcreteProvider concreteProvider, LocalErrorReporter errorReporter) {
    myConcreteProvider = concreteProvider;
    myErrorReporter = errorReporter;
  }

  public static void desugar(Concrete.Definition definition, ConcreteProvider concreteProvider, ErrorReporter errorReporter) {
    definition.accept(new DesugarVisitor(concreteProvider, new ProxyErrorReporter(definition.getData(), errorReporter)), null);
  }

  private Set<LocatedReferable> getClassFields(ClassReferable classRef) {
    Set<LocatedReferable> fields = new HashSet<>();
    Set<GlobalReferable> visitedClasses = new HashSet<>();
    Deque<ClassReferable> toVisit = new ArrayDeque<>();
    toVisit.add(classRef);
    while (!toVisit.isEmpty()) {
      classRef = toVisit.pop();
      ClassReferable underlyingRef = classRef.getUnderlyingReference();
      if (underlyingRef != null) {
        classRef = underlyingRef;
      }
      if (!visitedClasses.add(classRef)) {
        continue;
      }

      fields.addAll(classRef.getFieldReferables());
      toVisit.addAll(classRef.getSuperClassReferences());
    }
    return fields;
  }

  private Referable checkDefinition(Concrete.Definition def) {
    if (def.enclosingClass != null) {
      Referable thisParameter = new LocalReferable("this");
      def.accept(new ClassFieldChecker(thisParameter, def.enclosingClass, myConcreteProvider, getClassFields(def.enclosingClass), null, myErrorReporter), null);
      return thisParameter;
    } else {
      return null;
    }
  }

  @Override
  public Void visitFunction(Concrete.FunctionDefinition def, Void params) {
    // Add this parameter
    Referable thisParameter = checkDefinition(def);
    if (thisParameter != null) {
      def.getParameters().add(0, new Concrete.TelescopeParameter(def.getData(), false, Collections.singletonList(thisParameter), new Concrete.ReferenceExpression(def.getData(), def.enclosingClass)));
      if (def.getBody() instanceof Concrete.ElimFunctionBody && ((Concrete.ElimFunctionBody) def.getBody()).getEliminatedReferences().isEmpty()) {
        for (Concrete.FunctionClause clause : ((Concrete.ElimFunctionBody) def.getBody()).getClauses()) {
          clause.getPatterns().add(0, new Concrete.NamePattern(clause.getData(), false, thisParameter));
        }
      }
    }

    // Process expressions
    super.visitFunction(def, null);
    return null;
  }

  @Override
  public Void visitData(Concrete.DataDefinition def, Void params) {
    // Add this parameter
    Referable thisParameter = checkDefinition(def);
    if (thisParameter != null) {
      def.getParameters().add(0, new Concrete.TelescopeParameter(def.getData(), false, Collections.singletonList(thisParameter), new Concrete.ReferenceExpression(def.getData(), def.enclosingClass)));
      if (def.getEliminatedReferences() != null && def.getEliminatedReferences().isEmpty()) {
        for (Concrete.ConstructorClause clause : def.getConstructorClauses()) {
          clause.getPatterns().add(0, new Concrete.NamePattern(clause.getData(), false, thisParameter));
        }
      }
    }

    // Process expressions
    super.visitData(def, null);
    return null;
  }

  @Override
  public Void visitClass(Concrete.ClassDefinition def, Void params) {
    Set<LocatedReferable> fields = getClassFields(def.getData());

    // Process enclosing class
    boolean isParentField = false;
    if (def.enclosingClass != null) {
      Set<String> names = new HashSet<>();
      for (Concrete.ClassField field : def.getFields()) {
        names.add(field.getData().textRepresentation());
      }

      String name = "parent";
      while (names.contains(name)) {
        name = name + "'";
      }

      TCReferable thisParameter = new LocatedReferableImpl(Precedence.DEFAULT, name, def.getData(), false);
      def.getFieldsExplicitness().add(0, false);
      def.getFields().add(0, new Concrete.ClassField(thisParameter, def, false, new Concrete.ReferenceExpression(def.getData(), def.enclosingClass)));
      fields.add(thisParameter);
      isParentField = true;
    }

    Set<TCReferable> futureFields = new HashSet<>();
    for (Concrete.ClassField field : def.getFields()) {
      futureFields.add(field.getData());
    }

    // Check fields
    ClassFieldChecker classFieldChecker = new ClassFieldChecker(null, def.getData(), myConcreteProvider, fields, futureFields, myErrorReporter);
    Concrete.Expression previousType = null;
    for (int i = 0; i < def.getFields().size(); i++) {
      Concrete.ClassField classField = def.getFields().get(i);
      Concrete.Expression fieldType = classField.getResultType();
      Referable thisParameter = new LocalReferable("this");
      classFieldChecker.setThisParameter(thisParameter);
      if (fieldType == previousType) {
        classField.setResultType(def.getFields().get(i - 1).getResultType());
      } else {
        previousType = fieldType;
        if (!isParentField) {
          fieldType = fieldType.accept(classFieldChecker, null);
        }
        classField.setResultType(new Concrete.PiExpression(fieldType.getData(), Collections.singletonList(new Concrete.TelescopeParameter(fieldType.getData(), false, Collections.singletonList(thisParameter), new Concrete.ReferenceExpression(fieldType.getData(), def.getData()))), fieldType));
      }
      futureFields.remove(classField.getData());
      isParentField = false;
    }

    // Check implementations
    for (Concrete.ClassFieldImpl classFieldImpl : def.getImplementations()) {
      Concrete.Expression impl = classFieldImpl.implementation;
      Referable thisParameter = new LocalReferable("this");
      classFieldChecker.setThisParameter(thisParameter);
      classFieldImpl.implementation = new Concrete.LamExpression(impl.getData(), Collections.singletonList(new Concrete.TelescopeParameter(impl.getData(), false, Collections.singletonList(thisParameter), new Concrete.ReferenceExpression(impl.getData(), def.getData()))), impl.accept(classFieldChecker, null));
    }

    // Process expressions
    super.visitClass(def, null);
    return null;
  }

  @Override
  public Void visitInstance(Concrete.Instance def, Void params) {
    // Add this parameter
    Referable thisParameter = checkDefinition(def);
    if (thisParameter != null) {
      def.getParameters().add(0, new Concrete.TelescopeParameter(def.getData(), false, Collections.singletonList(thisParameter), new Concrete.ReferenceExpression(def.getData(), def.enclosingClass)));
    }

    // Process expressions
    super.visitInstance(def, null);
    return null;
  }

  @Override
  public Concrete.Expression visitApp(Concrete.AppExpression expr, Void params) {
    // Convert class call with arguments to class extension.
    expr = (Concrete.AppExpression) super.visitApp(expr, null);
    Concrete.Expression fun = expr.getFunction();
    if (fun instanceof Concrete.ReferenceExpression) {
      Referable ref = ((Concrete.ReferenceExpression) fun).getReferent();
      if (ref instanceof ClassReferable) {
        Concrete.ClassDefinition def = myConcreteProvider.getConcreteClass((ClassReferable) ref);
        if (def != null) {
          List<Concrete.ClassFieldImpl> classFieldImpls = new ArrayList<>();
          List<Boolean> fieldsExplicitness = def.getFieldsExplicitness();
          for (int i = 0, j = 0; i < expr.getArguments().size(); i++, j++) {
            boolean fieldExplicit = j < fieldsExplicitness.size() ? fieldsExplicitness.get(j) : true;
            Concrete.Expression argument = expr.getArguments().get(i).expression;
            if (fieldExplicit == expr.getArguments().get(i).isExplicit()) {
              classFieldImpls.add(new Concrete.ClassFieldImpl(argument.getData(), null, argument));
            } else if (fieldExplicit) {
              myErrorReporter.report(new TypecheckingError("Expected an explicit argument", argument));
            } else {
              classFieldImpls.add(new Concrete.ClassFieldImpl(argument.getData(), null, new Concrete.HoleExpression(argument.getData())));
              i--;
            }
          }
          return new Concrete.ClassExtExpression(expr.getData(), fun, classFieldImpls);
        }
      }
    }
    return expr;
  }
}
