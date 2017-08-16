package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.error.doc.DocFactory;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.provider.SourceInfoProvider;

public class DuplicateNameError extends NamingError {
  public final Abstract.ReferableSourceNode referable;
  public final Abstract.ReferableSourceNode previous;

  public DuplicateNameError(Level level, Abstract.ReferableSourceNode referable, Abstract.ReferableSourceNode previous, Abstract.SourceNode cause) {
    super(level, "Duplicate name: " + referable.getName(), cause);
    this.referable = referable;
    this.previous = previous;
  }

  @Override
  public Doc getBodyDoc(SourceInfoProvider src) {
    String text = "Previous occurrence";
    String prevPos = src.positionOf(previous);
    if (prevPos == null) {
      return DocFactory.hList(DocFactory.text(text + ": "), DocFactory.refDoc(previous));
    }

    String module = src.moduleOf(previous);
    if (module != null) {
      String module1 = src.moduleOf(referable);
      if (module.equals(module1)) {
        module = null;
      }
    }
    return DocFactory.text(text + " at " + (module == null ? "" : module + ":") + prevPos);
  }
}
