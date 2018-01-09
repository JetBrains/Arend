package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.error.doc.DocFactory;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

import java.util.Collection;
import java.util.stream.Collectors;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class ClassFieldSynonymError extends TypecheckingError {
  public GlobalReferable classField;
  public Collection<GlobalReferable> synonyms;

  public ClassFieldSynonymError(GlobalReferable classField, Collection<GlobalReferable> synonyms, Concrete.SourceNode cause) {
    super("", cause);
    this.classField = classField;
    this.synonyms = synonyms;
  }

  @Override
  public LineDoc getHeaderDoc(PrettyPrinterConfig src) {
    return hList(super.getHeaderDoc(src), text("Field "), refDoc(classField), text(" has more than one synonyms: "),
      hSep(text(", "), synonyms.stream().map(DocFactory::refDoc).collect(Collectors.toList())));
  }
}
