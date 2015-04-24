package com.jetbrains.jetpad.vclang;

import com.jetbrains.jetpad.vclang.parser.BuildVisitor;
import com.jetbrains.jetpad.vclang.parser.VcgrammarLexer;
import com.jetbrains.jetpad.vclang.parser.VcgrammarParser;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.error.ParserError;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Concrete;
import com.jetbrains.jetpad.vclang.term.visitor.NormalizeVisitor;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConsoleMain {

  public static void main(String[] args) throws IOException {
    InputStream stream = args.length == 0 ? System.in : new FileInputStream(new File(args[0]));
    ANTLRInputStream input = new ANTLRInputStream(stream);
    VcgrammarLexer lexer = new VcgrammarLexer(input);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    VcgrammarParser parser = new VcgrammarParser(tokens);
    VcgrammarParser.DefsContext tree = parser.defs();
    BuildVisitor builder = new BuildVisitor(Prelude.DEFINITIONS);
    List<Definition> defs = builder.visitDefs(tree);
    List<ParserError> parserErrors = builder.getErrors();
    if (!parserErrors.isEmpty()) {
      for (ParserError error : parserErrors) {
        System.err.println(error);
      }
      return;
    }

    Map<String, Definition> context = new HashMap<>();
    List<TypeCheckingError> errors = new ArrayList<>();
    for (Definition def : defs) {
      if (def instanceof FunctionDefinition) {
        FunctionDefinition funcDef = (FunctionDefinition) def.checkTypes(context, new ArrayList<Binding>(), errors);
        if (funcDef != null) {
          def = new FunctionDefinition(def.getName(), def.getPrecedence(), def.getFixity(), funcDef.getArguments(), funcDef.getResultType(), ((FunctionDefinition) def).getArrow(), ((FunctionDefinition) def).getTerm().normalize(NormalizeVisitor.Mode.NF));
          context.put(def.getName(), def);
        }
      }
      if (def != null) {
        StringBuilder stringBuilder = new StringBuilder();
        def.prettyPrint(stringBuilder, new ArrayList<String>(), Abstract.Expression.PREC);
        System.out.println(stringBuilder);
        System.out.println();
      }
    }
    for (TypeCheckingError error : errors) {
      if (error.getExpression() instanceof Concrete.Expression) {
        Concrete.Position position = ((Concrete.Expression) error.getExpression()).getPosition();
        System.err.print(position.line + ":" + position.column + ": ");
      }
      System.err.println(error);
    }
  }
}
