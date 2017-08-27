package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.error.SourceInfo;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.error.doc.DocFactory;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;

public class ReferableDuplicateNameError<T> extends ReferableError<T> {
  private final Referable previous;

  public ReferableDuplicateNameError(Level level, Referable referable, Referable previous) {
    super(level, "Duplicate name: " + referable.textRepresentation(), referable);
    this.previous = previous;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterInfoProvider src) {
    return getBodyDoc(referable, previous);
  }

  static Doc getBodyDoc(Referable referable, Referable previous) {
    String text = "Previous occurrence";
    if (!(previous instanceof SourceInfo)) {
      return DocFactory.hList(DocFactory.text(text + ": "), DocFactory.refDoc(previous));
    }

    String module = ((SourceInfo) previous).moduleTextRepresentation();
    if (module != null) {
      String module1 = referable instanceof SourceInfo ? ((SourceInfo) referable).moduleTextRepresentation() : null;
      if (module.equals(module1)) {
        module = null;
      }
    }
    return DocFactory.text(text + " at " + (module == null ? "" : module + ":") + ((SourceInfo) previous).positionTextRepresentation());
  }
}
