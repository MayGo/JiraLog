package jiraLog

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = [
	"url", "username","query"
]))
class JiraConf {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	Long id

	String url
	String username
	String query

	@Override
	public String toString() {
		return query+" : " + username + "@" + url
	}
}
