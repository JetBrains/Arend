package org.arend.typechecking.visitor;

import org.arend.core.context.LinkList;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.context.param.TypedDependentLink;
import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.Definition;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.*;
import org.arend.core.expr.type.ExpectedType;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.type.TypeExpression;
import org.arend.core.sort.Sort;
import org.arend.naming.reference.HiddenLocalReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCClassReferable;
import org.arend.naming.reference.TCReferable;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.LocalErrorReporter;
import org.arend.typechecking.error.local.TypecheckingError;
import org.arend.typechecking.instance.pool.LocalInstancePool;
import org.arend.typechecking.result.TypecheckingResult;
import org.arend.util.Decision;

import java.util.*;

import static org.arend.core.expr.ExpressionFactory.parameter;

public abstract class BaseDefinitionTypechecker {
  protected BaseTypechecker typechecker;
  protected LocalErrorReporter errorReporter;

  protected BaseDefinitionTypechecker(BaseTypechecker typechecker) {
    this.typechecker = typechecker;
    errorReporter = typechecker == null ? null : typechecker.errorReporter;
  }

  protected void setTypechecker(BaseTypechecker typechecker) {
    this.typechecker = typechecker;
    errorReporter = typechecker.errorReporter;
  }

  protected Sort typecheckParameters(Concrete.ReferableDefinition def, LinkList list, LocalInstancePool localInstancePool, Sort expectedSort, DependentLink oldParameters) {
    Sort sort = Sort.PROP;

    if (oldParameters != null) {
      list.append(oldParameters);
    }

    for (Concrete.TypeParameter parameter : Objects.requireNonNull(Concrete.getParameters(def, true))) {
      Type paramResult = typechecker.checkType(parameter.getType(), expectedSort == null ? ExpectedType.OMEGA : new UniverseExpression(expectedSort), true);
      if (paramResult == null) {
        sort = null;
        paramResult = new TypeExpression(new ErrorExpression(null, null), Sort.SET0);
      } else if (sort != null) {
        sort = sort.max(paramResult.getSortOfType());
      }

      DependentLink param;
      int numberOfParameters;
      boolean oldParametersOK = true;
      if (parameter instanceof Concrete.TelescopeParameter) {
        List<? extends Referable> referableList = parameter.getReferableList();
        List<String> names = parameter.getNames();
        param = oldParameters != null
          ? oldParameters
          : referableList.size() == 1 && referableList.get(0) instanceof HiddenLocalReferable
          ? new TypedDependentLink(parameter.getExplicit(), names.get(0), paramResult, true, EmptyDependentLink.getInstance())
          : parameter(parameter.getExplicit(), names, paramResult);
        numberOfParameters = names.size();

        if (oldParameters == null) {
          int i = 0;
          for (DependentLink link = param; link.hasNext(); link = link.getNext(), i++) {
            typechecker.addBinding(referableList.get(i), link);
          }
        } else {
          for (int i = 0; i < names.size(); i++) {
            if (oldParameters.hasNext()) {
              typechecker.addBinding(referableList.get(i), oldParameters);
              oldParameters = oldParameters.getNext();
            } else {
              oldParametersOK = false;
              break;
            }
          }
        }
      } else {
        numberOfParameters = 1;
        if (oldParameters != null) {
          param = oldParameters;
          if (oldParameters.hasNext()) {
            typechecker.addBinding(null, oldParameters);
            oldParameters = oldParameters.getNext();
          } else {
            oldParametersOK = false;
          }
        } else {
          param = parameter(parameter.getExplicit(), (String) null, paramResult);
        }
      }
      if (!oldParametersOK) {
        errorReporter.report(new TypecheckingError("Cannot typecheck definition. Try to clear cache", parameter));
        return null;
      }

      if (localInstancePool != null) {
        TCClassReferable classRef = parameter.getType().getUnderlyingTypeClass();
        if (classRef != null) {
          ClassDefinition classDef = (ClassDefinition) typechecker.getTypechecked(classRef);
          if (classDef != null && !classDef.isRecord()) {
            ClassField classifyingField = classDef.getClassifyingField();
            int i = 0;
            for (DependentLink link = param; i < numberOfParameters; link = link.getNext(), i++) {
              ReferenceExpression reference = new ReferenceExpression(link);
              localInstancePool.addInstance(classifyingField == null ? null : FieldCallExpression.make(classifyingField, paramResult.getSortOfType(), reference), classRef, reference, parameter);
            }
          }
        }
      }

      if (oldParameters == null) {
        list.append(param);
        for (; param.hasNext(); param = param.getNext()) {
          typechecker.addBinding(null, param);
        }
      }
    }

    return sort;
  }

  protected Decision isPropLevel(Concrete.Expression expression) {
    Referable fun = expression == null ? null : expression.getUnderlyingReferable();
    if (fun instanceof TCReferable) {
      Definition typeDef = typechecker.getTypechecked((TCReferable) fun);
      if (typeDef != null) {
        boolean couldBe = false;
        for (Definition.ParametersLevel parametersLevel : typeDef.getParametersLevels()) {
          if (parametersLevel.level == -1) {
            if (parametersLevel.parameters == null) {
              return Decision.YES;
            }
            couldBe = true;
          }
        }
        if (couldBe) {
          return Decision.MAYBE;
        }
      } else {
        return Decision.MAYBE;
      }
    }
    return Decision.NO;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  protected boolean checkLevel(boolean isLemma, boolean isProperty, Integer level, Concrete.SourceNode sourceNode) {
    if ((isLemma || isProperty) && (level == null && !typechecker.isDumb() || level != null && level != -1)) {
      errorReporter.report(new TypecheckingError("The level of a " + (isLemma ? "lemma" : "property") + " must be \\Prop", sourceNode));
      return false;
    } else {
      return true;
    }
  }

  protected boolean typecheckFunctionHeader(FunctionDefinition typedDef, Concrete.FunctionDefinition def, LocalInstancePool localInstancePool, boolean newDef) {
    LinkList list = new LinkList();

    if (def.getResultTypeLevel() != null && !(def.getKind() == Concrete.FunctionDefinition.Kind.LEMMA || def.getBody() instanceof Concrete.ElimFunctionBody)) {
      errorReporter.report(new TypecheckingError(TypecheckingError.Kind.LEVEL_IN_FUNCTION, def.getResultTypeLevel()));
      def.setResultTypeLevel(null);
    }

    boolean paramsOk = typecheckParameters(def, list, localInstancePool, null, newDef ? null : typedDef.getParameters()) != null;

    Expression expectedType = null;
    Concrete.Expression cResultType = def.getResultType();
    boolean isLemma = def.getKind() == Concrete.FunctionDefinition.Kind.LEMMA || def.getKind() == Concrete.FunctionDefinition.Kind.LEVEL;
    if (cResultType != null) {
      Decision isProp = isPropLevel(cResultType);
      boolean needProp = def.getKind() == Concrete.FunctionDefinition.Kind.LEMMA && def.getResultTypeLevel() == null;
      ExpectedType typeExpectedType = needProp && isProp == Decision.NO ? new UniverseExpression(Sort.PROP) : ExpectedType.OMEGA;
      Type expectedTypeResult = def.getBody() instanceof Concrete.CoelimFunctionBody && !def.isRecursive() ? null // The result type will be typechecked together with all field implementations during body typechecking.
        : typechecker.checkType(cResultType, typeExpectedType, !(def.getBody() instanceof Concrete.TermFunctionBody) || def.isRecursive() || isLemma);
      if (expectedTypeResult != null) {
        expectedType = expectedTypeResult.getExpr();
        if (needProp && isProp == Decision.MAYBE) {
          Sort sort = expectedTypeResult.getSortOfType();
          if (sort == null || !sort.isProp()) {
            DefCallExpression defCall = expectedType.checkedCast(DefCallExpression.class);
            Integer level = defCall == null ? null : defCall.getUseLevel();
            if (!checkLevel(true, false, level, def)) {
              isLemma = false;
            }
          }
        }
      }
    }

    if (newDef) {
      if (expectedType == null && def.isRecursive()) {
        expectedType = new ErrorExpression(null, null);
      }

      typedDef.setParameters(list.getFirst());
      typedDef.setResultType(expectedType);
      typedDef.setStatus(paramsOk && expectedType != null ? Definition.TypeCheckingStatus.BODY_NEEDS_TYPE_CHECKING : Definition.TypeCheckingStatus.HEADER_HAS_ERRORS);
      typedDef.setIsLemma(isLemma);
    }

    return paramsOk;
  }

  protected void checkElimBody(Concrete.FunctionDefinition def) {
    if (def.isRecursive() && !(def.getBody() instanceof Concrete.ElimFunctionBody)) {
      errorReporter.report(new TypecheckingError("Recursive functions must be defined by pattern matching", def));
    }
  }

  protected ClassCallExpression typecheckCoClauses(FunctionDefinition typedDef, Concrete.Definition def, Concrete.Expression resultType, Concrete.Expression resultTypeLevel, List<Concrete.ClassFieldImpl> classFieldImpls) {
    ClassCallExpression type;
    TypecheckingResult result;
    Set<ClassField> pseudoImplemented;
    if (typedDef.isLemma()) {
      TypecheckingResult typeResult = typechecker.finalCheckExpr(resultType, resultTypeLevel == null ? new UniverseExpression(Sort.PROP) : ExpectedType.OMEGA, false);
      if (typeResult == null || !(typeResult.expression instanceof ClassCallExpression)) {
        return null;
      }
      type = (ClassCallExpression) typeResult.expression;
      pseudoImplemented = new HashSet<>();
      result = typechecker.finalize(typechecker.typecheckClassExt(classFieldImpls, ExpectedType.OMEGA, null, type, pseudoImplemented, resultType), null, def);
      if (result == null || !(result.expression instanceof ClassCallExpression)) {
        return null;
      }
    } else {
      pseudoImplemented = Collections.emptySet();
      result = typechecker.finalCheckExpr(Concrete.ClassExtExpression.make(def.getData(), resultType, classFieldImpls), ExpectedType.OMEGA, false);
      if (result == null || !(result.expression instanceof ClassCallExpression)) {
        return null;
      }
      type = (ClassCallExpression) result.expression;
    }

    typechecker.checkAllImplemented((ClassCallExpression) result.expression, pseudoImplemented, def);
    return type;
  }
}
