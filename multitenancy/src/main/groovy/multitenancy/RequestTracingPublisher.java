package multitenancy;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class RequestTracingPublisher implements Publisher<MutableHttpResponse<?>> {

    private final HttpRequest<?> request;
    private final Publisher<MutableHttpResponse<?>> actual;

    public RequestTracingPublisher(HttpRequest<?> request, Publisher<MutableHttpResponse<?>> actual) {
        this.request = request;
        this.actual = actual;
    }

    @Override
    public void subscribe(Subscriber<? super MutableHttpResponse<?>> subscriber) {
//        System.out.println("RequestTracingPublisher. RequestContext.current() = " + RequestContext.current());
        RequestContext.with(request, new Runnable() {
            @Override
            public void run() {
//                System.out.println("RequestTracingPublisher.run RequestContext.current() = " + RequestContext.current());
                actual.subscribe(new Subscriber<MutableHttpResponse<?>>() {
                    @Override
                    public void onSubscribe(Subscription s) {
//                        System.out.println("RequestTracingPublisher.actual.onSubscribe() = " + RequestContext.current());
                        RequestContext.with(request, () -> {
//                            System.out.println("RequestTracingPublisher.actual.within.onSubscribe() = " + RequestContext.current());
                            subscriber.onSubscribe(s);
                        });

                    }

                    @Override
                    public void onNext(MutableHttpResponse<?> mutableHttpResponse) {
                        RequestContext.with(request, () -> subscriber.onNext(mutableHttpResponse));
                    }

                    @Override
                    public void onError(Throwable t) {
                        RequestContext.with(request, () -> subscriber.onError(t));
                    }

                    @Override
                    public void onComplete() {
                        RequestContext.with(request, () -> subscriber.onComplete());
                    }
                });
            }
        });
    }
}
