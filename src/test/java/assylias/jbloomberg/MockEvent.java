/*
 * Copyright (C) 2012 - present by Yann Le Tallec.
 * Please see distribution for license.
 */
package assylias.jbloomberg;

import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Event.EventType;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.MessageIterator;
import com.bloomberglp.blpapi.impl.eY;
import com.bloomberglp.blpapi.impl.eZ;
import java.util.Iterator;
import java.util.List;
import mockit.Mock;
import mockit.MockUp;

/**
 *
 * @author Yann Le Tallec
 */
public class MockEvent extends Event {

    private final EventType type;
    private final List<Message> messages;

    public MockEvent(EventType type, List<Message> messages) {
        this.type = type;
        this.messages = messages;
    }

    @Mock
    public EventType eventType() {
        return type;
    }

    @Mock
    public Iterator<Message> iterator() {
        return messages.iterator();
    }

    @Override
    public boolean isValid() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected eZ t() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
