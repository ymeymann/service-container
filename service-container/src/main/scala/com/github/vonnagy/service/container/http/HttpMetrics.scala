package com.github.vonnagy.service.container.http

import java.util.concurrent.TimeUnit

import akka.actor.{ActorSelection, ActorSystem, Cancellable}
import akka.pattern.ask
import akka.util.Timeout
import com.github.vonnagy.service.container.log.LoggingAdapter
import com.github.vonnagy.service.container.metrics._
import spray.can.Http
import spray.can.server.Stats

import scala.concurrent.duration._

private[http] trait HttpMetrics extends LoggingAdapter {

  implicit def system: ActorSystem

  var metricsJob: Option[Cancellable] = None
  var lastStats = Stats(FiniteDuration(0, TimeUnit.MILLISECONDS), 0, 0, 0, 0, 0, 0, 0)

  def httpListener: Option[ActorSelection]

  val totConn = Gauge("container.http.connections.total") {
    lastStats.totalConnections
  }
  val openConn = Gauge("container.http.connections.open") {
    lastStats.openConnections
  }
  val maxOpenConn = Gauge("container.http.connections.max-open") {
    lastStats.maxOpenConnections
  }
  val totReq = Gauge("container.http.requests.total") {
    lastStats.totalRequests
  }
  val openReq = Gauge("container.http.requests.open") {
    lastStats.openRequests
  }
  val maxOpenReq = Gauge("container.http.requests.max-open") {
    lastStats.maxOpenRequests
  }
  val uptime = Gauge("container.http.uptime") {
    lastStats.uptime.toMillis
  }
  val idle = Gauge("container.http.idle-timeouts") {
    lastStats.requestTimeouts
  }

  protected[http] def scheduleHttpMetrics(interval: FiniteDuration): Unit = {
    // Schedule an event to gather the http statistics so that we can add information to our metrics system
    log.info("Scheduling http server metrics handler")
    implicit val dis = system.dispatcher
    metricsJob = Some(system.scheduler.schedule(interval, interval)(getMetrics))
  }

  protected[http] def cancelHttpMetrics(): Unit = {
    metricsJob.exists(_.cancel())
    metricsJob = None
  }

  private def getMetrics(): Unit = {

    try {
      implicit val dis = system.dispatcher
      implicit val timeout: Timeout = 1.second
      if (httpListener.isDefined) httpListener.get ? Http.GetStats onSuccess {
        case x: Stats => lastStats = x
      }
    }
    catch {
      case e: Exception =>
        log.error("An error occurred when trying to fetch and record the http server metrics", e)
    }
  }
}
