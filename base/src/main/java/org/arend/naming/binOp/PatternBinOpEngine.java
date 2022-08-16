package org.arend.naming.binOp;

import org.arend.ext.error.ErrorReporter;
import org.arend.ext.util.Pair;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.Referable;
import org.arend.term.Fixity;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PatternBinOpEngine implements BinOpEngine<Concrete.Pattern> {

  private static final PatternBinOpEngine engine = new PatternBinOpEngine();

  private PatternBinOpEngine() {
  }

  @Override
  public @Nullable Referable getReferable(@NotNull Concrete.Pattern elem) {
    return elem instanceof Concrete.NamePattern ? ((Concrete.NamePattern) elem).getRef() : null;
  }

  @Override
  public @NotNull Concrete.Pattern wrapSequence(Object data, Concrete.@NotNull Pattern base, List<@NotNull Pair<? extends Concrete.Pattern, Boolean>> explicitComponents) {
    if (base instanceof Concrete.ConstructorPattern) {
      ArrayList<Concrete.Pattern> newPatterns = new ArrayList<>(base.getPatterns());
      for (Pair<? extends Concrete.Pattern, Boolean> comp : explicitComponents) {
        newPatterns.add(comp.proj1);
      }
      return new Concrete.ConstructorPattern(data, ((Concrete.ConstructorPattern) base).getConstructor(), newPatterns, base.getAsReferable());
    } else if (base instanceof Concrete.UnparsedConstructorPattern || base instanceof Concrete.NumberPattern || base instanceof Concrete.TuplePattern) {
      ArrayList<Concrete.BinOpSequenceElem<Concrete.Pattern>> newPatterns = new ArrayList<>(List.of(new Concrete.BinOpSequenceElem<>(base, Fixity.NONFIX, base.isExplicit())));
      for (Pair<? extends Concrete.Pattern, Boolean> comp : explicitComponents) {
        newPatterns.add(new Concrete.BinOpSequenceElem<>(comp.proj1, Fixity.NONFIX, comp.proj2));
      }
      return new Concrete.UnparsedConstructorPattern(data, base.isExplicit(), newPatterns, base.getAsReferable());
    } else {
      return new Concrete.ConstructorPattern(data, getReferable(base), explicitComponents.stream().map((pair) -> pair.proj1).collect(Collectors.toList()), base.getAsReferable());
    }
  }

  @Override
  public @NotNull Concrete.Pattern augmentWithLeftReferable(Object data, @NotNull Referable leftRef, Concrete.@NotNull Pattern mid, Concrete.@NotNull Pattern right) {
    Referable innerReferable = getReferableInner(mid);
    if (innerReferable instanceof GlobalReferable) {
      // fallback to prefix form
      List<Pair<? extends Concrete.Pattern, Boolean>> patterns;
      if (right instanceof Concrete.ConstructorPattern && !(((Concrete.ConstructorPattern) right).getConstructor() instanceof GlobalReferable)) {
        patterns = new ArrayList<>(Collections.singleton(Pair.create(new Concrete.NamePattern(right.getData(), right.isExplicit(), ((Concrete.ConstructorPattern) right).getConstructor(), null), right.isExplicit())));
        patterns.addAll(right.getPatterns().stream().map(pt -> Pair.create(pt, pt.isExplicit())).collect(Collectors.toList()));
      } else if (right instanceof Concrete.UnparsedConstructorPattern) {
        patterns = ((Concrete.UnparsedConstructorPattern) right).getUnparsedPatterns().stream().map(pt -> Pair.create(pt.getComponent(), pt.isExplicit)).collect(Collectors.toList());
      } else if (right instanceof Concrete.ConstructorPattern && !((GlobalReferable) ((Concrete.ConstructorPattern) right).getConstructor()).getPrecedence().isInfix) {
        patterns = new ArrayList<>(List.of(Pair.create(new Concrete.ConstructorPattern(right.getData(), right.isExplicit(), ((Concrete.ConstructorPattern) right).getConstructor(), List.of(), right.getAsReferable()), right.isExplicit())));
        patterns.addAll(right.getPatterns().stream().map(it -> Pair.create(it, it.isExplicit())).collect(Collectors.toList()));
      } else {
        patterns = new ArrayList<>(List.of(Pair.create(new Concrete.NamePattern(null, true, leftRef, null), true), Pair.create(right, right.isExplicit())));
      }
      return wrapSequence(data, mid, patterns);
    } else {
      return BinOpParser.makeBinOp(mid, new Concrete.NamePattern(null, true, leftRef, null), right, this);
    }
  }

  private Referable getReferableInner(Concrete.Pattern pattern) {
    return pattern instanceof Concrete.NamePattern ? ((Concrete.NamePattern) pattern).getReferable() : pattern instanceof Concrete.ConstructorPattern ? ((Concrete.ConstructorPattern) pattern).getConstructor() : null;
  }

  @Override
  public @NotNull String getPresentableComponentName() {
    return "pattern";
  }

  public static @NotNull Concrete.Pattern parse(@NotNull Concrete.UnparsedConstructorPattern pattern, @NotNull ErrorReporter reporter) {
    return new BinOpParser<>(reporter, engine).parse(pattern.getUnparsedPatterns());
  }
}
