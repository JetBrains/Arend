package org.arend.typechecking.visitor;

import org.arend.core.context.LinkList;
import org.arend.core.context.binding.Binding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.context.param.TypedDependentLink;
import org.arend.core.definition.*;
import org.arend.core.expr.*;
import org.arend.core.expr.type.ExpectedType;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.type.TypeExpression;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.naming.reference.HiddenLocalReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCClassReferable;
import org.arend.naming.reference.TCReferable;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.LocalErrorReporter;
import org.arend.typechecking.error.local.TypecheckingError;
import org.arend.typechecking.implicitargs.equations.DummyEquations;
import org.arend.typechecking.implicitargs.equations.Equations;
import org.arend.typechecking.instance.pool.LocalInstancePool;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.arend.core.expr.ExpressionFactory.parameter;

public abstract class BaseTypechecker {
  protected LocalErrorReporter errorReporter;

  protected abstract Type checkType(Concrete.Expression expr, ExpectedType expectedType, boolean isFinal);

  protected abstract void addBinding(@Nullable Referable referable, Binding binding);

  protected abstract Definition getTypechecked(TCReferable referable);

  protected abstract Integer getExpressionLevel(DependentLink link, Expression type, Expression expr, Equations equations, Concrete.SourceNode sourceNode);

  protected Sort typecheckParameters(Concrete.ReferableDefinition def, LinkList list, LocalInstancePool localInstancePool, Sort expectedSort, DependentLink oldParameters) {
    Sort sort = Sort.PROP;

    if (oldParameters != null) {
      list.append(oldParameters);
    }

    for (Concrete.TypeParameter parameter : Objects.requireNonNull(Concrete.getParameters(def, true))) {
      Type paramResult = checkType(parameter.getType(), expectedSort == null ? ExpectedType.OMEGA : new UniverseExpression(expectedSort), true);
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
            addBinding(referableList.get(i), link);
          }
        } else {
          for (int i = 0; i < names.size(); i++) {
            if (oldParameters.hasNext()) {
              addBinding(referableList.get(i), oldParameters);
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
            addBinding(null, oldParameters);
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
          ClassDefinition classDef = (ClassDefinition) getTypechecked(classRef);
          if (classDef != null && !classDef.isRecord()) {
            ClassField classifyingField = classDef.getClassifyingField();
            int i = 0;
            for (DependentLink link = param; i < numberOfParameters; link = link.getNext(), i++) {
              ReferenceExpression reference = new ReferenceExpression(link);
              // Expression oldInstance =
              localInstancePool.addInstance(classifyingField == null ? null : FieldCallExpression.make(classifyingField, paramResult.getSortOfType(), reference), classRef, reference, parameter);
              // if (oldInstance != null) {
              //   errorReporter.report(new DuplicateInstanceError(oldInstance, reference, parameter));
              // }
            }
          }
        }
      }

      if (oldParameters == null) {
        list.append(param);
        for (; param.hasNext(); param = param.getNext()) {
          addBinding(null, param);
        }
      }
    }

    return sort;
  }

  protected enum PropLevel { YES, NO, COULD_BE }

  protected PropLevel isPropLevel(Concrete.Expression expression) {
    Referable fun = expression == null ? null : expression.getUnderlyingReferable();
    if (fun instanceof TCReferable) {
      Definition typeDef = getTypechecked((TCReferable) fun);
      if (typeDef != null) {
        boolean couldBe = false;
        for (Definition.ParametersLevel parametersLevel : typeDef.getParametersLevels()) {
          if (parametersLevel.level == -1) {
            if (parametersLevel.parameters == null) {
              return PropLevel.YES;
            }
            couldBe = true;
          }
        }
        if (couldBe) {
          return PropLevel.COULD_BE;
        }
      } else {
        return PropLevel.COULD_BE;
      }
    }
    return PropLevel.NO;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  protected boolean checkLevel(boolean isLemma, boolean isProperty, Integer level, Concrete.SourceNode sourceNode) {
    if ((isLemma || isProperty) && (level == null || level != -1)) {
      errorReporter.report(new TypecheckingError("The level of a " + (isLemma ? "lemma" : "property") + " must be \\Prop", sourceNode));
      return false;
    } else {
      return true;
    }
  }

  protected void typecheckFunctionHeader(FunctionDefinition typedDef, Concrete.FunctionDefinition def, LocalInstancePool localInstancePool, boolean newDef) {
    LinkList list = new LinkList();

    if (def.getResultTypeLevel() != null && !(def.getKind() == Concrete.FunctionDefinition.Kind.LEMMA || def.getBody() instanceof Concrete.ElimFunctionBody)) {
      errorReporter.report(new TypecheckingError("\\level is allowed only for lemmas and functions defined by pattern matching", def.getResultTypeLevel()));
      def.setResultTypeLevel(null);
    }

    boolean paramsOk = typecheckParameters(def, list, localInstancePool, null, newDef || typedDef == null ? null : typedDef.getParameters()) != null;

    Expression expectedType = null;
    Concrete.Expression cResultType = def.getResultType();
    boolean isLemma = def.getKind() == Concrete.FunctionDefinition.Kind.LEMMA || def.getKind() == Concrete.FunctionDefinition.Kind.LEVEL;
    if (cResultType != null) {
      PropLevel propLevel = isPropLevel(cResultType);
      boolean needProp = def.getKind() == Concrete.FunctionDefinition.Kind.LEMMA && def.getResultTypeLevel() == null;
      ExpectedType typeExpectedType = needProp && propLevel == PropLevel.NO ? new UniverseExpression(Sort.PROP) : ExpectedType.OMEGA;
      Type expectedTypeResult;
      if (def.getBody() instanceof Concrete.CoelimFunctionBody) {
        expectedTypeResult = null;
      } else if (def.getBody() instanceof Concrete.TermFunctionBody && !def.isRecursive() && !isLemma) {
        expectedTypeResult = checkType(cResultType, typeExpectedType, false);
      } else {
        expectedTypeResult = checkType(cResultType, typeExpectedType, true);
      }
      if (expectedTypeResult != null) {
        expectedType = expectedTypeResult.getExpr();
        if (needProp && propLevel == PropLevel.COULD_BE) {
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
      typedDef.setParameters(list.getFirst());
      typedDef.setResultType(expectedType);
      typedDef.setStatus(paramsOk && expectedType != null ? Definition.TypeCheckingStatus.BODY_NEEDS_TYPE_CHECKING : Definition.TypeCheckingStatus.HEADER_HAS_ERRORS);
      typedDef.setIsLemma(isLemma);
    }

    if (def.isRecursive() && expectedType == null) {
      errorReporter.report(new TypecheckingError(def.getBody() instanceof Concrete.CoelimFunctionBody
          ? "Function defined by copattern matching cannot be recursive"
          : "Cannot infer the result type of a recursive function", def));
    }

    if (paramsOk && def.getKind() == Concrete.FunctionDefinition.Kind.LEVEL) {
      Definition useParent = getTypechecked(def.getUseParent());
      if (useParent instanceof DataDefinition || useParent instanceof ClassDefinition || useParent instanceof FunctionDefinition) {
        boolean ok = true;
        List<ClassField> levelFields = null;
        Expression type = null;
        DependentLink parameters = null;
        DependentLink link = list.getFirst();
        if (useParent instanceof DataDefinition || useParent instanceof FunctionDefinition) {
          ExprSubstitution substitution = new ExprSubstitution();
          List<Expression> defCallArgs = new ArrayList<>();
          for (DependentLink defLink = useParent.getParameters(); defLink.hasNext(); defLink = defLink.getNext(), link = link.getNext()) {
            if (!link.hasNext()) {
              ok = false;
              break;
            }
            if (!Expression.compare(link.getTypeExpr(), defLink.getTypeExpr().subst(substitution), Equations.CMP.EQ)) { // TODO
              if (parameters == null) {
                parameters = DependentLink.Helper.take(list.getFirst(), DependentLink.Helper.size(defLink));
              }
            }
            ReferenceExpression refExpr = new ReferenceExpression(link);
            defCallArgs.add(refExpr);
            substitution.add(defLink, refExpr);
          }

          if (ok) {
            if (link.hasNext()) {
              type = useParent instanceof DataDefinition
                  ? new DataCallExpression((DataDefinition) useParent, Sort.STD, defCallArgs)
                  : new FunCallExpression((FunctionDefinition) useParent, Sort.STD, defCallArgs);
            } else {
              ok = false;
            }
          }
        } else {
          ClassCallExpression classCall = null;
          DependentLink classCallLink = link;
          for (; classCallLink.hasNext(); classCallLink = classCallLink.getNext()) {
            classCallLink = classCallLink.getNextTyped(null);
            classCall = classCallLink.getTypeExpr().checkedCast(ClassCallExpression.class);
            if (classCall != null && classCall.getDefinition() == useParent && (!classCall.hasUniverses() || classCall.getSortArgument().equals(Sort.STD))) {
              break;
            }
          }
          if (!classCallLink.hasNext() && expectedType != null) {
            PiExpression piType = expectedType.normalize(NormalizeVisitor.Mode.WHNF).checkedCast(PiExpression.class); // TODO
            if (piType != null) {
              classCall = piType.getParameters().getTypeExpr().normalize(NormalizeVisitor.Mode.WHNF).checkedCast(ClassCallExpression.class); // TODO
              if (classCall != null && classCall.getDefinition() == useParent && (!classCall.hasUniverses() || classCall.getSortArgument().equals(Sort.STD))) {
                classCallLink = piType.getParameters();
              }
            }
          }

          if (classCall == null || !classCallLink.hasNext()) {
            ok = false;
          } else {
            if (!classCall.getImplementedHere().isEmpty()) {
              levelFields = new ArrayList<>();
              Expression thisExpr = new ReferenceExpression(classCallLink);
              for (ClassField classField : classCall.getDefinition().getFields()) {
                Expression impl = classCall.getImplementationHere(classField);
                if (impl == null) {
                  continue;
                }
                if (!(link.hasNext() && impl instanceof ReferenceExpression && ((ReferenceExpression) impl).getBinding() == link)) {
                  ok = false;
                  break;
                }
                levelFields.add(classField);
                if (!classField.getType(Sort.STD).applyExpression(thisExpr).equals(link.getTypeExpr())) { // TODO
                  if (parameters == null) {
                    int numberOfClassParameters = 0;
                    for (DependentLink link1 = link; link1 != classCallLink && link1.hasNext(); link1 = link1.getNext()) {
                      numberOfClassParameters++;
                    }
                    parameters = DependentLink.Helper.take(list.getFirst(), numberOfClassParameters);
                  }
                }
                link = link.getNext();
              }
            }
            type = classCall;
          }
        }

        Integer level = getExpressionLevel(link, expectedType, ok ? type : null, DummyEquations.getInstance(), def);
        if (level != null && newDef) {
          if (useParent instanceof DataDefinition) {
            if (parameters == null) {
              ((DataDefinition) useParent).setSort(level == -1 ? Sort.PROP : new Sort(((DataDefinition) useParent).getSort().getPLevel(), new Level(level)));
            } else {
              ((DataDefinition) useParent).addLevelParameters(new Definition.ParametersLevel(parameters, level));
            }
          } else if (useParent instanceof FunctionDefinition) {
            ((FunctionDefinition) useParent).addLevelParameters(new Definition.ParametersLevel(parameters, level));
          } else {
            if (levelFields == null) {
              ((ClassDefinition) useParent).setSort(level == -1 ? Sort.PROP : new Sort(((ClassDefinition) useParent).getSort().getPLevel(), new Level(level)));
            } else {
              ((ClassDefinition) useParent).addParametersLevel(new ClassDefinition.ParametersLevel(parameters, level, levelFields));
            }
          }
        }
      }
    }
  }
}
