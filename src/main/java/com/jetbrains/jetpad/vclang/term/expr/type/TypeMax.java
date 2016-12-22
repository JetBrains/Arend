package com.jetbrains.jetpad.vclang.term.expr.type;

import com.jetbrains.jetpad.vclang.prettyprint.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.Referable;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.sort.Sort;
import com.jetbrains.jetpad.vclang.term.expr.sort.SortMax;
import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.List;
import java.util.Set;

public interface TypeMax extends PrettyPrintable {
  TypeMax subst(ExprSubstitution exprSubst, LevelSubstitution levelSubst);
  TypeMax applyExpressions(List<? extends Expression> expressions);
  boolean isLessOrEquals(Sort sort);
  boolean isLessOrEquals(Type type, Equations equations, Abstract.SourceNode sourceNode);
  SortMax toSorts();
  TypeMax getPiParameters(List<DependentLink> params, boolean normalize, boolean implicitOnly);
  TypeMax fromPiParameters(List<DependentLink> params);
  TypeMax addParameters(DependentLink params, boolean modify);
  DependentLink getPiParameters();
  TypeMax getPiCodomain();
  TypeMax normalize(NormalizeVisitor.Mode mode);
  TypeMax strip(Set<Binding> bounds, LocalErrorReporter errorReporter);
  Expression toExpression();
  boolean findBinding(Referable binding);
}
