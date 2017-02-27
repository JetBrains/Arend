package com.jetbrains.jetpad.vclang.core.expr.type;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.sort.SortMax;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintable;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.List;
import java.util.Set;

public interface TypeMax extends PrettyPrintable {
  TypeMax subst(ExprSubstitution exprSubst, LevelSubstitution levelSubst);
  TypeMax applyExpressions(List<? extends Expression> expressions);
  boolean isLessOrEquals(Sort sort);
  boolean isLessOrEquals(TypeMax type, Equations equations, Abstract.SourceNode sourceNode);
  SortMax toSorts();
  TypeMax getPiParameters(List<DependentLink> params, boolean normalize, boolean implicitOnly);
  TypeMax fromPiParameters(List<DependentLink> params);
  TypeMax addParameters(DependentLink params, boolean modify);
  DependentLink getPiParameters();
  TypeMax getPiCodomain();
  TypeMax normalize(NormalizeVisitor.Mode mode);
  TypeMax strip(Set<Binding> bounds, LocalErrorReporter errorReporter);
  Expression toExpression();
  TypeMax getType();
  boolean findBinding(Variable binding);
}
