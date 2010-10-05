package org.aleksz.ltj;

import java.rmi.RemoteException;

import org.aleksz.ltj.soap.JiraSoapService;
import org.aleksz.ltj.soap.RemoteIssue;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.spi.LoggingEvent;

public class AppenderServiceImpl implements AppenderService {

	private Config config;
	private final JiraSoapService jiraService;

	public AppenderServiceImpl(Config config, JiraSoapService jiraService) {
		this.config = config;
		this.jiraService = jiraService;
	}

	@Override
	public RemoteIssue createIssue(LoggingEvent loggingEvent) {
		RemoteIssue result = new RemoteIssue();

		result.setProject(config.getProject());
		result.setType(config.getIssueTypeId());
		result.setSummary(loggingEvent.getRenderedMessage());
		result.setDescription(composeDescription(loggingEvent));

		return result;
	}

	private String composeDescription(LoggingEvent loggingEvent) {

		StringBuilder result = new StringBuilder();

		if (loggingEvent.getThrowableInformation() != null) {
			result.append(Util.toString(loggingEvent.getThrowableInformation().getThrowable()));
		}

		return result.toString();
	}

	@Override
	public boolean duplicateExists(RemoteIssue issue, String token) throws RemoteException, RemoteException {

		StringBuilder JQL = new StringBuilder();
		JQL.append("project = ");
		JQL.append(config.getProject());
		JQL.append(" AND summary ~ \"\\\"");
		JQL.append(StringEscapeUtils.escapeJava(issue.getSummary()));
		JQL.append("\\\"\" AND description ");
		if (StringUtils.isBlank(issue.getDescription())) {
			JQL.append("IS EMPTY");
		} else {
			JQL.append("~ \"\\\"");
			JQL.append(StringEscapeUtils.escapeJava(issue.getDescription()));
			JQL.append("\\\"\"");
		}
		JQL.append(" AND status in (Open, \"In Progress\", Reopened)");

		return jiraService.getIssuesFromJqlSearch(token, JQL.toString(), 1).length > 0;
	}
}
