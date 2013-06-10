package jiralog

import com.atlassian.jira.rest.client.domain.SearchResult
import com.atlassian.jira.rest.client.internal.jersey.JerseyJiraRestClientFactory;
import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.JiraRestClientFactory;
import com.atlassian.jira.rest.client.NullProgressMonitor;
import com.atlassian.jira.rest.client.domain.Issue;
import com.atlassian.jira.rest.client.RestClientException
import javax.persistence.EntityManager
import javax.persistence.Query
import jiraLog.JiraConf
import jiraLog.CustomIssue
import griffon.transform.Threading
import groovyx.gpars.GParsPool

class JiraLogController {
	def model
	def view
	final static NullProgressMonitor progressMonitor = new NullProgressMonitor();
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
				model.jiraQuery=jiraConf.query
			}
		}
	}
	def load = { evt = null ->
		String url= model.jiraUrl
		String username= model.jiraUsername
		String password= model.jiraPassword
		String query = model.jiraQuery

		if(!password)execInsideUIAsync{ model.message="Provide JIRA password" }
		if(model.minutesSpentTotal) clear()

		try{
			final JerseyJiraRestClientFactory factory = new JerseyJiraRestClientFactory();
			final URI jiraServerUri = new URI(url);
			final JiraRestClient restClient = factory.createWithBasicHttpAuthentication(jiraServerUri, username, password );

			execInsideUIAsync{ model.message="Starting to search....." }
			SearchResult searchResultForNull = restClient.getSearchClient().searchJql(query, 700, 0, progressMonitor);

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

			execInsideUIAsync{ model.message="Querying....." }

			List<Issue> issues = searchResultForNull.getIssues();
			int total=  issues.size()
			int i=0
			int minutesSpentTotal=0
			List<CustomIssue> outputList
			GParsPool.withPool {
				outputList = issues.collectParallel { iss ->
					i++
					execInsideUIAsync{
						model.message="Proccessing: "+i+"/"+total
					}
					// TODO: query in threadpool
					Issue issue = getIssue(iss.key, restClient)
					def name = issue.summary
					def minutesSpent=issue.worklogs.findAll{it.author?.name==username}?.collect{it.minutesSpent}?.sum()
					if(minutesSpent) minutesSpentTotal+=minutesSpent

					return new CustomIssue(key:issue.key, url:issue.self.toString(), name:issue.summary,minutesSpent:minutesSpent)
				}
				.findAll{it.minutesSpent!=null}//Currently we find all, maybe logged work without hours
			}


			int hours = (int)minutesSpentTotal/60;
			int minutes = (int)minutesSpentTotal%60;
			String minutesSpentTotalFormatted= hours+":"+minutes

			execInsideUIAsync {
				model.minutesSpentTotal=minutesSpentTotalFormatted
				model.customIssues.addAll outputList
			}
		}catch(RestClientException ex){
			execInsideUIAsync{ model.message = ex.message }
		}catch(Exception ex){
			execInsideUIAsync{ model.message = ex.message }
		}
	}

	public static Issue getIssue(String issueKey, JiraRestClient restClient) throws Exception {
		Issue issue = null;
		try{
			issue = restClient.getIssueClient().getIssue(issueKey, progressMonitor);
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
