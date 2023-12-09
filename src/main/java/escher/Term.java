package escher;

import java.util.List;
import java.util.Map;

/**
 * term := <br>
 * | Var(name) <br>
 * | Component(name, term, ..., term) <br>
 * | if term then term else term <br>
 * please note this `term` is called `program` in the thesis.
 */
public interface Term {
    String show();

    int kind();

    boolean lessThan(Term that);

    boolean greaterThan(Term that);

    TermValue executeTerm(Map<String, TermValue> varMap, Map<String, ComponentImpl> compMap);

    TermValue executeTermDebug(Map<String, TermValue> varMap, Map<String, ComponentImpl> compMap, int depth);

    ExtendedValue executeTermInExtendedEnv(Map<String, ExtendedValue> varMap, Map<String, ExtendedCompImpl> compMap);

    void printTerm(int depth);
}

class Var implements Term {
    private final String name;

    public Var(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String show() {
        return "@" + name;
    }

    @Override
    public int kind() {
        return 0;
    }

    @Override
    public boolean lessThan(Term that) {
        if (that instanceof Var) {
            Var other = (Var) that;
            return name.compareTo(other.getName()) < 0;
        }
        return false;
    }

    @Override
    public boolean greaterThan(Term that) {
        if (that instanceof Var) {
            Var other = (Var) that;
            return name.compareTo(other.getName()) > 0;
        }
        return false;
    }

    @Override
    public TermValue executeTerm(Map<String, TermValue> varMap, Map<String, ComponentImpl> compMap) {
        return varMap.get(name);
    }

    @Override
    public TermValue executeTermDebug(Map<String, TermValue> varMap, Map<String, ComponentImpl> compMap, int depth) {
        System.out.println("  ".repeat(depth) + ">> " + show());
        TermValue v = varMap.getOrDefault(name, throw new ExecutionError("variable '" + name + "' not in scope!"));
        System.out.println("  ".repeat(depth) + "--> " + v.show());
        return v;
    }

    @Override
    public ExtendedValue executeTermInExtendedEnv(Map<String, ExtendedValue> varMap, Map<String, ExtendedCompImpl> compMap) {
        return varMap.get(name);
    }

    @Override
    public void printTerm(int depth) {
        System.out.println(" ".repeat(depth) + show());
    }
}

class Component implements Term {
    private final String name;
    private final List<Term> terms;

    public Component(String name, List<Term> terms) {
        this.name = name;
        this.terms = terms;
    }

    public String getName() {
        return name;
    }

    public List<Term> getTerms() {
        return terms;
    }

    @Override
    public String show() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("(");
        for (int i = 0; i < terms.size(); i++) {
            sb.append(terms.get(i).show());
            if (i < terms.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public int kind() {
        return 1;
    }

    @Override
    public boolean lessThan(Term that) {
        if (that instanceof Component) {
            Component other = (Component) that;
            if (name.compareTo(other.getName()) < 0) {
                return true;
            } else if (name.equals(other.getName())) {
                return Term.termsLt(terms, other.getTerms());
            }
        }
        return false;
    }

    @Override
    public boolean greaterThan(Term that) {
        if (that instanceof Component) {
            Component other = (Component) that;
            if (name.compareTo(other.getName()) > 0) {
                return true;
            } else if (name.equals(other.getName())) {
                return Term.termsLt(other.getTerms(), terms);
            }
        }
        return false;
    }

    @Override
    public TermValue executeTerm(Map<String, TermValue> varMap, Map<String, ComponentImpl> compMap) {
        List<TermValue> args = terms.stream()
                .map(t -> t.executeTerm(varMap, compMap))
                .collect(Collectors.toList());
        return compMap.get(name).executeEfficient(args);
    }

    @Override
    public TermValue executeTermDebug(Map<String, TermValue> varMap, Map<String, ComponentImpl> compMap, int depth) {
        System.out.println("  ".repeat(depth) + ">> " + show());
        List<TermValue> args = terms.stream()
                .map(t -> t.executeTermDebug(varMap, compMap, depth + 1))
                .collect(Collectors.toList());
        TermValue v = compMap.getOrDefault(name, throw new ExecutionError("component '" + name + "' not in scope!"))
                .execute(args, true);
        System.out.println("  ".repeat(depth) + "--> " + v.show());
        return v;
    }

    @Override
    public ExtendedValue executeTermInExtendedEnv(Map<String, ExtendedValue> varMap, Map<String, ExtendedCompImpl> compMap) {
        List<TermValue> args = terms.stream()
                .map(t -> t.executeTermInExtendedEnv(varMap, compMap))
                .filter(v -> v != ValueUnknown.INSTANCE)
                .collect(Collectors.toList());
        return compMap.get(name).execute(args);
    }

    @Override
    public void printTerm(int depth) {
        System.out.println(" ".repeat(depth) + show());
    }
}

class If implements Term {
    private final Term condition;
    private final Term thenBranch;
    private final Term elseBranch;

    public If(Term condition, Term thenBranch, Term elseBranch) {
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }

    public Term getCondition() {
        return condition;
    }

    public Term getThenBranch() {
        return thenBranch;
    }

    public Term getElseBranch() {
        return elseBranch;
    }

    @Override
    public String show() {
        return "if " + condition.show() + " then " + thenBranch.show() + " else " + elseBranch.show();
    }

    @Override
    public int kind() {
        return 2;
    }

    @Override
    public boolean lessThan(Term that) {
        if (that instanceof If) {
            If other = (If) that;
            return Term.termsLt(List.of(condition, thenBranch, elseBranch), List.of(other.getCondition(), other.getThenBranch(), other.getElseBranch()));
        }
        return kind() < that.kind();
    }

    @Override
    public boolean greaterThan(Term that) {
        if (that instanceof If) {
            If other = (If) that;
            return Term.termsLt(List.of(other.getCondition(), other.getThenBranch(), other.getElseBranch()), List.of(condition, thenBranch, elseBranch));
        }
        return kind() > that.kind();
    }

    @Override
    public TermValue executeTerm(Map<String, TermValue> varMap, Map<String, ComponentImpl> compMap) {
        TermValue cv = condition.executeTerm(varMap, compMap);
        if (cv == ValueError.INSTANCE) {
            return ValueError.INSTANCE;
        } else if (cv == ValueBool.TRUE) {
            return thenBranch.executeTerm(varMap, compMap);
        } else if (cv == ValueBool.FALSE) {
            return elseBranch.executeTerm(varMap, compMap);
        }
        throw new Exception("Branch condition evaluated to false type");
    }

    @Override
    public TermValue executeTermDebug(Map<String, TermValue> varMap, Map<String, ComponentImpl> compMap, int depth) {
        System.out.println("  ".repeat(depth) + ">> " + show());
        TermValue cv = condition.executeTermDebug(varMap, compMap, depth + 1);
        TermValue v;
        if (cv == ValueUnknown.INSTANCE) {
            v = ValueUnknown.INSTANCE;
        } else if (cv == ValueError.INSTANCE) {
            v = ValueError.INSTANCE;
        } else if (cv == ValueBool.TRUE) {
            v = thenBranch.executeTermDebug(varMap, compMap, depth + 1);
        } else if (cv == ValueBool.FALSE) {
            v = elseBranch.executeTermDebug(varMap, compMap, depth + 1);
        } else {
            throw new Exception("Branch condition evaluated to false type");
        }
        System.out.println("  ".repeat(depth) + "--> " + v.show());
        return v;
    }

    @Override
    public ExtendedValue executeTermInExtendedEnv(Map<String, ExtendedValue> varMap, Map<String, ExtendedCompImpl> compMap) {
        ExtendedValue cv = condition.executeTermInExtendedEnv(varMap, compMap);
        if (cv == ValueUnknown.INSTANCE) {
            return ValueUnknown.INSTANCE;
        } else if (cv == ValueError.INSTANCE) {
            return ValueError.INSTANCE;
        } else if (cv == ValueBool.TRUE) {
            return thenBranch.executeTermInExtendedEnv(varMap, compMap);
        } else if (cv == ValueBool.FALSE) {
            return elseBranch.executeTermInExtendedEnv(varMap, compMap);
        } else {
            throw new Exception("Branch condition evaluated to false type");
        }
    }

    @Override
    public void printTerm(int depth) {
        System.out.println(" ".repeat(depth) + show());
        thenBranch.printTerm(depth + 5);
        System.out.println(" ".repeat(depth) + "else ");
        elseBranch.printTerm(depth + 5);
    }
}

class TermValue {
    // implementation of TermValue class
}

class ComponentImpl {
    // implementation of ComponentImpl class
}

class ExecutionError extends RuntimeException {
    public ExecutionError(String message) {
        super(message);
    }
}

class ExtendedValue {
    // implementation of ExtendedValue class
}

class ExtendedCompImpl {
    // implementation of ExtendedCompImpl class
}

class ValueError extends TermValue {
    public static final ValueError INSTANCE = new ValueError();

    private ValueError() {
    }

    @Override
    public String show() {
        return "ValueError";
    }
}

class ValueBool extends TermValue {
    public static final ValueBool TRUE = new ValueBool(true);
    public static final ValueBool FALSE = new ValueBool(false);

    private final boolean value;

    private ValueBool(boolean value) {
        this.value = value;
    }

    public boolean getValue() {
        return value;
    }

    @Override
    public String show() {
        return String.valueOf(value);
    }
}

class ValueUnknown extends ExtendedValue {
    public static final ValueUnknown INSTANCE = new ValueUnknown();

    private ValueUnknown() {
    }

    @Override
    public String show() {
        return "ValueUnknown";
    }
}

class TermUtils {
    public static boolean termsLt(List<Term> terms1, List<Term> terms2) {
        for (int i = 0; i < terms1.size(); i++) {
            if (lt(terms2.get(i), terms1.get(i))) {
                return false;
            } else if (lt(terms1.get(i), terms2.get(i))) {
                return true;
            }
        }
        return false;
    }

    public static boolean lt(Term t1, Term t2) {
        if (t1 instanceof Var && t2 instanceof Var) {
            Var v1 = (Var) t1;
            Var v2 = (Var) t2;
            return v1.lessThan(v2);
        } else if (t1 instanceof Component && t2 instanceof Component) {
            Component c1 = (Component) t1;
            Component c2 = (Component) t2;
            return c1.lessThan(c2);
        } else if (t1 instanceof If && t2 instanceof If) {
            If i1 = (If) t1;
            If i2 = (If) t2;
            return termsLt(List.of(i1.getCondition(), i1.getThenBranch(), i1.getElseBranch()),
                    List.of(i2.getCondition(), i2.getThenBranch(), i2.getElseBranch()));
        }
        return t1.kind() < t2.kind();
    }
}


