package com.example;

import com.amazonaws.services.sqs.model.Message;

import java.util.Optional;

public interface QueueService {

    //
    // Task 1: Define me.
    //

    /**
     * Pushes a message onto a queue.
     *
     * @param message
     */
    void push(String message);

    /**
     * Retrieves a single message from a queue
     *
     * @return Message
     */
    Optional<Message> pull();

    /**
     * Deletes a message from the queue that was received by pull()
     *
     * @param receiptHandle
     */
    void delete(String receiptHandle);

}
