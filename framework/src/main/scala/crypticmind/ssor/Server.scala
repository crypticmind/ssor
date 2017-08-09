package crypticmind.ssor

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import crypticmind.ssor.model.API
import crypticmind.ssor.repo.{DepartmentRepo, TeamRepo, UserRepo}
import sangria.ast
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.marshalling.sprayJson._
import sangria.parser.QueryParser
import spray.json._

import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Failure, Success}

object Server extends App {

  implicit val system = ActorSystem("ssor")
  implicit val materializer = ActorMaterializer()
  val api: API = new API(new UserRepo, new TeamRepo, new DepartmentRepo)

  import system.dispatcher

  val route: Route =
    path("graphql") {
      get {
        getFromResource("graphiql.html")
      } ~
      post {
        entity(as[JsValue]) { requestJson ⇒
          graphQLEndpoint(requestJson)
        }
      }
    }

  def graphQLEndpoint(requestJson: JsValue): StandardRoute = {
    val JsObject(fields) = requestJson

    val JsString(query) = fields("query")

    val operation = fields.get("operationName") collect {
      case JsString(op) ⇒ op
    }

    val vars = fields.get("variables") match {
      case Some(obj: JsObject) ⇒ obj
      case _ ⇒ JsObject.empty
    }

    QueryParser.parse(query) match {
      case Success(queryAst) ⇒ complete(executeGraphQLQuery(queryAst, operation, vars))
      case Failure(error) ⇒ complete(BadRequest, JsObject("error" → JsString(error.getMessage)))
    }
  }

  def executeGraphQLQuery(query: ast.Document, op: Option[String], vars: JsObject): Future[(StatusCode with Product with Serializable, JsValue)] =
    Executor.execute(api.schema, query, deferredResolver = api.resolver, variables = vars, operationName = op)
      .map(OK → _)
      .recover {
        case error: QueryAnalysisError ⇒ BadRequest → error.resolveError
        case error: ErrorWithResolver ⇒ InternalServerError → error.resolveError
      }

  Http().bindAndHandle(route, "0.0.0.0", 8080)
}
