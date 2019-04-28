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
package io.netty.example.http.router;


import io.github.spafka.rest.netty.handler.codec.http.router.RouteResult;
import io.github.spafka.rest.netty.handler.codec.http.router.Router;
import org.apache.flink.shaded.netty4.io.netty.buffer.Unpooled;
import org.apache.flink.shaded.netty4.io.netty.channel.ChannelFutureListener;
import org.apache.flink.shaded.netty4.io.netty.channel.ChannelHandler;
import org.apache.flink.shaded.netty4.io.netty.channel.ChannelHandlerContext;
import org.apache.flink.shaded.netty4.io.netty.channel.SimpleChannelInboundHandler;
import org.apache.flink.shaded.netty4.io.netty.handler.codec.http.*;
import org.apache.flink.shaded.netty4.io.netty.util.CharsetUtil;

@ChannelHandler.Sharable
public class HttpRouterServerHandler extends SimpleChannelInboundHandler<HttpRequest> {
    // For simplicity of this example, route targets are just simple strings.
    // But you can make them classes, and here once you get a target class,
    // you can create an instance of it and dispatch the request to the instance etc.
    private final Router<String> router;

    HttpRouterServerHandler(Router<String> router) {
        this.router = router;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpRequest req) {
        if (HttpUtil.is100ContinueExpected(req)) {
            ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
            return;
        }

        HttpResponse res = createResponse(req, router);
        flushResponse(ctx, req, res);
    }

    // Display debug info.
    private static HttpResponse createResponse(HttpRequest req, Router<String> router) {
        RouteResult<String> routeResult = router.route(req.method(), req.uri());

        String content =
                "router: \n" + router + "\n" +
                "req: " + req + "\n\n" +
                "routeResult: \n" +
                "target: " + routeResult.target() + "\n" +
                "pathParams: " + routeResult.pathParams() + "\n" +
                "queryParams: " + routeResult.queryParams() + "\n\n" +
                "allowedMethods: " + router.allowedMethods(req.uri());

        FullHttpResponse res = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                Unpooled.copiedBuffer(content, CharsetUtil.UTF_8)
        );

        res.headers().set(HttpHeaderNames.CONTENT_TYPE,   "text/plain");
        res.headers().set(HttpHeaderNames.CONTENT_LENGTH, res.content().readableBytes());

        return res;
    }

    private static void flushResponse(ChannelHandlerContext ctx, HttpRequest req, HttpResponse res) {
        if (!HttpUtil.isKeepAlive(req)) {
            ctx.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
        } else {
            res.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            ctx.writeAndFlush(res);
        }
    }
}
