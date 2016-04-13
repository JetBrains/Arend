package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.context.binding.InferenceBinding;

import java.util.List;

public class UnsolvedBindings extends TypeCheckingError {
  private final List<InferenceBinding> myBindings;

  public UnsolvedBindings(List<InferenceBinding> bindings) {
    super("Internal error: some meta variables were not solved", bindings.get(0).getSourceNode());
    myBindings = bindings;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(printHeader()).append(getMessage());
    for (InferenceBinding binding : myBindings) {
      builder.append("\n\t").append(binding);
      if (binding.getSourceNode() instanceof Concrete.SourceNode) {
        builder.append(" at ").append(((Concrete.SourceNode) binding.getSourceNode()).getPosition());
      }
    }
    return builder.toString();
  }
}
