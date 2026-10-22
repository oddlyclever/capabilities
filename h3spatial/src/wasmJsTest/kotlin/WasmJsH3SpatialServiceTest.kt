import kotlin.test.Test

/** Runs the shared service spec and the Uber agreement checks on this target — see H3SpatialServiceSpec for why per-target. */
class WasmJsH3SpatialServiceTest {
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
