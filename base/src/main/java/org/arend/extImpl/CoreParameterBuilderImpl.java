package org.arend.extImpl;

import org.arend.core.context.LinkList;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.context.param.TypedDependentLink;
import org.arend.core.expr.Expression;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.type.TypeExpression;
import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.core.context.CoreParameter;
import org.arend.ext.core.context.CoreParameterBuilder;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.visitor.CheckTypeVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CoreParameterBuilderImpl extends LinkList implements CoreParameterBuilder {
  private final CheckTypeVisitor myTypechecker;

  public CoreParameterBuilderImpl(CheckTypeVisitor typechecker) {
    myTypechecker = typechecker;
  }

  @Override
  public @NotNull DependentLink addCopyFirst(@NotNull CoreParameter parameter) {
    if (!(parameter instanceof DependentLink)) {
      throw new IllegalArgumentException();
    }
    DependentLink copy = DependentLink.Helper.copy((DependentLink) parameter);
    prepend(copy);
    return copy;
  }

  @Override
  public @NotNull DependentLink addCopyLast(@NotNull CoreParameter parameter) {
    if (!(parameter instanceof DependentLink)) {
      throw new IllegalArgumentException();
    }
    DependentLink copy = DependentLink.Helper.copy((DependentLink) parameter);
    append(copy);
    return copy;
  }

  private DependentLink setExplicit(DependentLink copy, boolean isExplicit) {
    for (DependentLink link = copy; link.hasNext(); link = link.getNext()) {
      link.setExplicit(isExplicit);
    }
    return copy;
  }

  @Override
  public @NotNull DependentLink addCopyFirst(@NotNull CoreParameter parameter, boolean isExplicit) {
    return setExplicit(addCopyFirst(parameter), isExplicit);
  }

  @Override
  public @NotNull DependentLink addCopyLast(@NotNull CoreParameter parameter, boolean isExplicit) {
    return setExplicit(addCopyLast(parameter), isExplicit);
  }

  private @NotNull DependentLink makeParameter(boolean isExplicit, @Nullable String name, @NotNull CoreExpression type, @NotNull ConcreteSourceNode marker) {
    if (!(type instanceof Expression && marker instanceof Concrete.SourceNode)) {
      throw new IllegalArgumentException();
    }
    return new TypedDependentLink(isExplicit, name, type instanceof Type ? (Type) type : new TypeExpression((Expression) type, myTypechecker.getSortOfType((Expression) type, (Concrete.SourceNode) marker)), EmptyDependentLink.getInstance());
  }

  @Override
  public @NotNull CoreParameter addFirst(boolean isExplicit, @Nullable String name, @NotNull CoreExpression type, @NotNull ConcreteSourceNode marker) {
    DependentLink param = makeParameter(isExplicit, name, type, marker);
    prepend(param);
    return param;
  }

  @Override
  public @NotNull CoreParameter addLast(boolean isExplicit, @Nullable String name, @NotNull CoreExpression type, @NotNull ConcreteSourceNode marker) {
    DependentLink param = makeParameter(isExplicit, name, type, marker);
    append(param);
    return param;
  }
}
