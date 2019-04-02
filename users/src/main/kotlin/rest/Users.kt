package rest

import domain.CreateRequest
import domain.LoginRequest
import domain.SessionCode
import domain.User
import gateway.SessionGateway
import gateway.Singleton
import gateway.UsersGateway
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import port.SessionPort
import port.UsersPort
import usecase.UsersUsecase

val usecase = UsersUsecase()

fun Route.create() {
    post("/users") {
        try {
            call.receive<CreateRequest>()
                .let { usecase.create(it) }
                .let { call.respond(HttpStatusCode.OK, it) }
        } catch (e: Throwable) {
            e.printStackTrace()
            val pair = handle(e)
            call.respond(pair.first, pair.second)
        }
    }
}

fun Route.login() {
    post("/users/login") {
        try {
            call.receive<LoginRequest>()
                .let { usecase.login(it) }
                .let {
                    call.response.header("set-cookie", "AUTH-SESSION=${it.sessionCode.value}; path=/;")
                    call.respondText("{}", ContentType.Application.Json)
                }
        } catch (e: Throwable) {
            e.printStackTrace()
            val pair = handle(e)
            if (pair.first == HttpStatusCode.Forbidden) {
                call.response.header("set-cookie", "AUTH-SESSION=;")
            }
            call.respond(pair.first, pair.second)
        }
    }
}

fun Route.identify() {
    get("/users/identify") {
        try {
            (call.request.cookies["AUTH-SESSION"] ?: "")
                .let { usecase.identify(SessionCode(it)) }
                .let { call.respond(it) }
        } catch (e: Throwable) {
            e.printStackTrace()
            val pair = handle(e)
            call.respond(pair.first, pair.second)
        }
    }
}

fun Route.list() {
    get("/users") {
        try {
            usecase.list()
                .let { call.respond(it) }
        } catch (e: Throwable) {
            e.printStackTrace()
            val pair = handle(e)
            call.respond(pair.first, pair.second)
        }
    }
}
