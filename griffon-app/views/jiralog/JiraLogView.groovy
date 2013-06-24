package jiralog
import javafx.beans.*
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import static javafx.geometry.HPos.RIGHT
import static javafx.geometry.VPos.BASELINE



application(title: 'Jira Logged Hours', sizeToScene: true, centerOnScreen: true) {
	scene(fill: lightgray, width: 900, height: 400) {

		borderPane {

			top {
				titledPane(id: "t1") {
					title { label("Provide JIRA connect parameters!") }
					content {
						gridPane {

							comboBox id: 'jiraConfigsSelect', row:0, column: 0, items: model.jiraConfs, columnSpan:2, onAction:controller.loadConf

							text 'Url', row: 1, column: 0
							textField id: 'jiraUrlText', row:1, column: 1,text: bind(model.jiraUrlProperty),onAction: controller.load

							text 'Username', row: 2, column: 0
							textField id: 'jiraUsernameText', row:2, column: 1 ,text: bind(model.jiraUsernameProperty),onAction: controller.load

							text 'Password', row: 3, column: 0
							passwordField id: 'jiraPasswordText', row: 3, column: 1,text: bind(model.jiraPasswordProperty),onAction: controller.load

							text 'Query', row: 4, column: 0, valignment: BASELINE
							textField id: 'jiraQueryText', row: 4, column: 1,text: bind(model.jiraQueryProperty),prefWidth: 400,onAction: controller.load

							text 'Worklog Author', row: 5, column: 0
							textField id: 'jiraWorklogAuthorText', row:5, column: 1 ,text: bind(model.jiraWorklogAuthorProperty),onAction: controller.load

							
							button id: 'submit', row: 6, column: 1, halignment: RIGHT,
							"Connect & Load",onAction: controller.load
						}
					}
				}
			}
			tv = tableView(selectionMode: 'single', cellSelectionEnabled: true, editable:true, items: model.customIssues) {
				tableColumn(editable: true, property: 'key', text: 'Key', prefWidth: 55)
				tableColumn(editable: true, property: 'url', text: 'Url', prefWidth: 100)
				tableColumn(editable: true, property: 'name', text: 'name', prefWidth: 400)
				tableColumn(editable: true, property: 'minutesSpentFormatted', text: 'Logged work', prefWidth: 50)
			}
			bottom {
				toolBar {
					text(text: 'Total Hours:')
					text(id:'minutesSpentTotalText',text: bind(model.minutesSpentTotalProperty))

					text(id:'messageText',text: bind(model.messageProperty))
				}
			}
		}
		noparent {
			minutesSpentTotalText.textProperty().addListener({ ObservableValue<? extends String> observable, String oldValue, String newValue ->
				observable.setValue(newValue);
			} as ChangeListener<String>)
			messageText.textProperty().addListener({ ObservableValue<? extends String> observable, String oldValue, String newValue ->
				observable.setValue(newValue);
			} as ChangeListener<String>)
			model.customIssues.addListener({
				tv.items.clear()
				tv.items.addAll(model.customIssues)
			} as InvalidationListener)
			model.jiraConfs.addListener({
				jiraConfigsSelect.items.clear()
				jiraConfigsSelect.items.addAll(model.jiraConfs)
			} as InvalidationListener)

			model.selectedJiraConfProperty.bind(jiraConfigsSelect.selectionModel.selectedItemProperty())
		}
	}
}

