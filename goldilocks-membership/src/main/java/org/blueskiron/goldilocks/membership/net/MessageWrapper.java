package org.blueskiron.goldilocks.membership.net;

import org.blueskiron.goldilocks.api.messages.AppendEntriesRequest;
import org.blueskiron.goldilocks.api.messages.AppendEntriesResponse;
import org.blueskiron.goldilocks.api.messages.VoteRequest;
import org.blueskiron.goldilocks.api.messages.VoteResponse;

import io.advantageous.boon.json.JsonFactory;
import io.advantageous.boon.json.ObjectMapper;

/**
 * @author jzachar
 *
 */
public class MessageWrapper {

    private static final ObjectMapper mapper = JsonFactory.create();
    private String className;
    private String payload;

    // default constructor
    private MessageWrapper() {

    }

    private MessageWrapper(String className, String payload) {
        this.className = className;
        this.payload = payload;
    }

    public static String wrap(Object message) {
        if (message instanceof VoteRequest) {
            return wrap(message, VoteRequest.class);
        } else if (message instanceof VoteResponse) {
            return wrap(message, VoteResponse.class);
        } else if (message instanceof AppendEntriesRequest) {
            return wrap(message, AppendEntriesRequest.class);
        } else if (message instanceof AppendEntriesResponse) {
            return wrap(message, AppendEntriesResponse.class);
        }
        else {
            // Eeek!
            return null;
        }
    }

    private static String wrap(Object message, Class<?> klazz) {
        MessageWrapper wrappedMessage =
                new MessageWrapper(klazz.getName(), mapper.writeValueAsString(message));
        return mapper.writeValueAsString(wrappedMessage);
    }

    public static MessageWrapper unwrap(String content) {
        return mapper.readValue(content, MessageWrapper.class);
    }

    public static Object unwrap(MessageWrapper message) throws ClassNotFoundException {
        Object obj = mapper.readValue(message.getPayload(), Class.forName(message.getClassName()));
        return obj;
    }

    public String getClassName() {
        return className;
    }

    public String getPayload() {
        return payload;
    }

    public static ObjectMapper getMapper() {
        return mapper;
    }
}
