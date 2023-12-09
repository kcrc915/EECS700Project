package escher;



import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SynthesisTest extends WordSpec {
    private static Set<Type> typesAtCost(int cost) {
        Set<Type> types = new HashSet<>();
        switch (cost) {
            case 1:
                types.add(tyInt());
                break;
            case 2:
                types.add(tyInt());
                types.add(tyList(tyVar(0)));
                break;
            case 3:
                types.add(tyPair(tyInt(), tyBool()));
                break;
        }
        return types;
    }

    private static Set<Pair<IS<Type>, Type>> typesForCosts(IS<Integer> costs, IS<Type> inputTypes, Type returnType) {
        return Synthesis.typesForCosts(SynthesisTest::typesAtCost, costs, inputTypes, returnType);
    }

    public SynthesisTest() {
        import DSL.*;
        "typesForCosts check".in(() -> {
            assert typesForCosts(IS(1, 1), IS(tyInt(), tyInt()), tyBool()).equals(Set.of(new Pair<>(IS(tyInt(), tyInt()), tyBool())));

            assert typesForCosts(IS(1, 2), IS(tyInt(), tyList(tyVar(0))), tyBool()).equals(
                    Set.of(new Pair<>(IS(tyInt(), tyList(tyInt())), tyBool()), new Pair<>(IS(tyInt(), tyList(tyVar(0))), tyBool())));

            assert typesForCosts(IS(1, 2), IS(tyInt(), tyList(tyVar(0))), tyVar(0)).equals(
                    Set.of(new Pair<>(IS(tyInt(), tyList(tyInt())), tyInt()), new Pair<>(IS(tyInt(), tyList(tyVar(0))), tyVar(0))));

            assert typesForCosts(IS(3, 1), IS(tyPair(tyVar(0), tyVar(0)), tyVar(0)), tyList(tyVar(1))).equals(Set.of());

            assert typesForCosts(IS(3, 1), IS(tyPair(tyVar(0), tyVar(1)), tyVar(0)), tyList(tyVar(2))).equals(
                    Set.of(new Pair<>(IS(tyPair(tyInt(), tyBool()), tyInt()), tyList(tyVar(0)))));


            assert typesForCosts(IS(2, 2), IS(tyVar(0), tyList(tyVar(0))), tyList(tyInt())).equals(
                    Set.of(new Pair<>(IS(tyList(tyInt()), tyList(tyVar(0))), tyList(tyInt()))),
                    new Pair<>(IS(tyList(tyVar(0)), tyList(tyVar(0))), tyList(tyInt()))));

            assert typesForCosts(IS(2, 2), IS(tyVar(0), tyList(tyVar(0))), tyList(tyVar(0))).equals(
                    Set.of(new Pair<>(IS(tyList(tyInt()), tyList(tyVar(0))), tyList(tyList(tyInt()))),
                            new Pair<>(IS(tyList(tyVar(0)), tyList(tyVar(0))), tyList(tyList(tyVar(0))))));
        });
    }

    private static Map<Integer, TermValue> map(Pair<Integer, TermValue>... pairs) {
        Map<Integer, TermValue> map = new HashMap<>();
        for (Pair<Integer, TermValue> pair : pairs) {
            map.put(pair.first(), pair.second());
        }
        return map;
    }

    private static Pair<Map<Integer, Boolean>, Pair<Map<Integer, Integer>, Map<Integer, Integer>>> splitValueMap(
            Map<Integer, Integer> valueMap, IS<Integer> indices) {
        Set<Integer> indexSet = new HashSet<>(indices);
        Map<Integer, Boolean> boolMap = new HashMap<>();
        Map<Integer, Integer> trueMap = new HashMap<>();
        Map<Integer, Integer> falseMap = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : valueMap.entrySet()) {
            int index = entry.getKey();
            int value = entry.getValue();
            if (indexSet.contains(index)) {
                boolMap.put(index, value != 0);
                trueMap.put(index, value);
            } else {
                falseMap.put(index, value);
            }
        }
        return new Pair<>(boolMap, new Pair<>(trueMap, falseMap));
    }

    public SynthesisTest() {
        import escher.Synthesis.IndexValueMap.*;
        "split correctly".in(() -> {
            assert splitValueMap(map(new Pair<>(1, 1), new Pair<>(2, 2), new Pair<>(3, 0), new Pair<>(4, 0)),
                    IS(0, 1, 2, 3, 4, 5, 6)).get().equals(
                    new Pair<>(map(new Pair<>(1, true), new Pair<>(2, true), new Pair<>(3, false), new Pair<>(4, false))),
                    new Pair<>(map(new Pair<>(1, 1), new Pair<>(2, 2))),
                    new Pair<>(map(new Pair<>(3, 0), new Pair<>(4, 0)))));

            assert splitValueMap(map(new Pair<>(1, 1), new Pair<>(2, 0), new Pair<>(3, 0), new Pair<>(4, 4)),
                    IS(0, 1, 2, 3, 4, 5, 6)).get().equals(
                    new Pair<>(map(new Pair<>(1, true), new Pair<>(2, false), new Pair<>(3, false), new Pair<>(4, true))),
                    new Pair<>(map(new Pair<>(1, 1), new Pair<>(4, 4))),
                    new Pair<>(map(new Pair<>(2, 0), new Pair<>(3, 0)))));
        });
    }
}


