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


  trait ConditionalReads[T <: DomainObj : Reads] extends Reads[T]{
    val objReads:Reads[T]
    def useReads: JsValue => Boolean

    def reads(json : JsValue) : JsResult[T] = {
      if (useReads(json)) objReads.reads(json)
      else JsError("unsuitable json")
    }
  }

  implicit object AddTableReads extends ConditionalReads[AddTable] {
    val useReads: JsValue => Boolean = json => (json \ "$type").as[String] == "add_table"
  }
  implicit val addTableWrites = Json.writes[AddTable]

  implicit object UpdateTableReads extends ConditionalReads[UpdateTable] {
    val useReads: JsValue => Boolean = json => (json \ "$type").as[String] == "update_table"
  }
  implicit val updateTableWrites = Json.writes[UpdateTable]

  implicit object RemoveTableReads extends ConditionalReads[RemoveTable] {
    val useReads: JsValue => Boolean = json => (json \ "$type").as[String] == "remove_table"
  }
  implicit val removeTableWrites = Json.writes[RemoveTable]

  implicit object TableAddedReads extends ConditionalReads[TableAdded] {
    val useReads: JsValue => Boolean = json => (json \ "$type").as[String] == "table_added"
  }
  implicit val tableAddedWrites = Json.writes[TableAdded]

  implicit object TableUpdatedReads extends ConditionalReads[TableUpdated] {
    val useReads: JsValue => Boolean = json => (json \ "$type").as[String] == "table_updated"
  }
  implicit val tableUpdatedWrites = Json.writes[TableUpdated]

  implicit object TableRemovedReads extends ConditionalReads[TableRemoved] {
    val useReads: JsValue => Boolean = json => (json \ "$type").as[String] == "table_removed"
  }
  implicit val tableRemovedWrites = Json.writes[TableRemoved]

  implicit object RemovalFailedReads extends ConditionalReads[RemovalFailed] {
    val useReads: JsValue => Boolean = json => (json \ "$type").as[String] == "removal_failed"
  }
  implicit val removalFailedWrites = Json.writes[RemovalFailed]

  implicit object UpdateFailedReads extends ConditionalReads[UpdateFailed] {
    val useReads: JsValue => Boolean = json => (json \ "$type").as[String] == "update_failed"
  }
  implicit val updateFailedWrites = Json.writes[UpdateFailed]

  implicit object NotAuthorizedReads extends ConditionalReads[NotAuthorized.type] {
    val useReads: JsValue => Boolean = json => (json \ "$type").as[String] == NotAuthorized.$type
  }
  implicit object NotAuthorizedWrites extends OWrites[NotAuthorized.type]{
    def writes(o : NotAuthorized.type): JsObject = Json.obj("$type" -> NotAuthorized.$type)
  }

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
