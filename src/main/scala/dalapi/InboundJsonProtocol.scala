package dalapi

import com.wordnik.swagger.annotations.{ApiModelProperty, ApiModel}
import dalapi.models.{ApiDataTable, ApiDataRecord, ApiDataField, ApiDataValue}

import spray.json._
import scala.reflect.runtime.universe._
import scala.annotation.meta.field
import dalapi.models._
import org.joda.time.{DateTimeZone, LocalDateTime}
import org.joda.time.format.ISODateTimeFormat

object InboundJsonProtocol extends DefaultJsonProtocol with NullOptions {
  implicit object DateTimeFormat extends RootJsonFormat[LocalDateTime] {

    val formatter = ISODateTimeFormat.basicDateTimeNoMillis

    //FIXME: does not seem to do the formatting
    def write(obj: LocalDateTime): JsValue = {
      JsString(formatter.print(obj))
    }

    def read(json: JsValue): LocalDateTime = json match {
      case JsString(s) => try {
        formatter.parseLocalDateTime(s)
      }
      catch {
        case t: Throwable => error(s)
      }
      case _ =>
        error(json.toString())
    }

    def error(v: Any): LocalDateTime = {
      val example = formatter.print(0)
      deserializationError(f"'$v' is not a valid date value. Dates must be in compact ISO-8601 format, e.g. '$example'")
    }
  }

  // Data configuration
  implicit val dataFieldformat = jsonFormat5(ApiDataField)
  implicit val virtualTableFormat: RootJsonFormat[ApiDataTable] = rootFormat(lazyFormat(jsonFormat7(ApiDataTable)))



  // Data
  implicit val apiDataRecord = jsonFormat2(ApiDataRecord)
  implicit val apiDataValueFormat = jsonFormat4(ApiDataValue)

  // Any id (used for crossreferences)
  implicit val apiGenericId = jsonFormat1(ApiGenericId)

  // Events
  implicit val apiEvent = jsonFormat2(ApiEvent)

  // Locations
  implicit val apiLocation = jsonFormat2(ApiLocation)

  // Organistaions
  implicit val apiOrganisation = jsonFormat2(ApiOrganisation)

  // People
  implicit val apiPerson = jsonFormat3(ApiPerson)
  implicit val apiPersonRelationship = jsonFormat3(ApiPersonRelationshipType)

  // Things
  implicit val apiThings = jsonFormat2(ApiThing)

  // Properties
  implicit val apiProperty = jsonFormat5(ApiProperty)

  // Crossrefs
  implicit val apiRelationship = jsonFormat1(ApiRelationship)

  // Property relationships
  implicit val apiPropertyRelationshipStatic = jsonFormat3(ApiPropertyRelationshipStatic)
  implicit val apiPropertyRelationshipDynamic = jsonFormat2(ApiPropertyRelationshipDynamic)

  // Types
  implicit val apiType = jsonFormat3(ApiSystemType)
  implicit val apiUom = jsonFormat4(ApiSystemUnitofmeasurement)
}
