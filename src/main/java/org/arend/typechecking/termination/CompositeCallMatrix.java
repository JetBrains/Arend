package org.arend.typechecking.termination;

import org.arend.error.doc.Doc;
import org.arend.error.doc.DocFactory;
import org.arend.term.prettyprint.PrettyPrinterConfig;

public class CompositeCallMatrix<T> extends BaseCallMatrix<T> {
  private final BaseCallMatrix<T> myM1;
  private final BaseCallMatrix<T> myM2;

  CompositeCallMatrix(BaseCallMatrix<T> m1, BaseCallMatrix<T> m2) {
    super(m1, m2);
    myM1 = m1;
    myM2 = m2;
  }

  @Override
  public Doc getMatrixLabel(PrettyPrinterConfig ppConfig) {
    return DocFactory.vList(myM1.getMatrixLabel(ppConfig), myM2.getMatrixLabel(ppConfig));
  }

  @Override
  public T getCodomain() {
    return myM2.getCodomain();
  }

  @Override
  public T getDomain() {
    return myM1.getDomain();
  }

  @Override
  public int getCompositeLength() {
    return myM1.getCompositeLength() + myM2.getCompositeLength();
  }

  @Override
  protected String[] getColumnLabels() {
    return myM2.getColumnLabels();
  }

  @Override
  protected String[] getRowLabels() {
    return myM1.getRowLabels();
  }
}
