/**
 * Copyright 2010 Ignite OÜ (www.ignite.ee)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package ee.ignite.logtojira.plugin;

import org.apache.log4j.spi.LoggingEvent;

public class SystemPropertiesPlugin extends AbstractPlugin {

	@Override
	public String getText(LoggingEvent loggingEvent) {
		return super.getText(loggingEvent) + ": " + System.getProperties();
	}

}
