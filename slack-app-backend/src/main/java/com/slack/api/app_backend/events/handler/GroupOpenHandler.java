package com.slack.api.app_backend.events.handler;

import com.slack.api.app_backend.events.EventHandler;
import com.slack.api.app_backend.events.payload.GroupOpenPayload;
import com.slack.api.model.event.GroupOpenEvent;

public abstract class GroupOpenHandler extends EventHandler<GroupOpenPayload> {

    @Override
    public String getEventType() {
        return GroupOpenEvent.TYPE_NAME;
    }
}
