package com.jetbrains.jetpad.vclang;

import com.jetbrains.jetpad.vclang.parser.BuildVisitor;
import com.jetbrains.jetpad.vclang.parser.VcgrammarLexer;
import com.jetbrains.jetpad.vclang.parser.VcgrammarParser;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionCheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionPrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.term.error.ParserError;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.expr.ElimExpression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import org.antlr.v4.runtime.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConsoleMain {

  public static void main(String[] args) throws IOException {
    InputStream stream = args.length == 0 ? System.in : new FileInputStream(new File(args[0]));
    ANTLRInputStream input = new ANTLRInputStream(stream);
    VcgrammarLexer lexer = new VcgrammarLexer(input);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    VcgrammarParser parser = new VcgrammarParser(tokens);
    BuildVisitor builder = new BuildVisitor(Prelude.DEFINITIONS);
    final List<ParserError> parserErrors = builder.getErrors();
    parser.removeErrorListeners();
    parser.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        parserErrors.add(new ParserError(new Concrete.Position(line, pos), msg));
      }
    });

    VcgrammarParser.DefsContext tree = parser.defs();
    List<Concrete.Definition> defs = parserErrors.isEmpty() ? builder.visitDefs(tree) : null;
    if (!parserErrors.isEmpty()) {
      for (ParserError error : parserErrors) {
        System.err.println(error);
      }
      return;
    }

    Map<String, Definition> context = Prelude.getDefinitions();
    List<TypeCheckingError> errors = new ArrayList<>();
    for (Abstract.Definition def : defs) {
      Definition typedDef = def.accept(new DefinitionCheckTypeVisitor(context, errors), new ArrayList<Binding>());

      if (typedDef instanceof FunctionDefinition) {
        FunctionDefinition funcDef = (FunctionDefinition) typedDef;
        if (funcDef.getTerm() != null && !(funcDef.getTerm() instanceof ElimExpression)) {
          typedDef = new FunctionDefinition(funcDef.getName(), funcDef.getPrecedence(), funcDef.getFixity(), funcDef.getArguments(), funcDef.getResultType().normalize(NormalizeVisitor.Mode.NF), funcDef.getArrow(), funcDef.getTerm().normalize(NormalizeVisitor.Mode.NF));
        }
      }

      if (typedDef != null) {
        StringBuilder stringBuilder = new StringBuilder();
        typedDef.accept(new DefinitionPrettyPrintVisitor(stringBuilder, new ArrayList<String>(), 0), null);
        System.out.println(stringBuilder);
        System.out.println();
      }
    }

    for (TypeCheckingError error : errors) {
      if (error.getExpression() instanceof Concrete.SourceNode) {
        Concrete.Position position = ((Concrete.SourceNode) error.getExpression()).getPosition();
        System.err.print(position.line + ":" + position.column + ": ");
      }
      System.err.println(error);
      System.err.println();
    }
  }
}
