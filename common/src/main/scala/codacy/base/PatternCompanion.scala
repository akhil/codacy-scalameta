package codacy.base

import codacy.dockerApi.ParameterDef
import play.api.libs.json._

import scala.util.{Failure, Success, Try}

sealed trait NoParams
case object NoParams extends NoParams

trait PatternCompanion{
  type Pattern <: codacy.base.Pattern
  type Configuration
  val defaultConfig:Configuration
  def fromConfiguration(config:Configuration):Pattern
  def fromParameters(params:Set[ParameterDef]):Try[Pattern]
}

object PatternCompanion{

  type Pattern = codacy.base.Pattern

  def apply[Pat <: Pattern,Config](cfg:Config, f: Config => Pat)(implicit readerP:Reads[Config]): PatternCompanion = {
    apply[Pat,Config](cfg)(f, (s:Set[ParameterDef]) =>
      JsObject(s.toList.map{ case ParameterDef(key,value) => key.value -> value }).validate(readerP) match{
        case JsSuccess(res,_) => Success(f(res))
        case err@JsError(_) => Failure(new Throwable(Json.stringify(JsError.toJson(err))))
      }
    )
  }

  def apply[Pat <: Pattern,Config](cfg:Config)(f: Config => Pat, g: Set[ParameterDef] => Try[Pat]): PatternCompanion = {
    new PatternCompanion {
      override type Pattern = Pat
      override type Configuration = Config
      override val defaultConfig = cfg
      override def fromConfiguration(config: Configuration): Pattern = f(config)
      override def fromParameters(params: Set[ParameterDef]) = g(params)
    }
  }

  def apply[Pat <: Pattern](f: Pat) : PatternCompanion = new PatternCompanion {
    override type Pattern = Pat
    override type Configuration = NoParams
    override val defaultConfig = NoParams
    override def fromConfiguration(config: Configuration): Pattern = f
    override def fromParameters(params: Set[ParameterDef]) = Success(f)
  }
}