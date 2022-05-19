package org.arend.core.context.param;

import org.arend.core.expr.ReferenceExpression;
import org.arend.core.expr.type.Type;
import org.arend.core.subst.SubstVisitor;

public class SigmaTypedDependentLink extends TypedDependentLink {
    private final boolean myProperty;

    public SigmaTypedDependentLink(String name, Type type, DependentLink next, boolean isProperty) {
        super(true, name, type, false, next);
        this.myProperty = isProperty;
    }

    public boolean isProperty() {
        return myProperty;
    }

    @Override
    public DependentLink subst(SubstVisitor substVisitor, int size, boolean updateSubst) {
        if (size > 0) {
            TypedDependentLink result = new SigmaTypedDependentLink( getName(), getType().subst(substVisitor), EmptyDependentLink.getInstance(), isProperty());
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
