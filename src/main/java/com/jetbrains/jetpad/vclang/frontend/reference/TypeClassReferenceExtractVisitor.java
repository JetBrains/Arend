package com.jetbrains.jetpad.vclang.frontend.reference;

import com.jetbrains.jetpad.vclang.naming.reference.ClassReferable;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.TypedReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.concrete.ConcreteReferableDefinitionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider.ConcreteProvider;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TypeClassReferenceExtractVisitor implements ConcreteReferableDefinitionVisitor<Void, ClassReferable> {
  private final ConcreteProvider myConcreteProvider;
  private int myArguments;

  public TypeClassReferenceExtractVisitor(ConcreteProvider concreteProvider) {
    myConcreteProvider = concreteProvider;
  }

  @Override
  public ClassReferable visitFunction(Concrete.FunctionDefinition def, Void params) {
    return getTypeClassReference(def.getParameters(), def.getResultType());
  }

  @Override
  public ClassReferable visitData(Concrete.DataDefinition def, Void params) {
    return null;
  }

  @Override
  public ClassReferable visitClass(Concrete.ClassDefinition def, Void params) {
    return null;
  }

  @Override
  public ClassReferable visitClassSynonym(Concrete.ClassSynonym def, Void params) {
    return null;
  }

  @Override
  public ClassReferable visitInstance(Concrete.Instance def, Void params) {
    return getTypeClassReference(def.getParameters(), def.getClassReference());
  }

  @Override
  public ClassReferable visitConstructor(Concrete.Constructor def, Void params) {
    return null;
  }

  @Override
  public ClassReferable visitClassField(Concrete.ClassField def, Void params) {
    return getTypeClassReference(Collections.emptyList(), def.getResultType());
  }

  @Override
  public ClassReferable visitClassFieldSynonym(Concrete.ClassFieldSynonym def, Void params) {
    Referable fieldRef = def.getUnderlyingField().getReferent();
    return fieldRef instanceof TypedReferable ? ((TypedReferable) fieldRef).getTypeClassReference() : null;
  }

  private Referable getTypeReference(Collection<? extends Concrete.Parameter> parameters, Concrete.Expression expr, boolean isType) {
    for (Concrete.Parameter parameter : parameters) {
      if (parameter.getExplicit()) {
        return null;
      }
    }

    if (isType) {
      while (true) {
        if (expr instanceof Concrete.PiExpression) {
          for (Concrete.TypeParameter parameter : ((Concrete.PiExpression) expr).getParameters()) {
            if (parameter.getExplicit()) {
              return null;
            }
          }
          expr = ((Concrete.PiExpression) expr).getCodomain();
        } else if (expr instanceof Concrete.ClassExtExpression) {
          expr = ((Concrete.ClassExtExpression) expr).getBaseClassExpression();
        } else {
          break;
        }
      }
    } else {
      while (true) {
        if (expr instanceof Concrete.LamExpression) {
          handleParameters(((Concrete.LamExpression) expr).getParameters());
          if (myArguments < 0) {
            return null;
          }
          expr = ((Concrete.LamExpression) expr).getBody();
        } else if (expr instanceof Concrete.ClassExtExpression) {
          expr = ((Concrete.ClassExtExpression) expr).getBaseClassExpression();
        } else {
          break;
        }
      }
    }

    if (expr instanceof Concrete.AppExpression) {
      myArguments += ((Concrete.AppExpression) expr).getArguments().size();
      expr = ((Concrete.AppExpression) expr).getFunction();
    }

    return expr instanceof Concrete.ReferenceExpression ? ((Concrete.ReferenceExpression) expr).getReferent() : null;
  }

  private void handleParameters(Collection<? extends Concrete.Parameter> parameters) {
    for (Concrete.Parameter parameter : parameters) {
      if (parameter.getExplicit()) {
        if (parameter instanceof Concrete.TelescopeParameter) {
          myArguments -= ((Concrete.TelescopeParameter) parameter).getReferableList().size();
        } else {
          myArguments--;
        }
        if (myArguments < 0) {
          return;
        }
      }
    }
  }

  private ClassReferable findClassReference(Referable referent) {
    Set<GlobalReferable> visited = null;
    while (true) {
      if (referent instanceof ClassReferable) {
        return (ClassReferable) referent;
      }
      if (!(referent instanceof GlobalReferable)) {
        return null;
      }
      Concrete.FunctionDefinition function = myConcreteProvider.getConcreteFunction((GlobalReferable) referent);
      if (function == null) {
        return null;
      }
      if (!(function.getBody() instanceof Concrete.TermFunctionBody)) {
        return null;
      }

      Concrete.Expression term = ((Concrete.TermFunctionBody) function.getBody()).getTerm();
      handleParameters(function.getParameters());
      if (myArguments < 0) {
        return null;
      }

      if (visited == null) {
        visited = new HashSet<>();
      }
      if (!visited.add((GlobalReferable) referent)) {
        return null;
      }

      referent = getTypeReference(Collections.emptyList(), term, false);
    }
  }

  public ClassReferable getTypeClassReference(Collection<? extends Concrete.Parameter> parameters, Concrete.Expression type) {
    myArguments = 0;
    return findClassReference(getTypeReference(parameters, type, true));
  }
}
