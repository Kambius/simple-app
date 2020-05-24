package com.kambius.simpleapp.common

import cats.data.NonEmptyList
import com.kambius.simpleapp.common.validation._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class validationSpec extends AnyFreeSpec with Matchers {
  "FieldValidator" - {
    "should validate emails" in {
      Email.validate("somename@email.com", "some_email") shouldBe None
      Email.validate("@eemail.com", "some_email") should matchPattern {
        case Some(NonEmptyList(FieldError("some_email", _), Nil)) =>
      }
    }

    "should validate non neg numbers" in {
      NonNegative[Int].validate(3, "some_num") shouldBe None
      NonNegative[Int].validate(0, "some_num") shouldBe None
      NonNegative[Int].validate(-1, "some_num") should matchPattern {
        case Some(NonEmptyList(FieldError("some_num", _), Nil)) =>
      }
      NonNegative[Double].validate(-1.6, "some_num") should matchPattern {
        case Some(NonEmptyList(FieldError("some_num", _), Nil)) =>
      }
    }
  }
}
