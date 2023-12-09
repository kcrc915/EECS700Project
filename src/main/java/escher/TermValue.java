package escher;

import java.util.List;
import java.util.Map;

interface ExtendedValue {
    String show();
}

class ValueUnknown implements ExtendedValue {
    public String show() {
        return "?";
    }
}

interface TermValue extends ExtendedValue {
    default boolean smallerThan(TermValue tv) {
        if (sizeCompare().isDefinedAt(tv)) {
            return sizeCompare().apply(tv);
        } else {
            return false;
        }
    }

    default boolean greaterThan(TermValue tv) {
        return tv.smallerThan(this);
    }

    PartialFunction<TermValue, Boolean> sizeCompare();
}

class ValueError implements TermValue {
    public String show() {
        return "Err";
    }

    public PartialFunction<TermValue, Boolean> sizeCompare() {
        return new PartialFunction<TermValue, Boolean>() {
            public Boolean apply(TermValue v1) {
                return false;
            }

            public boolean isDefinedAt(TermValue v1) {
                return true;
            }
        };
    }
}

class ValueBool implements TermValue {
    private boolean value;

    public ValueBool(boolean value) {
        this.value = value;
    }

    public String show() {
        return value ? "T" : "F";
    }

    public PartialFunction<TermValue, Boolean> sizeCompare() {
        return new PartialFunction<TermValue, Boolean>() {
            public Boolean apply(TermValue v1) {
                if (v1 instanceof ValueBool) {
                    return !value && ((ValueBool) v1).value;
                } else {
                    return false;
                }
            }

            public boolean isDefinedAt(TermValue v1) {
                return v1 instanceof ValueBool;
            }
        };
    }
}

class ValueInt implements TermValue {
    private int value;

    public ValueInt(int value) {
        this.value = value;
    }

    public String show() {
        return Integer.toString(value);
    }

    public PartialFunction<TermValue, Boolean> sizeCompare() {
        return new PartialFunction<TermValue, Boolean>() {
            public Boolean apply(TermValue v1) {
                if (v1 instanceof ValueInt) {
                    return Math.abs(value) < Math.abs(((ValueInt) v1).value);
                } else {
                    return false;
                }
            }

            public boolean isDefinedAt(TermValue v1) {
                return v1 instanceof ValueInt;
            }
        };
    }
}

class ValueList implements TermValue {
    private List<TermValue> elems;

    public ValueList(List<TermValue> elems) {
        this.elems = elems;
    }

    public String show() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < elems.size(); i++) {
            sb.append(elems.get(i).show());
            if (i < elems.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public PartialFunction<TermValue, Boolean> sizeCompare() {
        return new PartialFunction<TermValue, Boolean>() {
            public Boolean apply(TermValue v1) {
                if (v1 instanceof ValueList) {
                    return elems.size() < ((ValueList) v1).elems.size();
                } else {
                    return false;
                }
            }

            public boolean isDefinedAt(TermValue v1) {
                return v1 instanceof ValueList;
            }
        };
    }
}

abstract class BinaryTree<T> {
    public abstract int size();

    public abstract <B> BinaryTree<B> map(Function<T, B> f);
}

class BinaryNode<T> extends BinaryTree<T> {
    private T tag;
    private BinaryTree<T> left;
    private BinaryTree<T> right;

    public BinaryNode(T tag, BinaryTree<T> left, BinaryTree<T> right) {
        this.tag = tag;
        this.left = left;
        this.right = right;
    }

    public int size() {
        return 1 + left.size() + right.size();
    }

    public <B> BinaryTree<B> map(Function<T, B> f) {
        return new BinaryNode<>(f.apply(tag), left.map(f), right.map(f));
    }
}

class BinaryLeaf<T> extends BinaryTree<T> {
    public int size() {
        return 0;
    }

    public <B> BinaryTree<B> map(Function<T, B> f) {
        return new BinaryLeaf<>();
    }
}

class BinaryTreeUtils {
    public static <A> BinaryNode<A> singleNode(A tag) {
        return new BinaryNode<>(tag, new BinaryLeaf<>(), new BinaryLeaf<>());
    }
}

class ValueTree implements TermValue {
    private BinaryTree<TermValue> value;

    public ValueTree(BinaryTree<TermValue> value) {
        this.value = value;
    }

    public String show() {
        return aux(value);
    }

    private String aux(BinaryTree<TermValue> t) {
        if (t instanceof BinaryNode) {
            BinaryNode<TermValue> node = (BinaryNode<TermValue>) t;
            return "(" + node.tag.show() + ": " + aux(node.left) + ", " + aux(node.right) + ")";
        } else {
            return "L";
        }
    }

    public PartialFunction<TermValue, Boolean> sizeCompare() {
        return new PartialFunction<TermValue, Boolean>() {
            public Boolean apply(TermValue v1) {
                if (v1 instanceof ValueTree) {
                    return value.size() < ((ValueTree) v1).value.size();
                } else {
                    return false;
                }
            }

            public boolean isDefinedAt(TermValue v1) {
                return v1 instanceof ValueTree;
            }
        };
    }
}

class ValuePair implements TermValue {
    private TermValue left;
    private TermValue right;

    public ValuePair(TermValue left, TermValue right) {
        this.left = left;
        this.right = right;
    }

    public String show() {
        return "(" + left.show() + ", " + right.show() + ")";
    }

    public PartialFunction<TermValue, Boolean> sizeCompare() {
        return new PartialFunction<TermValue, Boolean>() {
            public Boolean apply(TermValue v1) {
                if (v1 instanceof ValuePair) {
                    ValuePair pair = (ValuePair) v1;
                    return (left.smallerThan(pair.left)) || (left.equals(pair.left) && right.smallerThan(pair.right));
                } else {
                    return false;
                }
            }

            public boolean isDefinedAt(TermValue v1) {
                return v1 instanceof ValuePair;
            }
        };
    }
}

class TermValueUtils {
    public static Option<TypeSubst> matchTApply(Type ty, TApply target) {
        if (ty instanceof TVar) {
            TVar tVar = (TVar) ty;
            return Option.apply(new TypeSubst(Map.apply(tVar.id(), target)));
        } else if (ty instanceof TFixedVar) {
            return Option.empty();
        } else if (ty instanceof TApply) {
            TApply tApply = (TApply) ty;
            if (ty.equals(target)) {
                return Option.apply(TypeSubst.empty());
            } else {
                return Option.empty();
            }
        } else {
            return Option.empty();
        }
    }
}

interface TypeConstructor {
    String name();

    int arity();

    default TApply of(Type... params) {
        require(params.length == arity());
        return new TApply(this, List.of(params));
    }
}

interface BasicType extends TypeConstructor {
    default int arity() {
        return 0;
    }
}

class TBool implements BasicType {
    private static final TBool instance = new TBool();

    private TBool() {
    }

    public static TBool of() {
        return instance;
    }

    public String name() {
        return "Bool";
    }
}

class TInt implements BasicType {
    private static final TInt instance = new TInt();

    private TInt() {
    }

    public static TInt of() {
        return instance;
    }

    public String name() {
        return "Int";
    }
}

class TList implements TypeConstructor {
    private static final TList instance = new TList();

    private TList() {
    }

    public static TList of() {
        return instance;
    }

    public String name() {
        return "List";
    }

    public int arity() {
        return 1;
    }
}

class TTree implements TypeConstructor {
    private static final TTree instance = new TTree();

    private TTree() {
    }

    public static TTree of() {
        return instance;
    }

    public String name() {
        return "Tree";
    }

    public int arity() {
        return 1;
    }
}

class TPair implements TypeConstructor {
    private static final TPair instance = new TPair();

    private TPair() {
    }

    public static TPair of() {
        return instance;
    }

    public String name() {
        return "Pair";
    }

    public int arity() {
        return 2;
    }
}

class TMap implements TypeConstructor {
    private static final TMap instance = new TMap();

    private TMap() {
    }

    public static TMap of() {
        return instance;
    }

    public String name() {
        return "Map";
    }

    public int arity() {
        return 2;
    }
}

class TypeSubst {
    private Map<Integer, Type> subst;

    public TypeSubst(Map<Integer, Type> subst) {
        this.subst = subst;
    }

    public Type apply(Type t) {
        if (t instanceof TVar) {
            TVar tVar = (TVar) t;
            if (subst.containsKey(tVar.id())) {
                return subst.get(tVar.id());
            } else {
                return t;
            }
        } else if (t instanceof TApply) {
            TApply tApply = (TApply) t;
            return new TApply(tApply.constructor(), tApply.params().map(this::apply));
        } else {
            return t;
        }
    }

    public TypeSubst compose(TypeSubst s) {
        Map<Integer, Type> newSubst = new HashMap<>();
        for (Map.Entry<Integer, Type> entry : subst.entrySet()) {
            newSubst.put(entry.getKey(), s.apply(entry.getValue()));
        }
        for (Map.Entry<Integer, Type> entry : s.subst.entrySet()) {
            if (!newSubst.containsKey(entry.getKey())) {
                newSubst.put(entry.getKey(), entry.getValue());
            }
        }
        return new TypeSubst(newSubst);
    }

    public static TypeSubst empty() {
        return new TypeSubst(new HashMap<>());
    }
}

class TVar implements Type {
    private int id;

    public TVar(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }
}

class TFixedVar implements Type {
    private int id;

    public TFixedVar(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }
}

class TApply implements Type {
    private TypeConstructor constructor;
    private List<Type> params;

    public TApply(TypeConstructor constructor, List<Type> params) {
        this.constructor = constructor;
        this.params = params;
    }

    public TypeConstructor constructor() {
        return constructor;
    }

    public List<Type> params() {
        return params;
    }
}

interface Type {
}

class PartialFunction<A, B> {
    private Function<A, B> apply;
    private Function<A, Boolean> isDefinedAt;

    public PartialFunction(Function<A, B> apply, Function<A, Boolean> isDefinedAt) {
        this.apply = apply;
        this.isDefinedAt = isDefinedAt;
    }

    public B apply(A a) {
        return apply.apply(a);
    }

    public boolean isDefinedAt(A a) {
        return isDefinedAt.apply(a);
    }
}

class Option<A> {
    private A value;

    private Option(A value) {
        this.value = value;
    }

    public static <A> Option<A> apply(A value) {
        return new Option<>(value);
    }

    public static <A> Option<A> empty() {
        return new Option<>(null);
    }

    public A get() {
        return value;
    }

    public boolean isEmpty() {
        return value == null;
    }
}

class ListUtils {
    public static <A> List<A> of(A... elems) {
        return List.of(elems);
    }
}

class MapUtils {
    public static <K, V> Map<K, V> of(Tuple2<K, V>... entries) {
        Map<K, V> map = new HashMap<>();
        for (Tuple2<K, V> entry : entries) {
            map.put(entry._1, entry._2);
        }
        return map;
    }
}

class Tuple2<A, B> {
    public final A _1;
    public final B _2;

    public Tuple2(A _1, B _2) {
        this._1 = _1;
        this._2 = _2;
    }
}

class FunctionUtils {
    public static <A, B> Function<A, B> of(Function<A, B> f) {
        return f;
    }
}

class Require {
    public static void require(boolean condition) {
        if (!condition) {
            throw new IllegalArgumentException();
        }
    }
}

public class Main {
    public static void main(String[] args) {
        // TODO: Add test cases
    }
}



