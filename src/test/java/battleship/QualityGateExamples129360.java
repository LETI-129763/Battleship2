package battleship;

/**
 * Examples used to validate the Checkstyle and Qodana quality gates.
 */
public final class QualityGateExamples129360 {

	private QualityGateExamples129360() {
	}

	// 129360 - Corrected MethodName example for the Checkstyle quality gate.
	public static int fixedCheckstyleViolationExample() {
		return 129360;
	}

	private static int qodanaParameterListTest(
			int a, int b, int c,
			int d, int e, int f
	) {
		return a + b + c + d + e + f;
	}
}
