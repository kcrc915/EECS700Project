package escher;

import java.util.*;

/**
 * type :=
 * | tVar
 * | typeConstructor[type, ... , type]
 *
 * Our type system is first-order, so function types are not permitted.
 */
public interface Type {
    String toString();
    Set<Integer> varSet();
    int nextFreeId();
    Type shiftId(int amount);
    Type renameIndices(Function<Integer, Integer> f);
    boolean instanceOf(Type that);
    Type fixVars();
    boolean containsVar(int id);
    boolean containsFixedVar(int id);
    boolean canAppearIn(Type bigType);
}

class TVar implements Type {
    private int id;

    public TVar(int id) {
        this.id = id;
    }

    public String toString() {
        return "?" + id;
    }

    public Set<Integer> varSet() {
        return new HashSet<>(Collections.singletonList(id));
    }

    public int nextFreeId() {
        return id + 1;
    }

    public Type shiftId(int amount) {
        return new TVar(id + amount);
    }

    public Type renameIndices(Function<Integer, Integer> f) {
        return new TVar(f.apply(id));
    }

    public boolean instanceOf(Type that) {
        return that instanceof TVar && ((TVar) that).id == id;
    }

    public Type fixVars() {
        return this;
    }

    public boolean containsVar(int id) {
        return this.id == id;
    }

    public boolean containsFixedVar(int id) {
        return false;
    }

    public boolean canAppearIn(Type bigType) {
        return true;
    }
}

class TFixedVar implements Type {
    private int id;

    public TFixedVar(int id) {
        this.id = id;
    }

    public String toString() {
        return "'" + id;
    }

    public Set<Integer> varSet() {
        return new HashSet<>();
    }

    public int nextFreeId() {
        return 0;
    }

    public Type shiftId(int amount) {
        return this;
    }

    public Type renameIndices(Function<Integer, Integer> f) {
        return this;
    }

    public boolean instanceOf(Type that) {
        return false;
    }

    public Type fixVars() {
        return this;
    }

    public boolean containsVar(int id) {
        return false;
    }

    public boolean containsFixedVar(int id) {
        return this.id == id;
    }

    public boolean canAppearIn(Type bigType) {
        return bigType.containsFixedVar(id);
    }
}

class TApply implements Type {
    private TypeConstructor contr;
    private List<Type> params;

    public TApply(TypeConstructor contr, List<Type> params) {
        this.contr = contr;
        this.params = params;
    }

    public String toString() {
        if (params.isEmpty()) {
            return contr.getName();
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(contr.getName());
            sb.append("[");
            for (int i = 0; i < params.size(); i++) {
                sb.append(params.get(i).toString());
                if (i < params.size() - 1) {
                    sb.append(",");
                }
            }
            sb.append("]");
            return sb.toString();
        }
    }

    public Set<Integer> varSet() {
        Set<Integer> set = new HashSet<>();
        for (Type param : params) {
            set.addAll(param.varSet());
        }
        return set;
    }

    public int nextFreeId() {
        int maxId = 0;
        for (Type param : params) {
            maxId = Math.max(maxId, param.nextFreeId());
        }
        return maxId;
    }

    public Type shiftId(int amount) {
        List<Type> shiftedParams = new ArrayList<>();
        for (Type param : params) {
            shiftedParams.add(param.shiftId(amount));
        }
        return new TApply(contr, shiftedParams);
    }

    public Type renameIndices(Function<Integer, Integer> f) {
        List<Type> renamedParams = new ArrayList<>();
        for (Type param : params) {
            renamedParams.add(param.renameIndices(f));
        }
        return new TApply(contr, renamedParams);
    }

    public boolean instanceOf(Type that) {
        if (that instanceof TApply) {
            TApply thatApply = (TApply) that;
            if (contr.equals(thatApply.contr) && params.size() == thatApply.params.size()) {
                for (int i = 0; i < params.size(); i++) {
                    if (!params.get(i).instanceOf(thatApply.params.get(i))) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    public Type fixVars() {
        List<Type> fixedParams = new ArrayList<>();
        for (Type param : params) {
            fixedParams.add(param.fixVars());
        }
        return new TApply(contr, fixedParams);
    }

    public boolean containsVar(int id) {
        for (Type param : params) {
            if (param.containsVar(id)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsFixedVar(int id) {
        for (Type param : params) {
            if (param.containsFixedVar(id)) {
                return true;
            }
        }
        return false;
    }

    public boolean canAppearIn(Type bigType) {
        if (bigType instanceof TApply) {
            TApply bigApply = (TApply) bigType;
            if (contr.equals(bigApply.contr) && instanceOf(bigApply)) {
                return true;
            }
            for (Type param : bigApply.params) {
                if (canAppearIn(param)) {
                    return true;
                }
            }
        }
        return false;
    }
}

class TypeSubst {
    private Map<Integer, Type> map;

    public TypeSubst(Map<Integer, Type> map) {
        this.map = map;
    }

    public Type apply(Type ty) {
        if (ty instanceof TVar) {
            TVar var = (TVar) ty;
            return map.getOrDefault(var.getId(), var);
        } else if (ty instanceof TApply) {
            TApply apply = (TApply) ty;
            List<Type> appliedParams = new ArrayList<>();
            for (Type param : apply.getParams()) {
                appliedParams.add(apply(param));
            }
            return new TApply(apply.getContr(), appliedParams);
        } else {
            return ty;
        }
    }

    public TypeSubst compose(TypeSubst that) {
        Map<Integer, Type> composedMap = new HashMap<>(that.map);
        for (Map.Entry<Integer, Type> entry : map.entrySet()) {
            composedMap.put(entry.getKey(), that.apply(entry.getValue()));
        }
        return new TypeSubst(composedMap);
    }

    public TypeSubst deleteVar(int i1) {
        Map<Integer, Type> deletedMap = new HashMap<>(map);
        deletedMap.remove(i1);
        return new TypeSubst(deletedMap);
    }

    public boolean contains(TypeSubst subst) {
        return subst.map.entrySet().stream().allMatch(entry -> map.containsKey(entry.getKey()) && map.get(entry.getKey()).equals(entry.getValue()));
    }
}

class TypeConstructor {
    private String name;

    public TypeConstructor(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

class TypeUtils {
    public static String show(Type t) {
        return t.toString();
    }

    public static Set<Integer> freeVarSet(Type t) {
        return t.varSet();
    }

    public static Type alphaNormalForm(Type t) {
        Map<Integer, Integer> map = alphaNormalRenaming(t);
        return t.renameIndices(map::get);
    }

    public static Map<Integer, Integer> alphaNormalRenaming(Type t) {
        int nextIndex = 0;
        Map<Integer, Integer> map = new HashMap<>();
        alphaNormalRenamingHelper(t, map, nextIndex);
        return map;
    }

    private static void alphaNormalRenamingHelper(Type t, Map<Integer, Integer> map, int nextIndex) {
        if (t instanceof TVar) {
            TVar var = (TVar) t;
            map.putIfAbsent(var.getId(), nextIndex);
        } else if (t instanceof TApply) {
            TApply apply = (TApply) t;
            for (Type param : apply.getParams()) {
                alphaNormalRenamingHelper(param, map, nextIndex);
            }
        }
    }

    public static Optional<TypeSubst> unify(Type ty1, Type ty2) {
        if (ty1.equals(ty2)) {
            return Optional.of(TypeSubst.empty());
        }
        if (ty1 instanceof TVar) {
            TVar var1 = (TVar) ty1;
            if (ty2.containsVar(var1.getId())) {
                return Optional.empty();
            } else {
                return Optional.of(new TypeSubst(Collections.singletonMap(var1.getId(), ty2)));
            }
        }
        if (ty2 instanceof TVar) {
            TVar var2 = (TVar) ty2;
            if (ty1.containsVar(var2.getId())) {
                return Optional.empty();
            } else {
                return Optional.of(new TypeSubst(Collections.singletonMap(var2.getId(), ty1)));
            }
        }
        if (ty1 instanceof TFixedVar || ty2 instanceof TFixedVar) {
            return Optional.empty();
        }
        if (ty1 instanceof TApply && ty2 instanceof TApply) {
            TApply apply1 = (TApply) ty1;
            TApply apply2 = (TApply) ty2;
            if (apply1.getContr().equals(apply2.getContr()) && apply1.getParams().size() == apply2.getParams().size()) {
                TypeSubst subst = TypeSubst.empty();
                for (int i = 0; i < apply1.getParams().size(); i++) {
                    Optional<TypeSubst> unifier = unify(subst.apply(apply1.getParams().get(i)), subst.apply(apply2.getParams().get(i)));
                    if (unifier.isEmpty()) {
                        return Optional.empty();
                    }
                    subst = subst.compose(unifier.get());
                }
                return Optional.of(subst);
            }
        }
        return Optional.empty();
    }

    public static boolean instanceOf(Type t, Type parent) {
        int pFreeId = parent.nextFreeId();
        Type t1 = t.shiftId(pFreeId);
        Optional<TypeSubst> unifier = unify(parent, t1);
        if (unifier.isPresent()) {
            return unifier.get().apply(t1).equals(t1);
        } else {
            return false;
        }
    }

    public static boolean canAppearIn(Type smallType, Type bigType) {
        if (smallType instanceof TVar) {
            return true;
        }
        if (smallType instanceof TFixedVar) {
            TFixedVar fixedVar = (TFixedVar) smallType;
            return bigType.containsFixedVar(fixedVar.getId());
        }
        if (smallType instanceof TApply && bigType instanceof TApply) {
            TApply smallApply = (TApply) smallType;
            TApply bigApply = (TApply) bigType;
            if (smallApply.getContr().equals(bigApply.getContr()) && bigApply.instanceOf(smallApply)) {
                return true;
            }
            for (Type param : bigApply.getParams()) {
                if (canAppearIn(smallApply, param)) {
                    return true;
                }
            }
        }
        return false;
    }
}

class TypeSubstUtils {
    public static TypeSubst empty() {
        return new TypeSubst(Collections.emptyMap());
    }
}


