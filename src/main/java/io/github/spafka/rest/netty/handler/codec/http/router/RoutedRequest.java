package io.github.spafka.rest.netty.handler.codec.http.router;

import org.apache.flink.shaded.netty4.io.netty.handler.codec.http.HttpRequest;
import org.apache.flink.shaded.netty4.io.netty.handler.codec.http.QueryStringDecoder;
import org.apache.flink.shaded.netty4.io.netty.util.ReferenceCountUtil;
import org.apache.flink.shaded.netty4.io.netty.util.ReferenceCounted;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Class for handling {@link HttpRequest} with associated {@link RouteResult}.
 */
public class RoutedRequest<T> implements ReferenceCounted {
    private final RouteResult<T> result;
    private final HttpRequest request;

    private final Optional<ReferenceCounted> requestAsReferenceCounted;
    private final QueryStringDecoder queryStringDecoder;

    public RoutedRequest(RouteResult<T> result, HttpRequest request) {
        this.result = requireNonNull(result);
        this.request = requireNonNull(request);
        this.requestAsReferenceCounted = Optional.ofNullable((request instanceof ReferenceCounted) ? (ReferenceCounted) request : null);
        this.queryStringDecoder = new QueryStringDecoder(request.uri());
    }

    public RouteResult<T> getRouteResult() {
        return result;
    }

    public HttpRequest getRequest() {
        return request;
    }

    public String getPath() {
        return queryStringDecoder.path();
    }

    @Override
    public int refCnt() {
        if (requestAsReferenceCounted.isPresent()) {
            return requestAsReferenceCounted.get().refCnt();
        }
        return 0;
    }

    @Override
    public boolean release() {
        if (requestAsReferenceCounted.isPresent()) {
            return requestAsReferenceCounted.get().release();
        }
        return true;
    }

    @Override
    public boolean release(int arg0) {
        if (requestAsReferenceCounted.isPresent()) {
            return requestAsReferenceCounted.get().release(arg0);
        }
        return true;
    }

    @Override
    public ReferenceCounted retain() {
        if (requestAsReferenceCounted.isPresent()) {
            requestAsReferenceCounted.get().retain();
        }
        return this;
    }

    @Override
    public ReferenceCounted retain(int arg0) {
        if (requestAsReferenceCounted.isPresent()) {
            requestAsReferenceCounted.get().retain(arg0);
        }
        return this;
    }

    @Override
    public ReferenceCounted touch() {
        if (requestAsReferenceCounted.isPresent()) {
            ReferenceCountUtil.touch(requestAsReferenceCounted.get());
        }
        return this;
    }

    @Override
    public ReferenceCounted touch(Object hint) {
        if (requestAsReferenceCounted.isPresent()) {
            ReferenceCountUtil.touch(requestAsReferenceCounted.get(), hint);
        }
        return this;
    }
}
