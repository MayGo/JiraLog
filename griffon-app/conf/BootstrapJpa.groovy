import javax.persistence.EntityManager
import jiraLog.JiraConf
class BootstrapJpa {
    def init = { String persistenceUnit, EntityManager em ->  
        em.getTransaction().begin()  
        [[url: 'https://sample.atlassian.net',  username: 'sampleuser', query:'project = SAMPLE']].each { data ->  
            em.persist(new JiraConf(data))  
        }  
        em.getTransaction().commit()  
    }  
  
    def destroy = { String persistenceUnit, EntityManager em ->  
    }  
} 
