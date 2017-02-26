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

import org.reactivestreams.Subscriber;
import reactor.util.context.Context;
import reactor.util.context.ContextStage;

/**
 * @author Stephane Maldini
 */
public interface OperatorContext<T> extends ContextStage {

	Subscriber<? super T> actual();

	@SuppressWarnings("unchecked")
	@Override
	default void pushContext(Context context) {
		Subscriber<? super T> a = actual();
		if(a != this && a instanceof ContextStage){
			((ContextStage)a).pushContext(context);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	default Context pullContext() {
		Subscriber<? super T> a = actual();
		if(a != this && a instanceof ContextStage){
			return ((ContextStage)a).pullContext();
		}
		return Context.empty();
	}
}
