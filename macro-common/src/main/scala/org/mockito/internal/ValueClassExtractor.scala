package org.mockito.internal

import scala.language.experimental.macros
import scala.reflect.macros.blackbox
import scala.util.Properties

trait ValueClassExtractor[VC] {
  def extract(vc: VC): Any
}

class NormalClassExtractor[T] extends ValueClassExtractor[T] {
  override def extract(vc: T): Any = vc
}

class ReflectionExtractor[VC] extends ValueClassExtractor[VC] {
  override def extract(vc: VC): Any = {
    val constructorParam = vc.getClass.getConstructors.head.getParameters.head
    val accessor = vc.getClass.getMethods
      .filter(m => m.getName == constructorParam.getName || m.getName.endsWith("$$" + constructorParam.getName))
      .head
    accessor.setAccessible(true)
    accessor.invoke(vc)
  }
}

object ValueClassExtractor {

  private val ScalaVersion = Properties.scalaPropOrElse("version.number", "unknown")

  implicit def instance[VC]: ValueClassExtractor[VC] = macro materialise[VC]

  def materialise[VC: c.WeakTypeTag](c: blackbox.Context): c.Expr[ValueClassExtractor[VC]] = {
    import c.universe._
    val tpe          = weakTypeOf[VC]
    val typeSymbol   = tpe.typeSymbol
    val isValueClass = typeSymbol.isClass && typeSymbol.asClass.isDerivedValueClass

    val r = if (isValueClass) {

      if (ScalaVersion.startsWith("2.12") || ScalaVersion.startsWith("2.13"))
        c.Expr[ValueClassExtractor[VC]](q"new _root_.org.mockito.internal.ReflectionExtractor[$tpe]")
      else if (ScalaVersion.startsWith("2.11"))
        c.Expr[ValueClassExtractor[VC]] {
          val companion = typeSymbol.companion
          q"""
            new _root_.org.mockito.internal.ValueClassExtractor[$tpe] {
              override def extract(vc: $tpe): Any = $companion.unapply(vc).get
            }
          """
        } else throw new Exception(s"Unsupported scala version $ScalaVersion")

    } else
      c.Expr[ValueClassExtractor[VC]](q"new _root_.org.mockito.internal.NormalClassExtractor[$tpe]")

    if (c.settings.contains("mockito-print-extractor")) println(show(r.tree))

    r
  }
}
