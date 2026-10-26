import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the shared service spec and the Uber agreement checks on a real Android runtime.
 *
 * Android is the one target a host test cannot cover: its H3 is `com.uber:h3` loading the
 * `libh3-java.so` this module packages, and that library only loads on a device or emulator. A pass
 * here proves the loader finds the packaged library, the JNI symbols match the jar's `NativeMethods`,
 * and the results agree with Uber's reference data — on the ABI the device runs.
 */
@RunWith(AndroidJUnit4::class)
class AndroidH3SpatialServiceTest {
    @Test fun pointToCell() = H3SpatialServiceSpec.pointToCell()
    @Test fun canonicalForm() = H3SpatialServiceSpec.canonicalForm()
    @Test fun cellGeometry() = H3SpatialServiceSpec.cellGeometry()
    @Test fun validity() = H3SpatialServiceSpec.validity()
    @Test fun gridTraversal() = H3SpatialServiceSpec.gridTraversal()
    @Test fun pentagons() = H3SpatialServiceSpec.pentagons()
    @Test fun hierarchy() = H3SpatialServiceSpec.hierarchy()
    @Test fun geoJson() = H3SpatialServiceSpec.geoJson()
    @Test fun failureContract() = H3SpatialServiceSpec.failureContract()
    @Test fun agreesWithUberPointToCell() = UberAgreementSpec.pointToCell()
    @Test fun agreesWithUberBoundaries() = UberAgreementSpec.boundaries()
    @Test fun agreesWithUberCentres() = UberAgreementSpec.centres()
    @Test fun uberFixturesPresent() = UberAgreementSpec.fixturesPresent()
}
