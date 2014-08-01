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

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.addthis.maljson.JSONObject;
import com.addthis.tutor.filter.HydraTutorState;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import com.sun.jersey.api.core.HttpContext;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.OutputStreamAppender;

@Path("/validate")
public class FilterTutorResource {

    private static final int TIMEOUT = 2;
    private static final TimeUnit TIMEOUT_UNITS = TimeUnit.MINUTES;

    @Context
    private HttpContext context;

    private final Cache<String, HydraTutorState> userState;

    private final ExecutorService executors = Executors.newFixedThreadPool(8, new ExitingThreadFactory());

    // TODO: eliminate this class
    static class ExitingThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable r) {
            Thread retval = new Thread(r);
            retval.setDaemon(true);
            return retval;
        }
    }

    static class FilterCallable implements Callable<JSONObject> {

        final String input;
        final String filter;
        final String filtertype;
        final HydraTutorState state;

        FilterCallable(String input, String filter, String filtertype, HydraTutorState state) {
            this.input = input;
            this.filter = filter;
            this.filtertype = filtertype;
            this.state = state;
        }

        private OutputStreamAppender configureLoging() {
            OutputStreamAppender appender = ThreadLocalAppender.threadLocalStream.get();
            if (appender == null) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                Logger template = (Logger) LoggerFactory.getLogger("com.addthis.tutor.dropwizard.FilterTutorResource");
                LoggerContext context = template.getLoggerContext();
                appender = new OutputStreamAppender();
                PatternLayoutEncoder encoder = new PatternLayoutEncoder();
                encoder.setPattern("%date [%level] %logger: %msg%n");
                encoder.setContext(context);
                encoder.start();
                appender.setEncoder(encoder);
                appender.setContext(context);
                appender.setOutputStream(stream);
                appender.start();
                ThreadLocalAppender.threadLocalStream.set(appender);
            }
            return appender;
        }

        @Override
        public JSONObject call() throws Exception {
            JSONObject response = new JSONObject();

            OutputStreamAppender appender = configureLoging();
            ByteArrayOutputStream stream = (ByteArrayOutputStream) appender.getOutputStream();
            try {
                try {
                    String output = state.filter(input, filter, filtertype);
                    response.put("output", output);
                } catch (Exception ex) {
                    response.put("output", ex.toString());
                }

                if (stream != null) {
                    response.put("messages", stream.toString());
                } else {
                    response.put("messages", "");
                }

                return response;
            } finally {
                if (stream != null) {
                    stream.reset();
                }
            }
        }
    }

    public FilterTutorResource(HydraTutorConfiguration config) {
        userState = CacheBuilder.newBuilder()
                .maximumSize(1000).expireAfterWrite(2, TimeUnit.DAYS)
                .build();
    }

    //jquery.ajax.post you might have to use @QueryParam

    @POST
    @Path("/post")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED, MediaType.WILDCARD})
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(@FormParam("input") String input, @FormParam("filter") String filter,
            @FormParam("filtertype") String filtertype, @FormParam("uid") String uid) throws Exception {

        try {
            HydraTutorState state = userState.getIfPresent(uid);

            if (state == null) {
                state = new HydraTutorState();
                userState.put(uid, state);
            }

            Future<JSONObject> future = executors.submit(
                    new FilterCallable(input, filter, filtertype, state));
            state.setFuture(future);
            JSONObject response = future.get(TIMEOUT, TIMEOUT_UNITS);
            return (Response.ok(response.toString()).build());

        } catch (TimeoutException e) {
            JSONObject response = new JSONObject();
            String output = "Timeout of " + TIMEOUT + " " +
                            TIMEOUT_UNITS.toString().toLowerCase() + " exceeded.";
            response.put("output", output);
            response.put("messages", e.toString());
            return (Response.ok(response.toString()).build());
        } catch (ExecutionException e) {
            JSONObject response = new JSONObject();
            String output = e.getCause().getMessage();
            response.put("output", output);
            response.put("messages", e.getCause().toString());
            return (Response.ok(response.toString()).build());
        } catch (Exception e) {
            JSONObject response = new JSONObject();
            String output = e.getMessage();
            response.put("output", output);
            response.put("messages", e.toString());
            return (Response.ok(response.toString()).build());
        }
    }

    @POST
    @Path("/reset")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED, MediaType.WILDCARD})
    @Produces({MediaType.APPLICATION_JSON})
    public String reset(@FormParam("uid") String uid) {
        HydraTutorState state = userState.getIfPresent(uid);

        if (state == null) {
            state = new HydraTutorState();
            userState.put(uid, state);
        }

        state.reset();

        return "ok";
    }

    private static String printStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }

}
