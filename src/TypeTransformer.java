public class TypeTransformer {

    public static Program T(Program p, TypeMap tm) {
        Functions functions = (Functions) T(p.functions, tm);
        return new Program(p.globals, functions);
    }

    public static Functions T(Functions fs, TypeMap globals) {
        Functions out = new Functions();
        for (int i = 0; i < fs.members.size(); i++) {
            TypeMap tm = new TypeMap();
            tm.putAll(globals);
            tm.putAll(StaticTypeCheck.typing(fs.members.get(i).params));
            tm.putAll(StaticTypeCheck.typing(fs.members.get(i).locals));
            for (int j = 0; j < fs.members.size(); j++) {
                tm.put(fs.members.get(j).name, fs.members.get(j).type);
            }
            Function f = new Function(fs.members.get(i).name, fs.members.get(i).type,
                    fs.members.get(i).params, fs.members.get(i).locals, (Block) T(fs.members.get(i).body, tm));
            out.members.add(f);
        }
        return out;
    }

    public static Expression T(Expression e, TypeMap tm) {
        if (e instanceof Value)
            return e;
        if (e instanceof Variable)
            return e;
        if (e instanceof Binary) {
            Binary b = (Binary) e;
            Type typ1 = StaticTypeCheck.typeOf(b.term1, tm);
            Type typ2 = StaticTypeCheck.typeOf(b.term2, tm);
            Expression t1 = T(b.term1, tm);
            Expression t2 = T(b.term2, tm);
            if (typ1 == Type.INT)
                return new Binary(b.op.intMap(b.op.val), t1, t2);
            else if (typ1 == Type.FLOAT)
                return new Binary(b.op.floatMap(b.op.val), t1, t2);
            else if (typ1 == Type.CHAR)
                return new Binary(b.op.charMap(b.op.val), t1, t2);
            else if (typ1 == Type.BOOL)
                return new Binary(b.op.boolMap(b.op.val), t1, t2);
            throw new IllegalArgumentException("should never reach here");
        }
        if (e instanceof Unary) {
            Unary u = (Unary) e;
            Type typ = StaticTypeCheck.typeOf(u.term, tm);
            Expression t = T(u.term, tm);
            if ((typ == Type.BOOL) && (u.op.NotOp()))
                return new Unary(u.op.boolMap(u.op.val), t);
            else if ((typ == Type.INT) && u.op.NegateOp())
                return new Unary(u.op.intMap(u.op.val), t);
            else if ((typ == Type.FLOAT) && u.op.NegateOp())
                return new Unary(u.op.floatMap(u.op.val), t);
            else if ((typ == Type.INT) && (u.op.floatOp() || u.op.charOp()))
                return new Unary(u.op.intMap(u.op.val), t);
            else if ((typ == Type.FLOAT) && u.op.intOp())
                return new Unary(u.op.floatMap(u.op.val), t);
            else if ((typ == Type.CHAR) && u.op.intOp())
                return new Unary(u.op.charMap(u.op.val), t);
        }
        if (e instanceof Call)
            return e;
        throw new IllegalArgumentException("should never reach here");
    }

    public static Statement T(Statement s, TypeMap tm) {
        if (s instanceof Skip) return s;
        if (s instanceof Assignment) {
            Assignment a = (Assignment) s;
            Variable target = a.target;
            Expression src = T(a.source, tm);
            Type ttype = (Type) tm.get(a.target);
            Type srctype = StaticTypeCheck.typeOf(a.source, tm);
            if (ttype == Type.FLOAT) {
                if (srctype == Type.INT) {
                    src = new Unary(new Operator(Operator.I2F), src);
                    srctype = Type.FLOAT;
                }
            } else if (ttype == Type.INT) {
                if (srctype == Type.CHAR) {
                    src = new Unary(new Operator(Operator.C2I), src);
                    srctype = Type.INT;
                }
            }
            StaticTypeCheck.check(ttype == srctype,
                    "bug in assignment to " + target);
            return new Assignment(target, src);
        }
        if (s instanceof Call) {
            return s;
        }
        if (s instanceof Conditional) {
            Conditional c = (Conditional) s;
            Expression test = T(c.test, tm);
            Statement tbr = T(c.thenbranch, tm);
            Statement ebr = T(c.elsebranch, tm);
            return new Conditional(test, tbr, ebr);
        }
        if (s instanceof Loop) {
            Loop l = (Loop) s;
            Expression test = T(l.test, tm);
            Statement body = T(l.body, tm);
            return new Loop(test, body);
        }
        if (s instanceof Block) {
            Block b = (Block) s;
            Block out = new Block();
            for (Statement stmt : b.members)
                out.members.add(T(stmt, tm));
            return out;
        }
        if (s instanceof Return) {
            return (Return) s;
        }
        throw new IllegalArgumentException("should never reach here");
    }


    public static void main(String args[]) {
        String filename = "src\\Test Programs\\recFib.cpp";
        Parser parser = new Parser(new Lexer(filename));
        System.out.println("Begin parsing... " + filename + "\n");
        Program prog = parser.program();
        prog.display();
        System.out.println("\nBegin type checking... " + filename);
        System.out.print("Globals = ");
        TypeMap globals = StaticTypeCheck.typing(prog.globals);
        globals.display();
        System.out.println();
        StaticTypeCheck.V(prog);
        System.out.println("No type errors\n");
        Program out = T(prog, globals);
        System.out.println("Transformed Abstract Syntax Tree\n");
        out.display();
    }

}

