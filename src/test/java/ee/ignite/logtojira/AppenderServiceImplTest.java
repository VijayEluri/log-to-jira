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
package ee.ignite.logtojira;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.rmi.RemoteException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import ee.ignite.logtojira.AppenderService;
import ee.ignite.logtojira.AppenderServiceImpl;
import ee.ignite.logtojira.Config;
import ee.ignite.logtojira.Util;
import ee.ignite.logtojira.soap.JiraSoapService;
import ee.ignite.logtojira.soap.RemoteIssue;


public class AppenderServiceImplTest {

	private static final String SUMMARY = "the summary";
	private static final String DECRIPTION = "the description";
	private static final String TRICKY_SUMMARY = "?!~\t\n,.*/-@#$%^&()_+фвмÄÜÕ";
	private static final String TRICKY_DECRIPTION = "@#$%^&()_+фвмÄÜÕ?!~\t\n,.*/-";
	private static final String PROJECT = "TST";
	private static final String ISSUE_TYPE = "1";
	private static final String TOKEN = "tokenValue";

	private static final String DUPLICATE_JQL =
		"project = " + PROJECT +
		" AND summary ~ \"\\\"" + SUMMARY + "\\\"\"" +
		" AND description IS EMPTY" +
		" AND status in (Open, \"In Progress\", Reopened)" +
		" ORDER BY created";

	private static final String DUPLICATE_WITH_DESCRIPTION_JQL =
		"project = " + PROJECT +
		" AND summary ~ \"\\\"" + SUMMARY + "\\\"\"" +
		" AND description ~ \"\\\"" + DECRIPTION + "\\\"\"" +
		" AND status in (Open, \"In Progress\", Reopened)" +
		" ORDER BY created";

	private static final String DUPLICATE_WITH_DESCRIPTION_TRICKY_JQL =
		"project = " + PROJECT +
		" AND summary ~ \"\\\"?!~\\t\\n,.*/-@#$%^&()_+\\u0444\\u0432\\u043C\\u00C4\\u00DC\\u00D5\\\"\"" +
		" AND description ~ \"\\\"@#$%^&()_+\\u0444\\u0432\\u043C\\u00C4\\u00DC\\u00D5?!~\\t\\n,.*/-\\\"\"" +
		" AND status in (Open, \"In Progress\", Reopened)" +
		" ORDER BY created";

	private AppenderService service;
	private Config config;
	private JiraSoapService jiraService;

	@Before
	public void init() {
		config = new Config();
		config.setProject(PROJECT);
		config.setIssueTypeId(ISSUE_TYPE);
		jiraService = EasyMock.createMock(JiraSoapService.class);
		service = new AppenderServiceImpl(config, jiraService);
	}

	@Test
	public void duplicateDoesNotExist() throws ee.ignite.logtojira.soap.RemoteException, RemoteException {
		RemoteIssue issue = new RemoteIssue();
		issue.setSummary(SUMMARY);
		issue.setDescription(DECRIPTION);

		expect(jiraService.getIssuesFromJqlSearch(TOKEN, DUPLICATE_WITH_DESCRIPTION_JQL, 1))
				.andReturn(new RemoteIssue[] {});
		replay(jiraService);

		assertFalse(service.duplicateExists(issue, TOKEN));
		verify(jiraService);
	}

	@Test
	public void duplicateBySummaryExists() throws RemoteException {
		RemoteIssue issue = new RemoteIssue();
		issue.setSummary(SUMMARY);

		expect(jiraService.getIssuesFromJqlSearch(TOKEN, DUPLICATE_JQL, 1))
				.andReturn(new RemoteIssue[] { issue });
		replay(jiraService);

		assertTrue(service.duplicateExists(issue, TOKEN));
		verify(jiraService);
	}

	@Test
	public void duplicateDetectionHandlesSpecialChars() throws ee.ignite.logtojira.soap.RemoteException, RemoteException {
		RemoteIssue issue = new RemoteIssue();
		issue.setSummary(TRICKY_SUMMARY);
		issue.setDescription(TRICKY_DECRIPTION);

		expect(jiraService.getIssuesFromJqlSearch(TOKEN, DUPLICATE_WITH_DESCRIPTION_TRICKY_JQL, 1))
				.andReturn(new RemoteIssue[] {});
		replay(jiraService);

		service.duplicateExists(issue, TOKEN);
		verify(jiraService);
	}

	@Test
	public void createIssueWithoutException() throws RemoteException {
		RemoteIssue result = service.createIssue(createTestLoggingEvent());
		assertEquals("", result.getDescription());
	}

	@Test
	public void createIssue() throws RemoteException {
		Throwable e = new NullPointerException();
		LoggingEvent logEvent = createTestLoggingEvent(e);
		RemoteIssue result = service.createIssue(logEvent);
		assertEquals(config.getProject(), result.getProject());
		assertEquals(config.getIssueTypeId(), result.getType());
		assertEquals(logEvent.getRenderedMessage(), result.getSummary());
		assertEquals(Util.toString(e), result.getDescription());
	}

	private LoggingEvent createTestLoggingEvent(Throwable exception) {
		Logger log = Logger.getLogger(AppenderServiceImplTest.class);
		return new LoggingEvent(null, log, Level.ERROR, "tstmsg", exception);
	}

	private LoggingEvent createTestLoggingEvent() {
		return createTestLoggingEvent(null);
	}
}
