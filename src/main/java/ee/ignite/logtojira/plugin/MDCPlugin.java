package ee.ignite.logtojira.plugin;

import org.apache.log4j.spi.LoggingEvent;

public class MDCPlugin extends AbstractPlugin {

	@Override
	public String getText(LoggingEvent loggingEvent) {
		return super.getText(loggingEvent) + ": " + loggingEvent.getProperties().toString();
	}

}
