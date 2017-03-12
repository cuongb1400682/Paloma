package cit.edu.paloma.datamodals;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by charlie on 3/12/17.
 */

public class ChatGroup {
    private String groupId;
    private String groupName;
    private List<String> members;
    private List<Object> messages;
    private long timestamp;

    public ChatGroup() {
    }

    public ChatGroup(String groupId, String groupName, List<String> members, List<Object> messages, long timestamp) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.members = members;
        this.messages = messages;
        this.timestamp = timestamp;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    public List<Object> getMessages() {
        return messages;
    }

    public void setMessages(List<Object> messages) {
        this.messages = messages;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
