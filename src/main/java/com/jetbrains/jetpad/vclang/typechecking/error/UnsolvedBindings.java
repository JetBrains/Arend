package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.context.binding.InferenceBinding;

import java.util.List;

public class UnsolvedBindings extends TypeCheckingError {
  public final List<InferenceBinding> bindings;

  public UnsolvedBindings(Abstract.Definition definition, List<InferenceBinding> bindings) {
    super(definition, "Internal error: some meta variables were not solved", bindings.get(0).getSourceNode());
    this.bindings = bindings;
  }

  @Deprecated
  public UnsolvedBindings(List<InferenceBinding> bindings) {
    this(null, bindings);
  }
}
