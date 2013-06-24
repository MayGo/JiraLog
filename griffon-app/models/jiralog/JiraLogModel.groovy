package jiralog

import groovyx.javafx.beans.FXBindable
import griffon.util.GriffonNameUtils
import javafx.collections.FXCollections
import jiraLog.JiraConf
class JiraLogModel {
	List customIssues = FXCollections.observableList([])
	List jiraConfs = FXCollections.observableList([])
	@FXBindable JiraConf selectedJiraConf
	@FXBindable String minutesSpentTotal = "0."
	@FXBindable String jiraUrl        =""
	@FXBindable String jiraUsername   =""
	@FXBindable String jiraWorklogAuthor   =""
	@FXBindable String jiraPassword     =""
	@FXBindable String jiraQuery     =""
	@FXBindable String message     =""


	void mvcGroupInit(Map args) {
	}
}
