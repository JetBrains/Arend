package org.arend.ext.prettyprinting.doc;

import org.arend.ext.prettyprinting.PrettyPrintable;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class Doc implements PrettyPrintable {
  public abstract <P, R> R accept(DocVisitor<? super P, ? extends R> visitor, P params);

  public abstract int getWidth();
  public abstract int getHeight();
  public abstract boolean isNull(); // isNull() == (getHeight() == 0)
  public abstract boolean isSingleLine(); // isSingleLine() == (getHeight() <= 1)
  public abstract boolean isEmpty(); // isEmpty() == (getWidth() == 0)

  @NotNull
  public abstract List<LineDoc> linearize(int indent, boolean indentFirst); // linearize(n, b).size() == getHeight()

  @NotNull
  public final List<LineDoc> linearize() {
    return linearize(0, false);
  }

  @Override
  public String toString() {
    return DocStringBuilder.build(this);
  }

  @Override
  public void prettyPrint(StringBuilder builder, PrettyPrinterConfig ppConfig) {
    DocStringBuilder.build(builder, this);
  }

  @Override
  public Doc prettyPrint(PrettyPrinterConfig ppConfig) {
    return this;
  }
}
