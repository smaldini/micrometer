package io.micrometer.core.samples;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.netflix.spectator.atlas.AtlasConfig;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.atlas.AtlasMeterRegistry;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Scannable;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.scheduler.Schedulers;
import reactor.util.Logger;
import reactor.util.Loggers;

/**
 * @author Stephane Maldini
 */
public class ReactorSample {

	static final Logger log = Loggers.getLogger(ReactorSample.class);

	public static void main(String[] args) throws Exception {
		MeterRegistry registry = new AtlasMeterRegistry(new AtlasConfig() {
			@Override
			public Duration step() {
				return Duration.ofSeconds(3);
			}

			@Override
			public String get(String k) {
				return null;
			}

			@Override
			public String uri() {
				return "https://jon-atlas-cf.cfapps.io/api/v1/publish";
			}
		}, Clock.SYSTEM);

		Hooks.onNewSubscriber((pub, sub) -> new MetricSubscriber<>(pub, sub, registry));

		Flux.range(1, 30)
		    .hide()
		    .publishOn(Schedulers.parallel())
		    .subscribe((Subscriber<Integer>) new BaseSubscriber<Integer>() {
			    @Override
			    protected void hookOnSubscribe(Subscription subscription) {

				    Flux.interval(Duration.ofMillis(500))
				        .subscribe(i -> subscription.request(1));
			    }

		    });


		Flux.range(1, 30)
		    .hide()
		    .publishOn(Schedulers.parallel())
		    .subscribe((Subscriber<Integer>) new BaseSubscriber<Integer>() {
			    @Override
			    protected void hookOnSubscribe(Subscription subscription) {

				    Flux.interval(Duration.ofMillis(500))
				        .subscribe(i -> subscription.request(1));
			    }

		    });

		System.in.read();

	}

	//gauge test
	//https://jon-atlas-cf.cfapps.io/api/v1/graph?l=0&u.0=100&s=e-30m&q=name,reactive_queueBuffered,:eq,name,reactive_queueCapacity,:eq,:div,100,:mul,2,:lw,name,reactive_queueBuffered,:eq,2,:axis
	//timer test
	//http://jon-atlas-cf.cfapps.io/api/v1/graph?s=e-7m&ylabel.0=Latency+(seconds)&ylabel.1=Throughput+(requests/second)&title=Playback+Start+Latency&w=1140&layout=iw&q=name,reactor_itemLatency,:eq,:dist-avg,0,:axis,2,:lw,name,reactor_itemLatency,:eq,statistic,count,:eq,:and,1,:axis
	static final class MetricSubscriber<T> implements CoreSubscriber<T> {

		final CoreSubscriber<T> actual;
		final MeterRegistry     registry;
		final Timer             itemTimer;
		final Timer             flowTimer;
		final List<Tag>         tags;
		final long              startTime;

		long              itemTime;

		MetricSubscriber(Publisher<T> pub,
				CoreSubscriber<T> actual,
				MeterRegistry registry) {
			this.actual = actual;
			this.registry = registry;
			this.tags = Collections.singletonList(Tag.of("publisher", pub.toString() +"_"+actual.toString()));
			this.startTime = registry.getClock()
			                         .monotonicTime();
			this.itemTime = startTime;

			this.flowTimer = registry.timer("reactor_flowLatency", tags);
			this.itemTimer = registry.timer("reactor_itemLatency", tags);

			log.debug("Instrumenting {}", pub);
		}

		@Override
		public void onSubscribe(Subscription sub) {
			Stream.concat(Stream.of(Scannable.from(sub)),
					Scannable.from(sub)
					         .parents())
			      .forEach(s -> {
				      if (s.scanUnsafe(Scannable.Attr.PREFETCH) != null) {

					      registry.gauge("reactor_queueCapacity",
							      tags,
							      s,
							      sc -> sc.scan(Scannable.Attr.PREFETCH));
					      registry.gauge("reactor_queueBuffered",
							      tags,
							      s,
							      sc -> sc.scan(Scannable.Attr.BUFFERED));

				      }
			      });

			actual.onSubscribe(sub);
		}

		@Override
		public void onNext(T t) {
			long current = registry.getClock().monotonicTime();

			itemTimer.record(current - itemTime, TimeUnit.NANOSECONDS);

			itemTime = current;

			actual.onNext(t);
		}

		@Override
		public void onError(Throwable t) {
			flowTimer.record(registry.getClock()
			                         .monotonicTime() - startTime, TimeUnit.NANOSECONDS);
			actual.onError(t);
		}

		@Override
		public void onComplete() {
			flowTimer.record(registry.getClock()
			                         .monotonicTime() - startTime, TimeUnit.NANOSECONDS);
			actual.onComplete();
		}
	}
}
