/*
 * Copyright (c) 2011-2017 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactor.core.publisher;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.BiFunction;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.Exceptions;
import reactor.core.Fuseable;
import reactor.util.Context;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

final class FluxContextualize<T> extends FluxSource<T, T> implements Fuseable {

	final BiFunction<Context, Context, Context> doOnContext;

	FluxContextualize(Flux<? extends T> source,
			BiFunction<Context, Context, Context> doOnContext) {
		super(source);
		this.doOnContext = Objects.requireNonNull(doOnContext, "doOnContext");
	}

	@Override
	public void subscribe(Subscriber<? super T> s) {
		ContextualizeSubscriber<T> cs = new ContextualizeSubscriber<>(s, doOnContext);
		source.subscribe(cs);
		if (cs.context == ContextualizeSubscriber.EMPTY) {
			cs.onContext(EmptyContext.INSTANCE);
		}
	}

	static final class EmptyContext implements Context {

		static final EmptyContext INSTANCE = new EmptyContext();

		@Override
		public Context put(Object key, Object value) {
			Objects.requireNonNull(key, "key");
			return new SingleContext(Tuples.of(key, value));
		}

		@Override
		public <T> T get(Object key) {
			return null;
		}
	}

	static final class SingleContext implements Context {
		final Tuple2<Object, Object> pair;

		SingleContext(Tuple2<Object, Object> pair) {
			this.pair = pair;
		}

		@Override
		public Context put(Object key, Object value) {
			Objects.requireNonNull(key, "key");
			Map<Object, Object> m = new HashMap<>(2, 1f);
			m.put(pair.getT1(), pair.getT2());
			m.put(key, value);
			return new MultiContext(m);
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T get(Object key) {
			return pair.getT1().equals(key) ? (T)pair.getT2() : null;
		}
	}

	static final class MultiContext implements Context {
		final Map<Object, Object> map;

		MultiContext(Map<Object, Object> map) {
			this.map = map;
		}

		@Override
		public Context put(Object key, Object value) {
			Objects.requireNonNull(key, "key");
			Map<Object, Object> m = new HashMap<>(map.size() + 1, 1f);
			m.putAll(map);
			m.put(key, value);
			return new MultiContext(m);
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T get(Object key) {
			return (T)map.get(key);
		}
	}

	static final class ContextualizeSubscriber<T>
			implements ConditionalSubscriber<T>, OperatorContext<T>,
			           QueueSubscription<T> {

		final Subscriber<? super T>                 actual;
		final ConditionalSubscriber<? super T>      actualConditional;
		final BiFunction<Context, Context, Context> doOnContext;

		volatile Context context;

		static final AtomicReferenceFieldUpdater<ContextualizeSubscriber, Context>
				CONTEXT =
				AtomicReferenceFieldUpdater.newUpdater(ContextualizeSubscriber.class,
						Context.class,
						"context");

		static final Context EMPTY = new EmptyContext();

		QueueSubscription<T> qs;
		Subscription         s;

		@SuppressWarnings("unchecked")
		ContextualizeSubscriber(Subscriber<? super T> actual,
				BiFunction<Context, Context, Context> doOnContext) {
			this.actual = actual;
			CONTEXT.lazySet(this, EMPTY);
			this.doOnContext = doOnContext;
			if (actual instanceof ConditionalSubscriber) {
				this.actualConditional = (ConditionalSubscriber<? super T>) actual;
			}
			else {
				this.actualConditional = null;
			}
		}

		@Override
		public void onContext(Context context) {
			try {
				if(this.context == EMPTY){
					context = doOnContext.apply(context, EMPTY);
				}
				else {
					context = doOnContext.apply(this.context, context);
				}
				this.context = context;
				OperatorContext.super.onContext(context);
			}
			catch (Throwable t) {
				Exceptions.throwIfFatal(t);
				actual.onError(Operators.onOperatorError(s, t));
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		public void onSubscribe(Subscription s) {
			if (Operators.validate(this.s, s)) {
				this.s = s;
				if (s instanceof QueueSubscription) {
					this.qs = (QueueSubscription<T>) s;
				}

				actual.onSubscribe(this);
			}
		}

		@Override
		public void onNext(T t) {
			actual.onNext(t);
		}

		@Override
		public boolean tryOnNext(T t) {
			if (actualConditional != null) {
				return actualConditional.tryOnNext(t);
			}
			actual.onNext(t);
			return true;
		}

		@Override
		public void onError(Throwable t) {
			if(CONTEXT.getAndSet(this, EMPTY) != EMPTY) {
				actual.onError(t);
				return;
			}
			Operators.onErrorDropped(t);
		}

		@Override
		public void onComplete() {
			if(CONTEXT.getAndSet(this, EMPTY) != EMPTY) {
				actual.onComplete();
			}
		}

		@Override
		public Subscriber<? super T> actual() {
			return actual;
		}

		@Override
		public void request(long n) {
			s.request(n);
		}

		@Override
		public void cancel() {
			if(CONTEXT.getAndSet(this, EMPTY) != EMPTY) {
				s.cancel();
			}
		}

		@Override
		public int requestFusion(int requestedMode) {
			if (qs == null) {
				return Fuseable.NONE;
			}
			return qs.requestFusion(requestedMode);
		}

		@Override
		public T poll() {
			if (qs != null) {
				return qs.poll();
			}
			return null;
		}

		@Override
		public boolean isEmpty() {
			return qs == null || qs.isEmpty();
		}

		@Override
		public void clear() {
			if (qs != null) {
				qs.clear();
			}
		}

		@Override
		public int size() {
			return qs != null ? qs.size() : 0;
		}
	}

}
