package org.arend.naming.resolving.visitor;

import org.arend.ext.reference.Precedence;
import org.arend.naming.reference.*;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteReferableDefinitionVisitor;

import java.util.*;

public class TypeClassReferenceExtractVisitor implements ConcreteReferableDefinitionVisitor<Void, ClassReferable> {
  private int myArguments;

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
    handleParameters(parameters);
    if (myArguments < 0) {
      return null;
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
      if (expr instanceof Concrete.BinOpSequenceExpression && ((Concrete.BinOpSequenceExpression) expr).getClauses() == null) {
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

    return expr instanceof Concrete.ReferenceExpression ? RedirectingReferable.getOriginalReferable(((Concrete.ReferenceExpression) expr).getReferent()) : null;
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

  public boolean decrease(int count) {
    myArguments -= count;
    return myArguments >= 0;
  }

  public ClassReferable findClassReference(Referable referent) {
    Set<Referable> visited = null;
    while (true) {
      if (referent instanceof ClassReferable || referent == null) {
        return (ClassReferable) referent;
      }
      Referable underlyingRef = referent.getUnderlyingReferable();
      if (underlyingRef instanceof ClassReferable) {
        return (ClassReferable) underlyingRef;
      }
      if (!(underlyingRef instanceof TypedReferable)) {
        return null;
      }
      Referable ref = ((TypedReferable) underlyingRef).getBodyReference(this);
      if (!(ref instanceof GlobalReferable)) {
        return null;
      }

      if (visited == null) {
        visited = new HashSet<>();
      }
      if (!visited.add(underlyingRef)) {
        return null;
      }

      referent = ref;
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
