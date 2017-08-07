package crypticmind.ssor

import crypticmind.ssor.model.API
import crypticmind.ssor.repo.{DepartmentRepo, TeamRepo, UserRepo}
import spray.json.JsValue

import scala.concurrent.Future

//
//import akka.actor.ActorSystem
//import akka.http.scaladsl._
//import akka.http.scaladsl.server._
//import akka.http.scaladsl.server.Directives._
//import akka.stream.ActorMaterializer
//import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
//import sangria.parser._
//import spray.json._
//import sangria.marshalling.sprayJson._
//
//import scala.util.{Failure, Success}
//
//
//
//import language.postfixOps
//
//import sangria.ast.OperationType
//import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
//import sangria.parser.{QueryParser, SyntaxError}
//import sangria.marshalling.sprayJson._
//import spray.json._
//import akka.http.scaladsl.model.StatusCodes._
//import akka.stream.actor.{ActorPublisher, ActorSubscriber}
//import akka.util.Timeout
//import akka.actor.{ActorSystem, Props}
//import akka.http.scaladsl.Http
//import akka.http.scaladsl.server.Directives._
//import akka.http.scaladsl.server._
//import akka.stream.ActorMaterializer
//import akka.stream.scaladsl.{Sink, Source}
//import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
//import akka.event.Logging
//import akka.http.scaladsl.marshalling.ToResponseMarshallable
//import de.heikoseeberger.akkasse._
//import de.heikoseeberger.akkasse.EventStreamMarshalling._
//import sangria.execution.deferred.DeferredResolver
//
//import scala.concurrent.duration._
//import scala.util.control.NonFatal
//import scala.util.{Failure, Success}
//
object Server extends App {
//
//  implicit val system = ActorSystem("sangria-server")
//  implicit val materializer = ActorMaterializer()
//
//  import system.dispatcher
//
//  val route: Route =
//    (post & path("graphql")) {
//      entity(as[JsValue]) { requestJson ⇒
//        graphQLEndpoint(requestJson)
//      }
//    } ~
//      get {
//        getFromResource("graphiql.html")
//      }
//
//  def graphQLEndpoint(requestJson: JsValue) = {
//    val JsObject(fields) = requestJson
//
//    val JsString(query) = fields("query")
//
//    val operation = fields.get("operationName") collect {
//      case JsString(op) ⇒ op
//    }
//
//    val vars = fields.get("variables") match {
//      case Some(obj: JsObject) ⇒ obj
//      case _ ⇒ JsObject.empty
//    }
//
//    QueryParser.parse(query) match {
//
//      // query parsed successfully, time to execute it!
//      case Success(queryAst) ⇒
//        complete(executeGraphQLQuery(queryAst, operation, vars))
//
//      // can't parse GraphQL query, return error
//      case Failure(error) ⇒
//        complete(BadRequest, JsObject("error" → JsString(error.getMessage)))
//    }
//  }
//
//
//
//  def executeGraphQLQuery(query: Document, op: Option[String], vars: JsObject) =
//    Executor.execute(schema, query, new ProductRepo, variables = vars, operationName = op)
//      .map(OK → _)
//      .recover {
//        case error: QueryAnalysisError ⇒ BadRequest → error.resolveError
//        case error: ErrorWithResolver ⇒ InternalServerError → error.resolveError
//      }
//
//  Http().bindAndHandle(route, "0.0.0.0", 8080)
//


  import sangria.macros._
  import sangria.execution._
  import sangria.marshalling.sprayJson._
  import scala.concurrent.Await
  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext.Implicits.global

//  val query =
//    graphql"""
//      query MyUser {
//        user(id: "1") {
//          name
//          team {
//            name
//          }
//        }
//      }
//    """

  val query =
    graphql"""
      query MyUser {
        users {
          id
          name
          team {
            id
            name
          }
        }
      }
    """

  val api: API = new API(new UserRepo, new TeamRepo, new DepartmentRepo)

  val result: Future[JsValue] = Executor.execute(api.schema, query, deferredResolver = api.resolver)

  val x = Await.result(result, 1.minute)

  println(x.prettyPrint)



}
