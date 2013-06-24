package jiralog


import com.atlassian.jira.rest.client.JiraRestClient
import com.atlassian.jira.rest.client.JiraRestClientFactory
import com.atlassian.jira.rest.client.RestClientException
import com.atlassian.jira.rest.client.SearchRestClient
import com.atlassian.jira.rest.client.api.*;
import com.atlassian.jira.rest.client.api.domain.*;
import com.atlassian.jira.rest.client.auth.*;
import com.atlassian.jira.rest.client.domain.BasicIssue
import com.atlassian.jira.rest.client.domain.Issue
import com.atlassian.jira.rest.client.domain.SearchResult
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.atlassian.util.concurrent.Promise;


import javax.persistence.EntityManager
import javax.persistence.Query
import jiraLog.JiraConf
import jiraLog.CustomIssue
import griffon.transform.Threading
import groovyx.gpars.GParsPool

class JiraLogController {
	def model
	def view
	void mvcGroupInit(Map args) {
		// this method is called after model and view are injected


		withJpa { String persistenceUnit, EntityManager em ->
			List<JiraConf> tmpList = []
			tmpList.addAll em.createQuery('select p from JiraConf p order by p.id').resultList
			execInsideUIAsync { model.jiraConfs.addAll tmpList }
		}
		//load()
	}

	// void mvcGroupDestroy() {
	//    // this method is called when the group is destroyed
	// }

	def loadConf={evt = null ->
		JiraConf jiraConf = model.selectedJiraConf
		if(jiraConf){
			execInsideUIAsync {
				model.jiraUrl=jiraConf.url
				model.jiraUsername=jiraConf.username
				model.jiraWorklogAuthor=jiraConf.worklogAuthor
				model.jiraQuery=jiraConf.query
			}
		}
	}

	def load = { evt = null ->
		String url= model.jiraUrl
		String username= model.jiraUsername
		String worklogAuthor= (model.jiraWorklogAuthor)?:model.jiraUsername
		String password= model.jiraPassword
		String query = model.jiraQuery

		if(!password)execInsideUIAsync{ model.message="Provide JIRA password" }
		if(model.minutesSpentTotal) clear()

		final JiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
		final URI jiraServerUri = new URI(url);
		final JiraRestClient restClient = factory.createWithBasicHttpAuthentication(jiraServerUri, username, password );
		SearchRestClient searchClient = restClient.getSearchClient();
		
		try{

			execInsideUIAsync{ model.message="Starting to search....." }

			Promise<SearchResult> sizeQuery = searchClient.searchJql(query, (int)1, (int)0);
			SearchResult sizeQueryResult = sizeQuery.get();
			int totalSize = sizeQueryResult.getTotal();
			execInsideUIAsync{ model.message="Querying....." }

			int minutesSpentTotal=0
			int totalResultsReceived = 0

			List<BasicIssue> issues = fetchAllIssues(restClient.getSearchClient(), query)

			List<CustomIssue> outputList

			outputList = issues.collect { iss ->
				totalResultsReceived++
				execInsideUIAsync{
					model.message="Proccessing: "+totalResultsReceived+"/"+totalSize
				}
				Issue issue = getIssue(iss.key, restClient)
				def name = issue.summary
				def minutesSpent=issue.worklogs.findAll{it.author?.name == worklogAuthor}?.collect{it.minutesSpent}?.sum()
				if(minutesSpent) minutesSpentTotal+=minutesSpent

				return new CustomIssue(key:issue.key, url:issue.self.toString(), name:issue.summary,minutesSpent:minutesSpent)
			}
			.findAll{it.minutesSpent!=null}//Currently we find all, maybe logged work without hours

			// Save query details to DB
			withJpa { String persistenceUnit, EntityManager em ->
				try{
					em.getTransaction().begin()
					JiraConf jiraConf=new JiraConf(url:url, username:username, query:query)
					em.persist(jiraConf)
					em.getTransaction().commit()
					execInsideUIAsync { model.jiraConfs.add jiraConf }
				}catch(Exception ex){
					//Probably "Unique index or primary key violation"
					log.error ex.message
				}
			}

			//Display all spent hours and all issues
			int hours = (int)minutesSpentTotal / 60;
			int minutes = (int)minutesSpentTotal % 60;
			String minutesSpentTotalFormatted = hours+":"+minutes

			execInsideUIAsync {
				model.minutesSpentTotal=minutesSpentTotalFormatted
				model.customIssues.addAll outputList
			}
		}catch(RestClientException ex){
			log.error ex
			execInsideUIAsync{ model.message = ex.message }
		}catch(Exception ex){
			log.error ex.message
			execInsideUIAsync{ model.message = ex.message }
		}
	}

	/**
	 * Function to fetch all issues for given query
	 * @param searchClient
	 * @param query
	 * @return
	 */
	private List<BasicIssue> fetchAllIssues(SearchRestClient searchClient, String query) {
		int BATCH_SIZE = 100
		List<BasicIssue> matchingIssues = []

		int loadedIssues = 0
		boolean allLoaded = false
		int startAt = 0

		while(!allLoaded) {
			SearchResult batchResults = searchClient.searchJql(query, BATCH_SIZE, startAt).claim()
			List<BasicIssue> issuesInThisBatch = batchResults.getIssues()
			matchingIssues.addAll(issuesInThisBatch)

			startAt += BATCH_SIZE
			loadedIssues += issuesInThisBatch.size()
			allLoaded = batchResults.getTotal() <= loadedIssues
		}
		return matchingIssues
	}

	/**
	 * Function to get issue details
	 * @param issueKey
	 * @param restClient
	 * @return
	 * @throws Exception
	 */
	public static Issue getIssue(String issueKey, JiraRestClient restClient) throws Exception {
		Issue issue = null;
		try{
			issue = restClient.getIssueClient().getIssue(issueKey).get();
		}
		catch(Exception e){
			e.printStackTrace();
			throw new Exception("Exception thrown trying to lookup issue '" + issueKey + "'");
		}

		return issue;
	}

	@Threading(Threading.Policy.SKIP)
	void clear(evt) {
		model.message =""
		model.minutesSpentTotal = "0"
		model.customIssues.clear()
	}
}
