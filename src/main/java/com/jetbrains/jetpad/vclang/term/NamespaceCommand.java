package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.error.doc.DocFactory;
import com.jetbrains.jetpad.vclang.error.doc.DocStringBuilder;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public interface NamespaceCommand extends PrettyPrintable {
  enum Kind { OPEN, EXPORT, IMPORT }
  @Nonnull Kind getKind();
  /* @Nonnull */ @Nullable Referable getGroupReference();
  @Nonnull Collection<? extends GlobalReferable> getImportedPath();
  boolean isUsing();
  @Nonnull Collection<? extends NameRenaming> getOpenedReferences();
  @Nonnull Collection<? extends Referable> getHiddenReferences();

  @Override
  default String prettyPrint(PrettyPrinterInfoProvider infoProvider) {
    List<LineDoc> docs = new ArrayList<>();
    switch (getKind()) {
      case OPEN: docs.add(text("\\open")); break;
      case EXPORT: docs.add(text("\\export")); break;
      case IMPORT: docs.add(text("\\import")); break;
    }

    Collection<? extends GlobalReferable> path = getImportedPath();
    LineDoc referenceDoc;
    if (path.isEmpty()) {
      Referable ref = getGroupReference();
      referenceDoc = ref == null ? text("_") : refDoc(ref);
    } else {
      referenceDoc = hSep(text("."), path.stream().map(DocFactory::refDoc).collect(Collectors.toList()));
    }

    Collection<? extends NameRenaming> openedReferences = getOpenedReferences();
    boolean using = isUsing();
    if (!using || !openedReferences.isEmpty()) {
      List<LineDoc> renamingDocs = new ArrayList<>(openedReferences.size());
      for (NameRenaming renaming : openedReferences) {
        LineDoc renamingDoc = refDoc(renaming.getOldReference());
        GlobalReferable newRef = renaming.getNewReferable();
        if (newRef != null) {
          renamingDoc = hList(renamingDoc, text(" \\as "), refDoc(newRef));
        }
        renamingDocs.add(renamingDoc);
      }

      LineDoc openedReferencesDoc = hSep(text(", "), renamingDocs);
      if (!using) {
        referenceDoc = hList(referenceDoc, text("("), openedReferencesDoc, text(")"));
      }
      docs.add(referenceDoc);
      if (using) {
        docs.add(text("\\using"));
        docs.add(openedReferencesDoc);
      }
    } else {
      docs.add(referenceDoc);
    }

    Collection<? extends Referable> hidingReferences = getHiddenReferences();
    if (!hidingReferences.isEmpty()) {
      docs.add(text("\\hiding"));
      docs.add(hList(text("("), hSep(text(", "), hidingReferences.stream().map(DocFactory::refDoc).collect(Collectors.toList())), text(")")));
    }

    return DocStringBuilder.build(hSep(text(" "), docs));
  }
}
