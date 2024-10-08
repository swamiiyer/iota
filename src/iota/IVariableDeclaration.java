package iota;

/**
 * The AST node for a local variable declaration. Local variables are declared by its analyze() method, which also
 * re-writes any initializations as assignment statements, in turn generated by its codegen() method.
 */
class IVariableDeclaration extends IStatement {
    // Variable name.
    private final String name;

    // Variable type.
    private Type type;

    // Variable initializer.
    private final IExpression initializer;

    // The initializer rewritten (during analysis) as an assignment statement.
    private IStatement initialization;

    /**
     * Constructs an AST node for a variable declarator.
     *
     * @param line        line in which the variable occurs in the source file.
     * @param name        variable name.
     * @param type        variable type.
     * @param initializer initializer.
     */
    public IVariableDeclaration(int line, String name, Type type, IExpression initializer) {
        super(line);
        this.name = name;
        this.type = type;
        this.initializer = initializer;
    }

    /**
     * Returns the variable name.
     *
     * @return the variable name.
     */
    public String name() {
        return name;
    }

    /**
     * Returns the variable type.
     *
     * @return the variable type.
     */
    public Type type() {
        return type;
    }

    /**
     * Sets the variable type.
     *
     * @param type the type
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * {@inheritDoc}
     */
    public IVariableDeclaration analyze(Context context) {
        int offset = ((LocalContext) context).nextOffset();
        LocalVariableDefn defn = new LocalVariableDefn(type.resolve(context), offset);

        // Check for shadowing.
        Defn previousDefn = context.lookup(name);
        if (previousDefn instanceof LocalVariableDefn) {
            IAST.compilationUnit.reportSemanticError(line, "the variable " + name + " shadows another local variable");
        }

        // Declare it in the local context.
        context.addEntry(line, name, defn);

        // Turn initialization into assignment statement and analyze it.
        if (initializer != null) {
            defn.initialize();
            IAssignOp assignOp = new IAssignOp(line, new IVariable(line, name), initializer);
            assignOp.isStatementExpression = true;
            initialization = new IStatementExpression(line, assignOp).analyze(context);
        }

        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        initialization.codegen(output);
    }

    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("IVariableDeclaraton:" + line, e);
        e.addAttribute("name", name());
        e.addAttribute("type", type == null ? "" : type.toString());
        if (initializer != null) {
            JSONElement e1 = new JSONElement();
            e.addChild("Initializer", e1);
            initializer.toJSON(e1);
        }
    }
}
