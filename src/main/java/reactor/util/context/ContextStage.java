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

package reactor.util.context;

/**
 * A {@link Context} passing component able to fetch or push an arbitrary context into a
 * dependent components such as a chain of operators.
 *
 * @author Stephane Maldini
 */
public interface ContextStage {

	/**
	 * Synchronously push a {@link Context} to dependent components which can include
	 * downstream operators or terminal {@link org.reactivestreams.Subscriber}.
	 *
	 * @param context a new {@link Context} to propagate
	 */
	void pushContext(Context context);

	/**
	 * Request a {@link Context} from dependent components which can include downstream
	 * operators or a terminal {@link org.reactivestreams.Subscriber}.
	 *
	 * @return a resolved context or {@link Context#empty()}
	 */
	Context pullContext();
}
