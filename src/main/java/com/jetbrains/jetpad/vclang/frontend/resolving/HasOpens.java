package com.jetbrains.jetpad.vclang.frontend.resolving;

import com.jetbrains.jetpad.vclang.term.Concrete;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.function.Function;

public interface HasOpens {
  @Nonnull Iterable<OpenCommand> getOpens();

  Function<Concrete.Definition, Iterable<OpenCommand>> GET = def -> {
    if (def instanceof HasOpens) {
      return ((HasOpens) def).getOpens();
    } else {
      return Collections.emptySet();
    }
  };
}
