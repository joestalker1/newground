package domain

import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

object JsonConversion {
  implicit val loginRequestReads = Json.reads[LoginRequest]
  implicit val loginRequestWrites = Json.writes[LoginRequest]

  implicit object LoginFailedReads extends Reads[LoginFailed.type]{
    def reads(json : JsValue) : JsResult[LoginFailed.type] = {
      if((json \ "$type").as[String] == LoginFailed.$type) JsSuccess(LoginFailed)
      else JsError("expect "+LoginFailed.$type)
    }
  }

  implicit object LoginFailedWrites extends OWrites[LoginFailed.type]{
    def writes(o : LoginFailed.type):JsObject = Json.obj("$type" -> LoginFailed.$type)
  }

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

  implicit object SubscribeRequesReads extends Reads[SubscribeRequest.type]{
    def reads(json : JsValue) : JsResult[SubscribeRequest.type] = {
      if((json \ "$type").as[String] == SubscribeRequest.$type) JsSuccess(SubscribeRequest)
      else JsError("expect " + SubscribeRequest.$type)
    }
  }

  implicit object SubscribeRequestWrites extends OWrites[SubscribeRequest.type]{
    def writes(o : SubscribeRequest.type): JsObject = Json.obj("$type" -> SubscribeRequest.$type)
  }

  implicit object UnsubscribeRequesReads extends Reads[UnsubscribeRequest.type]{
    def reads(json : JsValue) : JsResult[UnsubscribeRequest.type] = {
      if((json \ "$type").as[String] == UnsubscribeRequest.$type) JsSuccess(UnsubscribeRequest)
      else JsError("expect " + UnsubscribeRequest.$type)
    }
  }

  implicit object UnsubscribeRequestWrites extends OWrites[UnsubscribeRequest.type] {
    def writes(o: UnsubscribeRequest.type): JsObject = Json.obj("$type" -> UnsubscribeRequest.$type)
  }


  trait ConditionalReads[T <:DomainObj] extends Reads[T]{
    val objReads = Json.reads[T]
    val useReads: Boolean
    def reads(json : JsValue) : JsResult[T] = {
      if (useReads) objReads.reads(json)
      else JsError("unsuitable json")
    }
  }

  implicit object AddTableReads extends ConditionalReads[AddTable]{
    val useReads: Boolean =
    def reads(json : JsValue) : JsResult[AddTable] = {
      if((json \ "$type").as[String] == "add_table") addTableReads.reads(json)
      else JsError("expect add_table")
    }
  }
  implicit val addTableWrites = Json.writes[AddTable]

  //case class UpdateTable(table: Table, $type: String = "update_table") extends DomainObj
  implicit object UpdateTableReads extends Reads[AddTable]{
    val updateTableReads = Json.reads[UpdateTable]
    def reads(json : JsValue) : JsResult[AddTable] = {
      if((json \ "$type").as[String] == "add_table") addTableReads.reads(json)
      else JsError("expect add_table")
    }
  }



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
