import org.scalatest.funsuite.AnyFunSuite
import fr.konexii.form._

/*
class ComponentSuite extends AnyFunSuite {
  test("this is a testing test") {
    assert(1 == 1)
  }

  test("create a basic form") {
    Field(
      id = "1",
      title = "What is your first name ?",
      required = false,
      attributes = Text(),
      next = Field(
        id = "2",
        title = "What is your last name ?",
        required = true,
        attributes = Text(),
        next = End(id = "3")
      )
    )
  }

  test("create a form with a condition") {
    Field(
      id = "1",
      title = "What is your first name ?",
      required = false,
      attributes = Text(),
      next = Branch(
        id = "2",
        condition = Or(
          lhs = Equal(
            rhs = Property("1"),
            lhs = StringLiteral("Mario")
          ),
          rhs =  Equal(
            rhs = Property("1"),
            lhs = StringLiteral("Luigi")
          )
        ),
        ifTrue = End(id = "3"),
        ifFalse = End(id = "4")
      )
    )
  }
}
*/
