package fr.konexii.formulon.domain

import org.scalatest.funspec.AnyFunSpec

import fr.konexii.formulon.domain.Invariants._

class InvariantsSuite extends AnyFunSpec {

  describe("A string") {

    describe("when validating that it is not blank") {

      it("should be invalid if it is empty") {
        assert(isNotBlank("").isInvalid)
      }

      it("should be valid if it contains a character") {
        assert(isNotBlank("a").isValid)
      }

    }

    describe("when validating its length") {

      it("should be valid if it is less than the limit") {
        assert(isNotMoreThan(2, "a").isValid)
      }

      it("should be valid if it is exactly at the limit") {
        assert(isNotMoreThan(2, "ab").isValid)
      }

      it("should be invalid if it is above the limit") {
        assert(isNotMoreThan(2, "abc").isInvalid)
      }

    }

    describe("when validating its content to be only alphanumeric") {

      it("should be invalid when a character is not alphanumeric") {
        assert(isOnlyAlphasAndDigits(")").isInvalid)
      }

      it("should be invalid when a character is a french accent") {
        assert(isOnlyAlphasAndDigits("È").isInvalid)
      }

      it("should be valid when a character is alphanumeric") {
        assert(isOnlyAlphasAndDigits("A").isValid)
      }

    }

    describe("when validating its content to be only unicode letters") {

      it("should be valid when a character is a unicode letter") {
        assert(isOnlyUnicodeLetters("A").isValid)
      }

      it("should be valid when a character is a weird unicode letter") {
        assert(isOnlyUnicodeLetters("ձ").isValid)
      }

      it("should be valid when a character a bunch of letters") {
        assert(isOnlyUnicodeLetters("ձauwhadiuh").isValid)
      }

      it("should be invalid when there is a character that is a not a letter") {
        assert(isOnlyUnicodeLetters("ձauw*hadiuh").isInvalid)
      }

    }

  }

}
