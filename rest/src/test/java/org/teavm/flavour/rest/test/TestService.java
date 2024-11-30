/*
 *  Copyright 2015 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.flavour.rest.test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.teavm.flavour.rest.Resource;

// The usage of both jakarta.ws.rs and javax.ws.rs is required until the teavm
// core browser runner switches to an implementation based on the jakarta
// namespace.
@Resource
@Path("test")
@javax.ws.rs.Path("test")
public interface TestService {

    @GET
    @javax.ws.rs.GET
    @Path("integers/sum")
    @javax.ws.rs.Path("integers/sum")
    int sum(
            @QueryParam("a") @javax.ws.rs.QueryParam("a") int a,
            @QueryParam("b") @javax.ws.rs.QueryParam("b") int b
    );

    @GET
    @javax.ws.rs.GET
    @Path("integers/mod-{mod}/sum")
    @javax.ws.rs.Path("integers/mod-{mod}/sum")
    int sum(
            @PathParam("mod") @javax.ws.rs.PathParam("mod") int mod,
            @QueryParam("a") @javax.ws.rs.QueryParam("a") int a,
            @QueryParam("b") @javax.ws.rs.QueryParam("b") int b
    );

    @GET
    @javax.ws.rs.GET
    @Path("complex/demo")
    @javax.ws.rs.Path("complex/demo")
    ComplexDemo getComplexDemo();
}
