package com.jetbrains.jetpad.vclang.module.error;

import com.jetbrains.jetpad.vclang.error.doc.DocFactory;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;

import java.util.List;

public class ModuleCycleError extends ModuleLoadingError {
  public final List<? extends SourceId> cycle;

  public ModuleCycleError(SourceId module, List<? extends SourceId> cycle) {
    super(module, "Module dependencies form a cycle");
    this.cycle = cycle;
  }

  @Override
  public LineDoc getBodyDoc(PrettyPrinterInfoProvider src) {
    StringBuilder builder = new StringBuilder();
    for (SourceId sourceId: cycle) {
      builder.append(sourceId.getModulePath()).append(" - ");
    }
    builder.append((cycle.get(0)));
    return DocFactory.text(builder.toString());
  }
}
