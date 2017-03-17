package com.jetbrains.jetpad.vclang.core.expr.type;

import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintable;

import java.util.List;

public interface Type extends PrettyPrintable {
  Type normalize(NormalizeVisitor.Mode mode);
  Type getPiParameters(List<SingleDependentLink> params, boolean normalize, boolean implicitOnly);
}
