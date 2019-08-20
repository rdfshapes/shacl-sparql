import java.io.File

import org.scalatest.FunSuite
import unibz.shapes.convert.Parser
import unibz.shapes.shape.{Shape => _}

class TestParser extends FunSuite {

  test(testName = "parse") {
    val schema = Parser.parse(new File("ex/shapes/nonRec/2/shacl/MovieShape.ttl"))
  }
}
