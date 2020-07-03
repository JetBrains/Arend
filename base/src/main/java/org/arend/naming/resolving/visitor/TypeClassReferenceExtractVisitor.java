package org.arend.naming.resolving.visitor;

import org.arend.ext.reference.Precedence;
import org.arend.naming.reference.ClassReferable;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.RedirectingReferable;
import org.arend.naming.reference.Referable;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteReferableDefinitionVisitor;
import org.arend.typechecking.provider.ConcreteProvider;

import java.util.*;

public class TypeClassReferenceExtractVisitor implements ConcreteReferableDefinitionVisitor<Void, ClassReferable> {
  public final ConcreteProvider concreteProvider;
  private int myArguments;

  public TypeClassReferenceExtractVisitor(ConcreteProvider concreteProvider) {
    this.concreteProvider = concreteProvider;
  }

  @Override
  public ClassReferable visitFunction(Concrete.BaseFunctionDefinition def, Void params) {
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
  public ClassReferable visitConstructor(Concrete.Constructor def, Void params) {
    return null;
  }

  @Override
  public ClassReferable visitClassField(Concrete.ClassField def, Void params) {
    return getTypeClassReference(def.getParameters(), def.getResultType());
  }

  public Referable getTypeReference(Collection<? extends Concrete.Parameter> parameters, Concrete.Expression expr, boolean isType) {
    for (Concrete.Parameter parameter : parameters) {
      if (parameter.isExplicit()) {
        return null;
      }
    }

    if (isType) {
      while (true) {
        if (expr instanceof Concrete.PiExpression) {
          for (Concrete.TypeParameter parameter : ((Concrete.PiExpression) expr).getParameters()) {
            if (parameter.isExplicit()) {
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

    while (true) {
      if (expr instanceof Concrete.BinOpSequenceExpression) {
        List<Concrete.BinOpSequenceElem> binOpSeq = ((Concrete.BinOpSequenceExpression) expr).getSequence();
        Precedence minPrec = null;
        int index = -1;
        for (int i = 0; i < binOpSeq.size(); i++) {
          if (binOpSeq.get(i).isInfixReference() || binOpSeq.get(i).isPostfixReference()) {
            Precedence prec = binOpSeq.get(i).getReferencePrecedence();
            if (minPrec == null) {
              minPrec = prec;
              index = i;
            } else {
              if (prec.priority == minPrec.priority && (prec.associativity != minPrec.associativity || prec.associativity == Precedence.Associativity.NON_ASSOC)) {
                return null;
              }
              if (prec.priority < minPrec.priority || prec.priority == minPrec.priority && prec.associativity == Precedence.Associativity.LEFT_ASSOC) {
                minPrec = prec;
                index = i;
              }
            }
          }
        }
        if (index == -1) {
          expr = binOpSeq.get(0).expression;
          myArguments += binOpSeq.size() - 1;
        } else {
          if (index == 0 && myArguments == 0 && binOpSeq.get(index).isPostfixReference()) {
            return null;
          }
          myArguments++;
          for (int i = index + 1; i < binOpSeq.size(); i++) {
            if (binOpSeq.get(i).isExplicit) {
              myArguments++;
              break;
            }
          }
          expr = binOpSeq.get(index).expression;
        }
      } else if (expr instanceof Concrete.AppExpression) {
        for (Concrete.Argument argument : ((Concrete.AppExpression) expr).getArguments()) {
          if (argument.isExplicit()) {
            myArguments++;
          }
        }
        expr = ((Concrete.AppExpression) expr).getFunction();
      } else {
        break;
      }
    }

    Referable ref = expr instanceof Concrete.ReferenceExpression ? RedirectingReferable.getOriginalReferable(((Concrete.ReferenceExpression) expr).getReferent()) : null;
    return ref instanceof ClassReferable || ref == null ? ref : ref.getUnderlyingReferable();
  }

  private void handleParameters(Collection<? extends Concrete.Parameter> parameters) {
    for (Concrete.Parameter parameter : parameters) {
      if (parameter.isExplicit()) {
        myArguments -= parameter.getNumberOfParameters();
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
      Concrete.FunctionDefinition function = concreteProvider.getConcreteFunction((GlobalReferable) referent);
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
    if (type == null) {
      return null;
    }

    myArguments = 0;
    return findClassReference(getTypeReference(parameters, type, true));
  }
}
