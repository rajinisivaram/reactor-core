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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

final class MultiContext implements Context {

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
		return (T) map.get(key);
	}
}
