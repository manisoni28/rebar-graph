/**
 * Copyright 2018-2019 Rob Schoening
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rebar.util;

import java.util.concurrent.TimeUnit;

public class Sleep {

	/**
	 * Block forever.
	 */
	public static void forever() {
		while (true == true) {
			sleep(60, TimeUnit.MINUTES);
		}
	}

	public static void sleep(int duration, TimeUnit unit) {
		sleep(unit.toMillis(duration));
	}

	public static void sleep(int ts) {
		sleep((long) ts);
	}

	public static void sleep(long ts) {
		if (ts > 0) {
			try {
				Thread.sleep(ts);
			} catch (InterruptedException ignore) {
				// ignore
			}
		}
	}

}
