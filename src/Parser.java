import java.util.*;

public class Parser {

    Token token;
    Lexer lexer;

    public Parser(Lexer ts) {
        lexer = ts;
        token = lexer.next();
    }

    private String match(TokenType t) {
        String value = token.value();
        if (token.type().equals(t))
            token = lexer.next();
        else
            error(t);
        return value;
    }

    private void error(TokenType tok) {
        System.err.println("Syntax error: expecting: " + tok
                + "; saw: " + token);
        System.exit(1);
    }

    private void error(String tok) {
        System.err.println("Syntax error: expecting: " + tok
                + "; saw: " + token);
        System.exit(1);
    }

    public Program program() {
        Functions functions = new Functions();
        Declarations globals = declarations(functions);
        functions = functions(functions);
        return new Program(globals, functions);
    }

    private Declarations declarations(Functions fs) {
        Declarations ds = new Declarations();
        if (isType()) {
            declaration(ds, fs);
        }
        return ds;
    }

    private void declaration(Declarations ds, Functions fs) {
        Type t = type();
        Variable var = new Variable(match(TokenType.Identifier));
        Declaration d = new Declaration(var, t);
        ds.add(d);

        while (token.type().equals(TokenType.Comma)) {
            token = lexer.next();
            var = new Variable(match(TokenType.Identifier));
            d = new Declaration(var, t);
            ds.add(d);
        }
        if (!token.type().equals(TokenType.Semicolon)) {
            ds.clear();
            function(fs, var, t);
        } else match(TokenType.Semicolon);
    }

    private Declarations params() {
        Declarations ds = new Declarations();
        if (isType()) {
            param(ds);
        }
        return ds;
    }

    private void param(Declarations ds) {
        Type t = type();
        Variable var = new Variable(match(TokenType.Identifier));
        Declaration d = new Declaration(var, t);
        ds.add(d);

        while (token.type().equals(TokenType.Comma)) {
            token = lexer.next();
            t = type();
            var = new Variable(match(TokenType.Identifier));
            d = new Declaration(var, t);
            ds.add(d);
        }
    }

    private Type type() {
        Type t = null;
        if (token.type().equals(TokenType.Int)) t = Type.INT;
        else if (token.type().equals(TokenType.Bool)) t = Type.BOOL;
        else if (token.type().equals(TokenType.Float)) t = Type.FLOAT;
        else if (token.type().equals(TokenType.Char)) t = Type.CHAR;
        else if (token.type().equals(TokenType.Void)) t = Type.VOID;
        else error("Type Error : " + token.type());
        token = lexer.next();
        return t;

    }

    private Statement statement(Variable fname) {
        Statement s = new Skip();
        if (token.type().equals(TokenType.Semicolon)) s = new Skip();
        else if (token.type().equals(TokenType.LeftBrace)) s = statements(fname);
        else if (token.type().equals(TokenType.Identifier)) {
            Variable var = new Variable(match(TokenType.Identifier));
            if (token.type().equals(TokenType.Assign)) {
                s = assignment(fname, var);
            } else if (token.type().equals(TokenType.LeftParen)) {
                s = callStatement(fname, var);
            }
        } else if (token.type().equals(TokenType.Return)) s = returnStatement(fname);
        else if (token.type().equals(TokenType.If)) s = ifStatement(fname);
        else if (token.type().equals(TokenType.While)) s = whileStatement(fname);
        return s;
    }

    private Functions functions(Functions fs) {
        while (isType()) {
            function(fs);
        }
        return fs;
    }

    private void function(Functions fs) {
        Type t = type();
        Variable var = new Variable(match(TokenType.Identifier));
        match(TokenType.LeftParen);
        Declarations params = params();
        match(TokenType.RightParen);
        match(TokenType.LeftBrace);
        Declarations locals = declarations(fs);
        Block b = statements(var);
        match(TokenType.RightBrace);

        Function f = new Function(var, t, params, locals, b);
        fs.members.add(f);
    }

    private void function(Functions fs, Variable var, Type t) {
        match(TokenType.LeftParen);
        Declarations params = params();
        match(TokenType.RightParen);
        match(TokenType.LeftBrace);
        Declarations locals = declarations(fs);
        Block b = statements(var);
        match(TokenType.RightBrace);

        Function f = new Function(var, t, params, locals, b);
        fs.members.add(f);
    }

    private Block statements(Variable fname) {
        Block b = new Block();
        if (token.type().equals(TokenType.LeftBrace)) {
            match(TokenType.LeftBrace);
            while (token.type().equals(TokenType.Semicolon) || token.type().equals(TokenType.Identifier) || token.type().equals(TokenType.Return) ||
                    token.type().equals(TokenType.LeftBrace) || token.type().equals(TokenType.If) || token.type().equals(TokenType.While)) {
                Statement s = statement(fname);
                b.members.add(s);
            }
            match(TokenType.RightBrace);
        } else {
            while (token.type().equals(TokenType.Semicolon) || token.type().equals(TokenType.Identifier) || token.type().equals(TokenType.Return) ||
                    token.type().equals(TokenType.LeftBrace) || token.type().equals(TokenType.If) || token.type().equals(TokenType.While)) {
                Statement s = statement(fname);
                b.members.add(s);
            }
        }
        return b;
    }

    private Statement assignment(Variable fname, Variable var) {
        Assignment asmt = null;
        Expression e = null;
        match(TokenType.Assign);
        e = expression();
        match(TokenType.Semicolon);
        asmt = new Assignment(var, e);

        return asmt;
    }

    private Statement callStatement(Variable fname, Variable var) {
        Call call = null;
        ArrayList<Expression> params = new ArrayList<Expression>();
        match(TokenType.LeftParen);
        while (!token.type().equals(TokenType.RightParen)) {
            Expression e;
            e = expression();
            params.add(e);
            if (token.type().equals(TokenType.Comma))
                match(TokenType.Comma);
        }
        match(TokenType.RightParen);
        match(TokenType.Semicolon);
        call = new Call(var, params);

        return call;
    }

    private Conditional ifStatement(Variable fname) {
        Conditional con = null;

        match(TokenType.If);
        match(TokenType.LeftParen);
        Expression e = expression();
        match(TokenType.RightParen);
        Statement s = statement(fname);
        if (token.type().equals(TokenType.Else)) {
            match(TokenType.Else);
            Statement els = statement(fname);
            con = new Conditional(e, s, els);
        } else con = new Conditional(e, s);

        return con;
    }

    private Loop whileStatement(Variable fname) {
        Loop loop = null;

        match(TokenType.While);
        match(TokenType.LeftParen);
        Expression e = expression();
        match(TokenType.RightParen);
        Statement s = statement(fname);
        loop = new Loop(e, s);

        return loop;
    }

    private Statement returnStatement(Variable fname) {
        Return result = null;
        match(TokenType.Return);
        Expression val = expression();
        match(TokenType.Semicolon);
        result = new Return(fname, val);

        return result;
    }

    private Expression expression() {
        Expression c = conjunction();
        while (token.type().equals(TokenType.Or)) {
            Operator op = new Operator(match(token.type()));
            Expression e = expression();
            c = new Binary(op, c, e);
        }
        return c;
    }

    private Expression conjunction() {
        Expression eq = equality();
        while (token.type().equals(TokenType.And)) {
            Operator op = new Operator(match(token.type()));
            Expression e = expression();
            eq = new Binary(op, eq, e);
        }
        return eq;
    }

    private Expression equality() {
        Expression r = relation();
        while (isEqualityOp()) {
            Operator op = new Operator(match(token.type()));
            Expression r2 = relation();
            r = new Binary(op, r, r2);
        }
        return r;
    }

    private Expression relation() {
        Expression a = addition();
        while (isRelationalOp()) {
            Operator op = new Operator(match(token.type()));
            Expression a2 = addition();
            a = new Binary(op, a, a2);
        }
        return a;
    }

    private Expression addition() {
        Expression e = term();
        while (isAddOp()) {
            Operator op = new Operator(match(token.type()));
            Expression term2 = term();
            e = new Binary(op, e, term2);
        }
        return e;
    }

    private Expression term() {
        Expression e = factor();
        while (isMultiplyOp()) {
            Operator op = new Operator(match(token.type()));
            Expression term2 = factor();
            e = new Binary(op, e, term2);
        }
        return e;
    }

    private Expression factor() {
        if (isUnaryOp()) {
            Operator op = new Operator(match(token.type()));
            Expression term = primary();
            return new Unary(op, term);
        } else return primary();
    }

    private Expression primary() {
        Expression e = null;
        if (token.type().equals(TokenType.Identifier)) {
            Variable var = new Variable(match(TokenType.Identifier));
            if (token.type().equals(TokenType.LeftParen)) {
                ArrayList<Expression> params = new ArrayList<>();
                match(TokenType.LeftParen);
                while (!token.type().equals(TokenType.RightParen)) {
                    Expression e1 = expression();
                    params.add(e1);
                    if (token.type().equals(TokenType.Comma))
                        match(TokenType.Comma);
                }
                match(TokenType.RightParen);
                e = new Call(var, params);
            } else {
                e = var;
            }
        } else if (isLiteral()) {
            e = literal();
        } else if (token.type().equals(TokenType.LeftParen)) {
            token = lexer.next();
            e = expression();
            match(TokenType.RightParen);
        } else if (isType()) {
            Operator op = new Operator(match(token.type()));
            match(TokenType.LeftParen);
            Expression term = expression();
            match(TokenType.RightParen);
            e = new Unary(op, term);
        } else error("Identifier | Literal | ( | Type");
        return e;
    }

    private Value literal() {
        Value value = null;
        String str = token.value();
        if (token.type().equals(TokenType.IntLiteral)) {
            value = new IntValue(Integer.parseInt(str));
            token = lexer.next();
        } else if (token.type().equals(TokenType.True)) {
            value = new BoolValue(true);
            token = lexer.next();
        } else if (token.type().equals(TokenType.False)) {
            value = new BoolValue(false);
            token = lexer.next();
        } else if (token.type().equals(TokenType.FloatLiteral)) {
            value = new FloatValue(Float.parseFloat(str));
            token = lexer.next();
        } else if (token.type().equals(TokenType.CharLiteral)) {
            value = new CharValue(str.charAt(0));
            token = lexer.next();
        }
        return value;
    }


    private boolean isAddOp() {
        return token.type().equals(TokenType.Plus) ||
                token.type().equals(TokenType.Minus);
    }

    private boolean isMultiplyOp() {
        return token.type().equals(TokenType.Multiply) ||
                token.type().equals(TokenType.Divide);
    }

    private boolean isUnaryOp() {
        return token.type().equals(TokenType.Not) ||
                token.type().equals(TokenType.Minus);
    }

    private boolean isEqualityOp() {
        return token.type().equals(TokenType.Equals) ||
                token.type().equals(TokenType.NotEqual);
    }

    private boolean isRelationalOp() {
        return token.type().equals(TokenType.Less) ||
                token.type().equals(TokenType.LessEqual) ||
                token.type().equals(TokenType.Greater) ||
                token.type().equals(TokenType.GreaterEqual);
    }

    private boolean isType() {
        return token.type().equals(TokenType.Int)
                || token.type().equals(TokenType.Bool)
                || token.type().equals(TokenType.Float)
                || token.type().equals(TokenType.Char)
                || token.type().equals(TokenType.Void);
    }

    private boolean isLiteral() {
        return token.type().equals(TokenType.IntLiteral) ||
                isBooleanLiteral() ||
                token.type().equals(TokenType.FloatLiteral) ||
                token.type().equals(TokenType.CharLiteral);
    }

    private boolean isBooleanLiteral() {
        return token.type().equals(TokenType.True) ||
                token.type().equals(TokenType.False);
    }

    public static void main(String args[]) {
        String filename = "src\\Test Programs\\recFib.cpp";
        Parser parser = new Parser(new Lexer(filename));
        System.out.println("Begin parsing... " + filename + "\n");
        Program prog = parser.program();
        prog.display();
    }

}