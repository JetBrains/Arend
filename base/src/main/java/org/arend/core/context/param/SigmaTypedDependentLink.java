package org.arend.core.context.param;

import org.arend.core.expr.ReferenceExpression;
import org.arend.core.expr.type.Type;
import org.arend.core.subst.SubstVisitor;
import org.arend.ext.concrete.expr.SigmaFieldKind;

public class SigmaTypedDependentLink extends TypedDependentLink {
    private final SigmaFieldKind explicitKindMark;

    public SigmaTypedDependentLink(String name, Type type, DependentLink next, SigmaFieldKind explicitKindMark) {
        super(true, name, type, false, next);
        this.explicitKindMark = explicitKindMark;
    }

    public SigmaFieldKind getFieldKind() {
        return explicitKindMark;
    }

    @Override
    public DependentLink subst(SubstVisitor substVisitor, int size, boolean updateSubst) {
        if (size > 0) {
            TypedDependentLink result = new SigmaTypedDependentLink( getName(), getType().subst(substVisitor), EmptyDependentLink.getInstance(), explicitKindMark);
            if (updateSubst) {
                substVisitor.getExprSubstitution().addSubst(this, new ReferenceExpression(result));
            } else {
                substVisitor.getExprSubstitution().add(this, new ReferenceExpression(result));
            }
            result.setNext(getNext().subst(substVisitor, size - 1, updateSubst));
            return result;
        } else {
            return EmptyDependentLink.getInstance();
        }
    }
}
