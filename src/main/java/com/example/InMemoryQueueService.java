package com.example;

import com.amazonaws.services.sqs.model.Message;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

public class InMemoryQueueService implements QueueService {

    //
    // Task 2: Implement me.
    //

    public static final Integer DEFAULT_VISIBILITY_TIMEOUT = 3000;

    private ScheduledExecutorService executorService;

    //BlockingQueue uses FIFO and is thread-safe
    private BlockingQueue<String> messages;

    private ConcurrentMap<String, ScheduledFuture> temporaryMessages = new ConcurrentHashMap<>();

    public InMemoryQueueService(ScheduledExecutorService executorService, Integer queueSize) {
        this.executorService = executorService;
        messages = new LinkedBlockingQueue<>(queueSize);
    }

    /**
     * Add message into memory blocking queue
     *
     * @param messageBody
     */
    public void push(String messageBody) {
        messages.add(messageBody);
        System.out.println("===Message added.===");
    }

    /**
     * Retrieve a message from queue
     *
     * @return Message
     */
    public Optional<Message> pull() {
        String randomUuid = UUID.randomUUID().toString();
        String messageBody = messages.poll();

        // Schedule reinsert on messages
        Runnable runnable = () -> {
            messages.add(messageBody);
            temporaryMessages.remove(randomUuid);
            System.out.println("===Pull Message : " + messageBody + "===");
        };
        ScheduledFuture scheduleFuture = executorService.schedule(runnable, (long) this.DEFAULT_VISIBILITY_TIMEOUT, TimeUnit.MILLISECONDS);

        // And save that into our in-flight map
        temporaryMessages.put(randomUuid, scheduleFuture);

        Message msg = new Message()
                .withMessageId(randomUuid)
                .withBody(messageBody)
                .withReceiptHandle(randomUuid);

        return Optional.of(msg);
    }

    /**
     * If the consumer delete the message, it will be removed from the temporaryMessages
     *
     * @param receiptHandle
     */
    public void delete(String receiptHandle) {
        ScheduledFuture sFuture = temporaryMessages.remove(receiptHandle);
        if (sFuture == null) {
            throw new RuntimeException("Message not exist");
        }

        // Cancel the future schedule
        sFuture.cancel(true);
    }

    public Integer getMessagesSize() {
        return messages.size();
    }

    public Integer getTemporaryMessagesSize() {
        return temporaryMessages.size();
    }

}
