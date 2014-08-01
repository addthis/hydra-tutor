/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.addthis.tutor.dropwizard;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;

import com.yammer.dropwizard.ConfiguredBundle;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;

import org.slf4j.LoggerFactory;

public class LogbackConfigurer implements ConfiguredBundle<HydraTutorConfiguration> {

    @Override
    public void run(HydraTutorConfiguration configuration, Environment environment) throws Exception {
        final Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        LoggerContext context = root.getLoggerContext();
        ThreadLocalAppender<ILoggingEvent> appender = createAppender(context);
        // configure logger for the relevant package
        context.getLogger("com.addthis").addAppender(appender);
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        //do nothing
    }

    private ThreadLocalAppender<ILoggingEvent> createAppender(LoggerContext context) {
        ThreadLocalAppender<ILoggingEvent> appender = new ThreadLocalAppender<>();
        appender.setContext(context);
        appender.setName("TLA");
        appender.start();
        return appender;
    }

}