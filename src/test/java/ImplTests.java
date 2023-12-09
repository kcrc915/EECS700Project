



import java.util.List;

import static escher.DSL.*;


public class ImplTests extends WordSpec {

    private void checkImpl(ComponentImpl impl, boolean debugExecute, List<List<TermValue>> testCases) {
        for (List<TermValue> testCase : testCases) {
            List<TermValue> in = testCase.subList(0, testCase.size() - 1);
            TermValue out = testCase.get(testCase.size() - 1);
            assertEquals(out, impl.execute(in, debugExecute));
        }
    }

    public void checkImpl(ComponentImpl impl, List<List<TermValue>> testCases) {
        checkImpl(impl, false, testCases);
    }

    @Test
    public void testLengthImpl() {
        ComponentImpl lengthImpl = recursiveImpl(
                "length",
                IS("xs"),
                IS(TList.of(tyVar(0))),
                tyInt,
                standardComps,
                `if`("isEmpty", v("xs"),
                "zero", DSL.$()),
        alphabeticSmaller,
                false
        );

        List<List<TermValue>> testCases = List.of(
                List.of(listValue()),
                List.of(listValue(1, 2)),
                List.of(listValue(true, false, true, true))
        );

        checkImpl(lengthImpl, testCases);
    }

    @Test
    public void testForeverImpl() {
        ComponentImpl forever = recursiveImpl(
                "forever",
                IS("x"),
                IS(tyVar(0)),
                tyVar(0),
                standardComps,
                "forever", v("x"),
                alphabeticSmaller,
                false
        );

        List<List<TermValue>> testCases = List.of(
                List.of(listValue()),
                List.of(ValueError)
        );

        checkImpl(forever, testCases);
    }

    @Test
    public void testFibImpl() {
        ComponentImpl fibImpl = recursiveImpl(
                "fib",
                IS("n"),
                IS(tyInt),
                tyInt,
                standardComps.add(isZero),
                `if`("or", "isZero", v("n"), "isZero", "dec", v("n")),
        alphabeticSmaller,
                false
        );

        List<List<TermValue>> testCases = List.of(
                List.of(ValueInt(0)),
                List.of(ValueInt(1)),
                List.of(ValueInt(2)),
                List.of(ValueInt(3)),
                List.of(ValueInt(4)),
                List.of(ValueInt(5))
        );

        checkImpl(fibImpl, testCases);
    }
}


