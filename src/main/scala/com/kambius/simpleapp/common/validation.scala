package com.kambius.simpleapp.common

import cats.data.NonEmptyList
import cats.syntax.option._

object validation {
  type FieldName = String
  type Message   = String

  final case class FieldError(fieldName: FieldName, message: Message)
  type ValidationResult = Option[NonEmptyList[FieldError]]

  trait Validator[T] {
    def validate(target: T): ValidationResult
  }

  object Validator {
    def apply[T](implicit ev: Validator[T]): Validator[T] = ev
  }

  implicit class ValidatorOps[T: Validator](value: T) {
    def validate: ValidationResult =
      Validator[T].validate(value)
  }

  trait FieldValidator[T] {
    def validate(field: T, fieldName: FieldName): ValidationResult
  }

  case object Email extends FieldValidator[String] {
    private val pattern = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$".r

    override def validate(field: String, fieldName: FieldName): ValidationResult =
      if (!pattern.matches(field))
        NonEmptyList.of(FieldError(fieldName, s"$field is not a valid email")).some
      else
        None
  }

  final class NonNegative[T](implicit numeric: Numeric[T]) extends FieldValidator[T] {
    override def validate(field: T, fieldName: FieldName): ValidationResult = {
      import numeric._
      if (numeric.zero > field)
        NonEmptyList.of(FieldError(fieldName, s"$field is negative")).some
      else
        None
    }
  }

  object NonNegative {
    def apply[T: Numeric]: NonNegative[T] = new NonNegative
  }
}
