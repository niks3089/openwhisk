/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.openwhisk.standalone

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import org.apache.openwhisk.common.{Logging, TransactionId}
import org.apache.openwhisk.http.BasicHttpService
import pureconfig._
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import scala.concurrent.{ExecutionContext}

class WebLocalLauncher(webPath: String, webPort: Int)(implicit logging: Logging,
                                             ec: ExecutionContext,
                                             actorSystem: ActorSystem,
                                             materializer: ActorMaterializer,
                                             tid: TransactionId) {
  private val interface = loadConfigOrThrow[String]("whisk.controller.interface")

  private val webUrl = s"http://${StandaloneDockerSupport.getLocalHostName()}:$webPort/"

  def run(): ServiceContainer = {
    BasicHttpService.startHttpService(WebLocalService.route, webPort, None, interface)(actorSystem, materializer)
    ServiceContainer(webPort, webUrl, "Web-local")
  }

  object WebLocalService extends BasicHttpService {
    override def routes(implicit transid: TransactionId): Route =
    path(PathEnd | Slash) {
      redirect("/index.html", StatusCodes.Found)
    } ~
      cors() {
        get {
          extractUnmatchedPath { _ =>
            getFromDirectory(webPath)
          }
        }
      }
  }
}
