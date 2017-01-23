package domain

import play.api.data.validation.ValidationError
import play.api.libs.json._

/**
  * Created by dfom on 22.01.2017.
  */
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

object JsonConversion {
  implicit val loginRequestReads = Json.reads[LoginRequest]
  implicit val loginRequestWrites = Json.writes[LoginRequest]
  implicit val loginFailedWrites = Json.writes[LoginFailed]
  implicit val loginFailedReads = Json.reads[LoginFailed]
  implicit val loginSuccessfulWrites = Json.writes[LoginSuccessful]
  implicit val loginSuccessfulReads = Json.reads[LoginSuccessful]
  implicit val pingReads = Json.reads[Ping]
  implicit val pingWrites = Json.writes[Ping]
  implicit val pongReads = Json.reads[Pong]
  implicit val pongWrites = Json.writes[Pong]
  implicit val tableReads = Json.reads[Table]
  implicit val tableWrites = Json.writes[Table]
  implicit val tableListReads = Json.reads[TableList]
  implicit val tableListWrites = Json.writes[TableList]

//  implicit val locationReads: Reads[Location] = (
//    (JsPath \ "lat").read[Double](min(-90.0) keepAnd max(90.0)) and
//      (JsPath \ "long").read[Double](min(-180.0) keepAnd max(180.0))
//    )(Location.apply _)

  implicit val subscribeRequestReads: Reads[SubscribeRequest.type] = (
    (JsPath \"$type").read[String](filter(ValidationError("expect subscribe_tables"))_ == "subscribe_tables")
    and SubscribeRequest
  )(_ => SubscribeRequest)

  //implicit val subscribeReads = Json.reads[SubscribeRequest]
  //implicit val subscribeWrites = Json.writes[SubscribeRequest]
  implicit val unsubscribeReads = Json.reads[UnsubscribeRequest]
  implicit val unsubscribeWrites = Json.writes[UnsubscribeRequest]
  implicit val tableAddedReads = Json.reads[TableAdded]
  implicit val tableAddedWrites = Json.writes[TableAdded]
  implicit val tableUpdatedReads = Json.reads[TableUpdated]
  implicit val tableUpdatedWrites = Json.writes[TableUpdated]
  implicit val tableRemovedReads = Json.reads[TableRemoved]
  implicit val tableRemovedWrites = Json.writes[TableRemoved]

  implicit class AsJson[A <: DomainObj](val obj: A) {
    def json(implicit env: Writes[A]): JsValue = Json.toJson(obj)
  }

  implicit class AsDomainObj(val json: JsValue) {
    def domain[A <: DomainObj](implicit env: Reads[A]): Either[Throwable, A] = {
      Json.fromJson[A](json) match {
        case JsSuccess(o, _) => Right(o)
        case JsError(errors) =>
          val errMsg = errors.foldLeft(new StringBuilder) { case (sbf, (jpath, seq)) =>
            sbf.append(jpath.path.map(_.toJsonString).mkString(",") + ":" + seq.map(_.message).mkString(","))
            sbf
          }
          Left(new NoSuchElementException(errMsg.toString()))
      }
    }
  }

  implicit class toJson(val ex: Throwable) extends AnyVal {
    def json: JsValue = Json.obj("$type" -> "exception", "reason" -> ex.toString)
  }

}
