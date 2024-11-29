package org.arend.term;

import org.arend.ext.module.ModulePath;
import org.arend.ext.prettyprinting.PrettyPrintable;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.DocStringBuilder;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.ext.reference.Precedence;
import org.arend.naming.reference.ModuleReferable;
import org.arend.naming.scope.Scope;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public interface NamespaceCommand extends PrettyPrintable {
  enum Kind { OPEN, IMPORT }
  @NotNull Kind getKind();
  @NotNull List<String> getPath();
  boolean isUsing();
  @NotNull Collection<? extends NameRenaming> getOpenedReferences();
  @NotNull Collection<? extends NameHiding> getHiddenReferences();

  private String scopeContextToString(Scope.ScopeContext context) {
    return context == Scope.ScopeContext.DYNAMIC ? "." : context == Scope.ScopeContext.PLEVEL ? "\\plevel " : context == Scope.ScopeContext.HLEVEL ? "\\hlevel " : "";
  }

  @Override
  default void prettyPrint(StringBuilder builder, PrettyPrinterConfig infoProvider) {
    List<String> path = getPath();
    if (path.isEmpty()) {
      return;
    }

    List<LineDoc> docs = new ArrayList<>();
    switch (getKind()) {
      case OPEN: docs.add(text("\\open")); break;
      case IMPORT: docs.add(text("\\import")); break;
    }

    List<LineDoc> docPath = new ArrayList<>(path.size());
    //for (int i = 1; i <= path.size(); i++) {
      docPath.add(refDoc(new ModuleReferable(new ModulePath(path))));
    //}
    LineDoc referenceDoc = hSep(text("."), docPath);

    Collection<? extends NameRenaming> openedReferences = getOpenedReferences();
    boolean using = isUsing();
    if (!using || !openedReferences.isEmpty()) {
      List<LineDoc> renamingDocs = new ArrayList<>(openedReferences.size());
      for (NameRenaming renaming : openedReferences) {
        LineDoc renamingDoc = hList(text(scopeContextToString(renaming.getScopeContext())), refDoc(renaming.getOldReference()));
        String newName = renaming.getName();
        if (newName != null) {
          Precedence precedence = renaming.getPrecedence();
          renamingDoc = hList(renamingDoc, text(" \\as " + (precedence == null ? "" : precedence)), text(newName));
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

    Collection<? extends NameHiding> hidingReferences = getHiddenReferences();
    if (!hidingReferences.isEmpty()) {
      docs.add(text("\\hiding"));
      docs.add(hList(text("("), hSep(text(", "), hidingReferences.stream().map(nh -> hList(text(scopeContextToString(nh.getScopeContext())), refDoc(nh.getHiddenReference()))).collect(Collectors.toList())), text(")")));
    }

    DocStringBuilder.build(builder, hSep(text(" "), docs));
  }
}
