package org.arend.naming.resolving.visitor;

import org.arend.ext.reference.Precedence;
import org.arend.ext.typechecking.MetaDefinition;
import org.arend.naming.reference.*;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteReferableDefinitionVisitor;
import org.arend.term.concrete.DefinableMetaDefinition;

import java.util.*;

public class TypeClassReferenceExtractVisitor implements ConcreteReferableDefinitionVisitor<Void, ClassReferable> {
  @Override
  public ClassReferable visitFunction(Concrete.BaseFunctionDefinition def, Void params) {
    return getTypeClassReference(def.getResultType());
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
    return getTypeClassReference(def.getResultType());
  }

  public Concrete.ReferenceExpression getTypeReferenceExpression(Concrete.Expression expr, boolean isType) {
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
        List<Concrete.BinOpSequenceElem<Concrete.Expression>> binOpSeq = ((Concrete.BinOpSequenceExpression) expr).getSequence();
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
          expr = binOpSeq.get(0).getComponent();
        } else {
          for (int i = index + 1; i < binOpSeq.size(); i++) {
            if (binOpSeq.get(i).isExplicit) {
              break;
            }
          }
          expr = binOpSeq.get(index).getComponent();
        }
      } else if (expr instanceof Concrete.AppExpression) {
        expr = ((Concrete.AppExpression) expr).getFunction();
      } else {
        break;
      }
    }

    if (!(expr instanceof Concrete.ReferenceExpression)) {
      return null;
    }

    Referable ref = RedirectingReferable.getOriginalReferable(((Concrete.ReferenceExpression) expr).getReferent());
    if (ref instanceof MetaReferable) {
      MetaDefinition def = ((MetaReferable) ref).getDefinition();
      if (def instanceof DefinableMetaDefinition) {
        return getTypeReferenceExpression(((DefinableMetaDefinition) def).body, isType);
      }
    }
    return (Concrete.ReferenceExpression) expr;
  }

  public Referable getTypeReference(Concrete.Expression expr, boolean isType) {
    Concrete.ReferenceExpression refExpr = getTypeReferenceExpression(expr, isType);
    return refExpr == null ? null : RedirectingReferable.getOriginalReferable(refExpr.getReferent());
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

  public ClassReferable getTypeClassReference(Concrete.Expression type) {
    return type == null ? null : findClassReference(getTypeReference(type, true));
  }
}
