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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

@Path("/")
public class AssetsResource {

    @Context
    HttpServletRequest request;
    @Context
    HttpServletResponse response;
    @Context
    UriInfo uriInfo;

    private String webDir = "web";

    public AssetsResource(HydraTutorConfiguration dashboardConfig) {
        this.webDir = dashboardConfig.getWebDirectory();
    }

    @GET
    public Response getIndex() {
        try {
            OutputStream out = response.getOutputStream();
            File file = new File(webDir, "index.html");
            InputStream in = new FileInputStream(file);
            byte buf[] = new byte[1024];
            int read;
            while ((read = in.read(buf)) >= 0) {
                out.write(buf, 0, read);
            }
            out.flush();
            return Response.ok().build();
        } catch (Exception ex) {
            return Response.serverError().build();
        }
    }


    @GET
    @Path("/{target:.+}")
    @Produces(MediaType.WILDCARD)
    public Response getAsset(@PathParam("target") String target) {
        File file = new File(webDir, uriInfo.getPath());
        if (file.exists() && file.isFile()) {
            try {
                OutputStream out = response.getOutputStream();
                InputStream in = new FileInputStream(file);
                byte buf[] = new byte[1024];
                int read;
                while ((read = in.read(buf)) >= 0) {
                    out.write(buf, 0, read);
                }
                out.flush();
                if (target.endsWith(".js")) {
                    response.setContentType("application/javascript");
                }
                return Response.ok().build();
            } catch (Exception ex) {
                return Response.serverError().build();
            }
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}

