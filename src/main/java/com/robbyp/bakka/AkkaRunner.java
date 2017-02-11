// Copyright 2017 Purcell Informatics Limited
//
// See the LICENCE file distributed with this work for additional
// information regarding copyright ownership.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.robbyp.bakka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.util.Timeout;
import com.robbyp.bakka.CountingActor.Count;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;


@Component
public class AkkaRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AppConfiguration.class);

    @Autowired
    private ApplicationContext ctx;

    public AkkaRunner(ApplicationContext context) {
        this.ctx = context;
    }

    public void run(String... args) throws Exception {
        ActorSystem system = ctx.getBean(ActorSystem.class);
        SpringExtension ext = ctx.getBean(SpringExtension.class);

        ActorRef counter = system.actorOf(ext.props("countingActor"), "counter");

        counter.tell(new Count(), null);
        counter.tell(new Count(), null);
        counter.tell(new Count(), null);

        FiniteDuration duration = FiniteDuration.create(3, TimeUnit.SECONDS);
        Future<Object> result = ask(counter, new CountingActor.Get(), Timeout.durationToTimeout(duration));
        try {
            logger.info("Got back " + Await.result(result, duration));
        } catch (Exception e) {
            logger.error("Failed getting result: " + e.getMessage());
            throw e;
        } finally {
            system.shutdown();
            system.awaitTermination();
        }

    }

}
