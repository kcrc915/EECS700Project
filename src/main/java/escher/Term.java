package escher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public interface Term {
    static boolean termsLt(List<Term> terms, List<Term> terms1) {
        return false;
    }

    String show();

    int kind();

    boolean lessThan(Term that);

    boolean greaterThan(Term that);

    termvalue executeTerm(Map<String, termvalue> varMap, Map<String, ComponentImpl> compMap) throws Exception;

    termvalue executeTermDebug(Map<String, termvalue> varMap, Map<String, ComponentImpl> compMap, int depth) throws Exception;

    ExtendedValue executeTermInExtendedEnv(Map<String, ExtendedValue> varMap, Map<String, ExtendedCompImpl> compMap) throws Exception;

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
    public termvalue executeTerm(Map<String, termvalue> varMap, Map<String, ComponentImpl> compMap) {
        return varMap.get(name);
    }

    @Override
    public termvalue executeTermDebug(Map<String, termvalue> varMap, Map<String, ComponentImpl> compMap, int depth) {
        System.out.println("  ".repeat(depth) + ">> " + show());
        termvalue v ;throw new ExecutionError("variable '" + name + "' not in scope!");
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
    public termvalue executeTerm(Map<String, termvalue> varMap, Map<String, ComponentImpl> compMap) {
        List<termvalue> args = terms.stream()
                .map(t -> {
                    try {
                        return t.executeTerm(varMap, compMap);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
        return compMap.get(name).executeEfficient(args);
    }

    @Override
    public termvalue executeTermDebug(Map<String, termvalue> varMap, Map<String, ComponentImpl> compMap, int depth) {
        System.out.println("  ".repeat(depth) + ">> " + show());
        List<termvalue> args = terms.stream()
                .map(t -> {
                    try {
                        return t.executeTermDebug(varMap, compMap, depth + 1);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());


        return null;
    }

    @Override
    public ExtendedValue executeTermInExtendedEnv(Map<String, ExtendedValue> varMap, Map<String, ExtendedCompImpl> compMap) throws Exception {
        return null;
    }

    private void execute(List<termvalue> args, boolean b) {
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
    public termvalue executeTerm(Map<String, termvalue> varMap, Map<String, ComponentImpl> compMap) throws Exception {
        termvalue cv = condition.executeTerm(varMap, compMap);
        if (cv == valueerror.INSTANCE) {
            return valueerror.INSTANCE;
        } else if (cv == valueBool.TRUE) {
            return thenBranch.executeTerm(varMap, compMap);
        } else if (cv == valueBool.FALSE) {
            return elseBranch.executeTerm(varMap, compMap);
        }
        throw new Exception("Branch condition evaluated to false type");
    }

    @Override
    public termvalue executeTermDebug(Map<String, termvalue> varMap, Map<String, ComponentImpl> compMap, int depth) throws Exception {
        System.out.println("  ".repeat(depth) + ">> " + show());
        termvalue cv = condition.executeTermDebug(varMap, compMap, depth + 1);
        valueerror v;

        if (cv == valueerror.INSTANCE) {
            v = valueerror.INSTANCE;
        } else if (cv == valueBool.TRUE) {
            v = (valueerror) thenBranch.executeTermDebug(varMap, compMap, depth + 1);
        } else if (cv == valueBool.FALSE) {
            v = (valueerror) elseBranch.executeTermDebug(varMap, compMap, depth + 1);
        } else {
            throw new Exception("Branch condition evaluated to false type");
        }
        System.out.println("  ".repeat(depth) + "--> " + v.show());
        return v;
    }

    @Override
    public ExtendedValue executeTermInExtendedEnv(Map<String, ExtendedValue> varMap, Map<String, ExtendedCompImpl> compMap) throws Exception {
        ExtendedValue cv = condition.executeTermInExtendedEnv(varMap, compMap);
        if (cv == valueunknown.INSTANCE) {
            return valueunknown.INSTANCE;
        } else if (cv == valueerror.INSTANCE) {
            return (ExtendedValue) valueerror.INSTANCE;
        } else if (cv == valueBool.TRUE) {
            return thenBranch.executeTermInExtendedEnv(varMap, compMap);
        } else if (cv == valueBool.FALSE) {
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

class termvalue {
    public String show() {
        String valueError = null;
        return valueError;
    }
    // implementation of TermValue class
}

class ComponentImpl {
    public String name;

    public <E> ComponentImpl(String holeName, ArrayList<E> es, Type returnType, Object o) {
    }

    public static <ArgListCompare> ComponentImpl recursiveImpl(Synthesis.ComponentSignature signature, Map<String, ComponentImpl> envCompMap, ArgListCompare argListCompare, Term term) {
        return null;
    }

    public termvalue executeEfficient(List<termvalue> args) {
        termvalue extendedValue = new termvalue();
        return extendedValue;
    }
    // implementation of ComponentImpl class
}

class ExecutionError extends RuntimeException {
    public ExecutionError(String message) {
        super(message);
    }
}



class ExtendedCompImpl {
    public ExtendedValue execute(List<termvalue> args) {
        return null;
    }
    // implementation of ExtendedCompImpl class
}

class valueerror extends termvalue {
    public static final valueerror INSTANCE = new valueerror();

    private valueerror() {
    }

    @Override
    public String show() {
        return "ValueError";
    }
}

class valueBool extends termvalue {
    public static final valueBool TRUE = new valueBool(true);
    public static final valueBool FALSE = new valueBool(false);

    private final boolean value;

    valueBool(boolean value) {
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

class valueunknown implements ExtendedValue {
    public static final valueunknown INSTANCE = new valueunknown();

    private valueunknown() {
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


