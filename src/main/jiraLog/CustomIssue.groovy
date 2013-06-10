package jiraLog

class CustomIssue {
    String url
    String key
    String name
    Integer minutesSpent

    public String getMinutesSpentFormatted(){
        int hours = (int)minutesSpent/60;
        int minutes = (int)minutesSpent%60;
        return hours+":"+minutes
    }
}
