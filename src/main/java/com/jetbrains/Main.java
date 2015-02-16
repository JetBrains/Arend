package main.java.com.jetbrains;

import main.java.com.jetbrains.parser.BuildVisitor;
import main.java.com.jetbrains.parser.VcgrammarLexer;
import main.java.com.jetbrains.parser.VcgrammarParser;
import main.java.com.jetbrains.term.NotInScopeException;
import main.java.com.jetbrains.term.definition.Definition;
import main.java.com.jetbrains.term.definition.FunctionDefinition;
import main.java.com.jetbrains.term.expr.Expression;
import main.java.com.jetbrains.term.typechecking.TypeCheckingException;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws IOException, NotInScopeException, TypeCheckingException {
        InputStream stream = args.length == 0 ? System.in : new FileInputStream(new File(args[0]));
        ANTLRInputStream input = new ANTLRInputStream(stream);
        VcgrammarLexer lexer = new VcgrammarLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        VcgrammarParser parser = new VcgrammarParser(tokens);
        ParseTree tree = parser.defs();
        BuildVisitor builder = new BuildVisitor();
        List<FunctionDefinition> defs = (List<FunctionDefinition>) builder.visit(tree);
        Map<String, Definition> signature = new HashMap<String, Definition>();
        for (FunctionDefinition def : defs) {
            List<String> names = new ArrayList<String>();
            Expression type = def.getType().fixVariables(names, signature).normalize();
            Expression term = def.getTerm().fixVariables(names, signature).normalize();
            term.checkType(new ArrayList<Definition>(), type);
            FunctionDefinition def1 = new FunctionDefinition(def.getName(), type, term);
            signature.put(def1.getName(), def1);
            def1.prettyPrint(System.out, new ArrayList<String>(), 0);
            System.out.println();
            System.out.println();
        }
    }
}
