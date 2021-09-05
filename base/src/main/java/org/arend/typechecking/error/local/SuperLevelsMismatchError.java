package org.arend.typechecking.error.local;

import org.arend.core.definition.ClassDefinition;
import org.arend.core.expr.ClassCallExpression;
import org.arend.core.subst.Levels;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.PrettyPrinterConfigImpl;
import org.arend.ext.prettyprinting.PrettyPrinterFlag;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;
import static org.arend.ext.prettyprinting.doc.DocFactory.text;

public class SuperLevelsMismatchError extends TypecheckingError {
  public final ClassDefinition superClass;
  public final Levels previousLevels;
  public final Levels currentLevels;

  public SuperLevelsMismatchError(ClassDefinition superClass, Levels previousLevels, Levels currentLevels, @NotNull Concrete.ReferenceExpression cause) {
    super("", cause);
    this.superClass = superClass;
    this.previousLevels = previousLevels;
    this.currentLevels = currentLevels;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig ppConfig) {
    return hList(text("Levels of super class '"), refDoc(superClass.getRef()), text("' do not match"));
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    PrettyPrinterConfigImpl newConfig = new PrettyPrinterConfigImpl(ppConfig);
    newConfig.expressionFlags = newConfig.expressionFlags.clone();
    newConfig.expressionFlags.add(PrettyPrinterFlag.SHOW_LEVELS);
    return vList(
      hList(text("Previous levels: "), termLine(new ClassCallExpression(superClass, previousLevels), newConfig)),
      hList(text("Current  levels: "), termLine(new ClassCallExpression(superClass, currentLevels), newConfig)));
  }
}
