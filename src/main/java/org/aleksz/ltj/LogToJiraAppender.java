package org.aleksz.ltj;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.rpc.ServiceException;

import org.aleksz.ltj.plugin.Plugin;
import org.aleksz.ltj.soap.JiraSoapService;
import org.aleksz.ltj.soap.JiraSoapServiceServiceLocator;
import org.aleksz.ltj.soap.RemoteAuthenticationException;
import org.aleksz.ltj.soap.RemoteComment;
import org.aleksz.ltj.soap.RemoteIssue;
import org.aleksz.ltj.soap.RemotePermissionException;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.ErrorCode;
import org.apache.log4j.spi.LoggingEvent;

public class LogToJiraAppender extends AppenderSkeleton {

	private Config config = new Config();
	private AppenderService service;
	private JiraSoapServiceServiceLocator jiraSoapServiceServiceLocator;
	private JiraSoapService jiraSoapService;
	private boolean enabled = true;
	private List<Plugin> plugins = new ArrayList<Plugin>();

	@Override
	protected void append(LoggingEvent loggingEvent) {

		if (!enabled) {
			return;
		}

		try {

			String token = getJiraService().login(config.getUsername(), config.getPassword());

			RemoteIssue newIssue = getService().createIssue(loggingEvent);
			RemoteIssue duplicate = getService().getLatestDuplicate(newIssue, token);

			if (duplicate == null) {
				newIssue = getJiraService().createIssue(token, newIssue);
				applyPlugins(newIssue, loggingEvent, token);
			} else {
				applyPlugins(duplicate, loggingEvent, token);
			}

			getJiraService().logout(token);

		} catch (RemoteAuthenticationException e) {
			errorHandler.error("JIRA auth failed", e, ErrorCode.GENERIC_FAILURE, loggingEvent);
		} catch (RemoteException e) {
			errorHandler.error("JIRA problem", e, ErrorCode.GENERIC_FAILURE, loggingEvent);
		}
	}

	private void applyPlugins(RemoteIssue issue, LoggingEvent event, String token)
			throws RemotePermissionException, RemoteAuthenticationException,
				org.aleksz.ltj.soap.RemoteException, RemoteException {

		for (Plugin plugin : plugins) {
			RemoteComment comment = getService().createComment(plugin, event);
			if (!getService().duplicateExists(comment, issue, token)) {
				getJiraService().addComment(token, issue.getKey(), comment);
			}
		}
	}

	@Override
	public void close() {
	}

	@Override
	public boolean requiresLayout() {
		return false;
	}

	public void setUrl(String url) {
		jiraSoapServiceServiceLocator = new JiraSoapServiceServiceLocator();
		try {
			this.jiraSoapService = jiraSoapServiceServiceLocator.getJirasoapserviceV2(new URL(url));
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(e);
		} catch (ServiceException e) {
			errorHandler.error("JIRA connection problem", e, ErrorCode.GENERIC_FAILURE);
		}
	}

	protected JiraSoapService getJiraService() {
		return jiraSoapService;
	}

	protected AppenderService getService() {

		if (service != null) {
			return service;
		}

		service = new AppenderServiceImpl(config, jiraSoapService);

		return service;
	}

	public void setUsername(String username) {
		config.setUsername(username);
	}

	public void setPassword(String password) {
		config.setPassword(password);
	}

	public void setProject(String project) {
		config.setProject(project);
	}

	public void setIssueTypeId(String issueTypeId) {
		config.setIssueTypeId(issueTypeId);
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setPlugins(List<Plugin> plugins) {
		this.plugins = plugins;
	}

	public void setPlugins(String commaSeparatedPlugins) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		for (String plugin : commaSeparatedPlugins.split(",")) {
			plugins.add((Plugin) Class.forName(plugin).newInstance());
		}
	}
}
