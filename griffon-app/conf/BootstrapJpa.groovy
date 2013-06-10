import javax.persistence.EntityManager
import jiraLog.JiraConf
class BootstrapJpa {
    def init = { String persistenceUnit, EntityManager em ->  
        em.getTransaction().begin()  
        [[url: 'https://sample.atlassian.net',  username: 'maigo', query:'project = SAMPLE AND watcher = currentUser()']].each { data ->  
            em.persist(new JiraConf(data))  
        }  
        em.getTransaction().commit()  
    }  
  
    def destroy = { String persistenceUnit, EntityManager em ->  
    }  
} 
