package org.arend.core.expr.visitor;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.TypedBinding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.expr.*;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.core.subst.SubstVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReplaceBindingVisitor extends SubstVisitor {
  private final Binding myBinding;
  private final ClassCallExpression myBindingType;
  private final Map<ClassField, AbsExpression> myImplementations;
  private final Expression myRenewExpression;
  private boolean myOK = true;

  private ReplaceBindingVisitor(Binding binding, ClassCallExpression bindingType, Map<ClassField, AbsExpression> implementations, Expression renewExpr) {
    super(new ExprSubstitution(), LevelSubstitution.EMPTY);
    myBinding = binding;
    myBindingType = bindingType;
    myImplementations = implementations;
    myRenewExpression = renewExpr;
  }

  public ReplaceBindingVisitor(Binding binding, ClassCallExpression bindingType, Expression renewExpr) {
    this(binding, bindingType, new HashMap<>(bindingType.getImplementedHere()), renewExpr);
  }

  public boolean isOK() {
    return myOK;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public Expression visitReference(ReferenceExpression expr, Void params) {
    if (expr.getBinding() == myBinding) {
      if (myRenewExpression != null) {
        return myRenewExpression;
      }
      myOK = false;
      return expr;
    }
    return super.visitReference(expr, params);
  }

  private Expression checkArgument(Expression arg, Expression type) {
    ReferenceExpression refArg = arg.cast(ReferenceExpression.class);
    if (refArg != null && refArg.getBinding() == myBinding) {
      ClassCallExpression argType = type.normalize(NormalizeVisitor.Mode.WHNF).cast(ClassCallExpression.class);
      if (argType != null && argType.getDefinition() != myBindingType.getDefinition()) {
        Map<ClassField, AbsExpression> implementations = new HashMap<>();
        List<? extends ClassField> fieldOrder = argType.getDefinition().getTypecheckingFieldOrder();
        if (fieldOrder == null) {
          return arg.accept(this, null);
        }
        for (ClassField field : fieldOrder) {
          AbsExpression impl = myImplementations.computeIfAbsent(field, e -> {
            AbsExpression absImpl = myBindingType.getDefinition().getImplementation(field);
            if (absImpl != null) {
              if (absImpl.getBinding() == null) {
                return new AbsExpression(null, absImpl.getExpression());
              } else {
                ReplaceBindingVisitor visitor = new ReplaceBindingVisitor(absImpl.getBinding(), myBindingType, myImplementations, null);
                Expression impl1 = absImpl.getExpression().accept(visitor, null);
                if (visitor.isOK()) {
                  return new AbsExpression(null, impl1);
                }
              }
            }
            return null;
          });

          if (impl != null) {
            implementations.put(field, impl);
          } else {
            return arg.accept(this, null);
          }
        }

        return new NewExpression(null, new ClassCallExpression(argType.getDefinition(), myBindingType.getSortArgument(), implementations, Sort.PROP, false));
      }
    }

    return arg.accept(this, null);
  }

  @Override
  public Expression visitDefCall(DefCallExpression expr, Void params) {
    DependentLink link = expr.getDefinition().getParameters();
    List<Expression> args = new ArrayList<>(expr.getDefCallArguments().size());
    for (Expression arg : expr.getDefCallArguments()) {
      args.add(checkArgument(arg, link.getTypeExpr()));
      link = link.getNext();
    }
    return expr.getDefinition().getDefCall(expr.getSortArgument(), args);
  }

  @Override
  public ConCallExpression visitConCall(ConCallExpression expr, Void params) {
    DependentLink dataLink = expr.getDefinition().getDataTypeParameters();
    List<Expression> dataTypeArgs = new ArrayList<>(expr.getDataTypeArguments().size());
    for (Expression arg : expr.getDataTypeArguments()) {
      dataTypeArgs.add(checkArgument(arg, dataLink.getTypeExpr()));
      dataLink = dataLink.getNext();
    }

    DependentLink link = expr.getDefinition().getParameters();
    List<Expression> args = new ArrayList<>(expr.getDefCallArguments().size());
    for (Expression arg : expr.getDefCallArguments()) {
      args.add(checkArgument(arg, link.getTypeExpr()));
      link = link.getNext();
    }

    return new ConCallExpression(expr.getDefinition(), expr.getSortArgument(), dataTypeArgs, args);
  }

  @Override
  public ClassCallExpression visitClassCall(ClassCallExpression expr, Void params) {
    Map<ClassField, AbsExpression> fieldSet = new HashMap<>();
    for (Map.Entry<ClassField, AbsExpression> entry : expr.getImplementedHere().entrySet()) {
      TypedBinding newBinding = new TypedBinding(entry.getValue().getBinding().getName(), entry.getValue().getBinding().getTypeExpr().accept(this, null));
      getExprSubstitution().add(entry.getValue().getBinding(), new ReferenceExpression(newBinding));
      fieldSet.put(entry.getKey(), new AbsExpression(newBinding, checkArgument(entry.getValue().getExpression(), entry.getKey().getType(Sort.STD).getCodomain())));
      getExprSubstitution().remove(entry.getValue().getBinding());
    }
    return new ClassCallExpression(expr.getDefinition(), expr.getSortArgument(), fieldSet, expr.getSort(), expr.hasUniverses());
  }

  @Override
  public Expression visitFieldCall(FieldCallExpression expr, Void params) {
    ReferenceExpression refExpr = expr.getArgument().cast(ReferenceExpression.class);
    if (refExpr != null && refExpr.getBinding() == myBinding) {
      Expression impl = myBindingType.getImplementation(expr.getDefinition(), refExpr);
      if (impl != null) {
        return impl.accept(this, null);
      }
    }
    return super.visitFieldCall(expr, params);
  }
}
