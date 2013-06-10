application {
    title = 'JiraLog'
    startupGroups = ['jiraLog']

    // Should Griffon exit when no Griffon created frames are showing?
    autoShutdown = true

    // If you want some non-standard application class, apply it here
    //frameClass = 'javax.swing.JFrame'
}
mvcGroups {
    // MVC Group for "jiraLog"
    'jiraLog' {
        model      = 'jiralog.JiraLogModel'
        view       = 'jiralog.JiraLogView'
        controller = 'jiralog.JiraLogController'
    }

}
