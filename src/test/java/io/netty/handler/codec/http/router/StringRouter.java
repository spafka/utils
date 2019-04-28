/*
 * Copyright 2015 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.handler.codec.http.router;

import io.github.spafka.rest.netty.handler.codec.http.router.Router;

final class StringRouter {
    // Utility classes should not have a public or default constructor.
    private StringRouter() { }

    static Router<String> create() {
        return new Router<String>()
                .addGet("/articles", "index")
                .addGet("/articles/:id", "show")
                .addGet("/articles/:id/:format", "show")
                .addGet("/articles/new", "new")
                .addPost("/articles", "post")
                .addPatch("/articles/:id", "patch")
                .addDelete("/articles/:id", "delete")
                .addAny("/anyMethod", "anyMethod")
                .addGet("/download/:*", "download")
                .notFound("404");
    }
}

interface Action { }
class Index implements Action { }
class Show  implements Action { }
