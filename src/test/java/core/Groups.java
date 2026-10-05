package core;

//TestNG group (tag) names, kept in one place so a typo cannot create a new group.
//Every test has: one or more service tags, one priority tag, regression, and smoke for the critical few
public final class Groups {

    private Groups() {
    }

    //Suites
    public static final String SMOKE = "smoke";
    public static final String REGRESSION = "regression";

    //Priority from the design doc: P1 must pass for a release, P2 important regression, P3 edge case
    public static final String P1 = "P1";
    public static final String P2 = "P2";
    public static final String P3 = "P3";

    //Services
    public static final String CUSTOMER = "customer";
    public static final String PRODUCT = "product";
    public static final String ORCHESTRATION = "orchestration";
    public static final String TRACKING = "tracking";
    public static final String PLATFORM = "platform";

    //Cross-cutting: tests that depend on the Kafka event pipeline
    public static final String KAFKA = "kafka";
}
