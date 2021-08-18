package org.arend.term.prettyprint;

import org.arend.naming.reference.Referable;
import org.arend.term.concrete.BaseConcreteExpressionVisitor;
import org.arend.term.concrete.Concrete;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FreeVariableCollectorConcrete extends BaseConcreteExpressionVisitor<Void> {
    private final Set<Referable> referables;

    public FreeVariableCollectorConcrete(Set<Referable> referables) {
        this.referables = referables;
    }


    @Override
    public Concrete.Expression visitReference(Concrete.ReferenceExpression expr, Void params) {
        referables.add(expr.getReferent());
        return expr;
    }

    @Override
    public Concrete.Expression visitLam(Concrete.LamExpression expr, Void params) {
        var result = super.visitLam(expr, params);
        removeParameters(expr.getParameters());
        return result;
    }

    private void removeParameters(List<? extends Concrete.Parameter> parameters) {
        parameters.stream().flatMap(a -> a.getRefList().stream()).collect(Collectors.toList()).forEach(referables::remove);
    }

    @Override
    public Concrete.Expression visitPi(Concrete.PiExpression expr, Void params) {
        Concrete.Expression result = super.visitPi(expr, params);
        removeParameters(expr.getParameters());
        return result;
    }

    @Override
    public Concrete.Expression visitLet(Concrete.LetExpression expr, Void params) {
        Concrete.Expression result = super.visitLet(expr, params);
        expr.getClauses().forEach(clause -> {
            removeParameters(clause.getParameters());
            var typedReferable = clause.getPattern().getAsReferable();
            if (typedReferable != null) {
                referables.remove(typedReferable.referable);
            }
        });
        return result;
    }

    @Override
    public Concrete.Expression visitSigma(Concrete.SigmaExpression expr, Void params) {
        Concrete.Expression result = super.visitSigma(expr, params);
        removeParameters(expr.getParameters());
        return result;
    }
}
