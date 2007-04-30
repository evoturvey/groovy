import junit.framework.Test;
import junit.framework.TestCase;

/**
 * Collecting all security-related Unit Tests under groovy package, written in Java.
 *
 * @author Christian Stein
 * @author Dierk Koenig
 */
public class UberTestCaseJavaGroovySecurity extends TestCase {

    public static Test suite() {
        return GroovyJavaSecurityTestsSuite.suite();
    }

}