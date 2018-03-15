package com.example;

import com.amazonaws.services.sqs.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

public class InMemoryQueueService implements QueueService {

    private ScheduledExecutorService executorService;
    private final Long visibilityTimeout;

    //BlockingQueue uses FIFO and is thread-safe
    private BlockingQueue<String> messages;

    private ConcurrentMap<String, ScheduledFuture<Message>> temporaryMessages = new ConcurrentHashMap<>();

    final Logger logger = LoggerFactory.getLogger(InMemoryQueueService.class);

    public InMemoryQueueService(ScheduledExecutorService executorService, Integer queueSize, Long visibilityTimeout) {
        this.executorService = executorService;
        this.visibilityTimeout = visibilityTimeout;
        messages = new LinkedBlockingQueue<>(queueSize);
    }

    public void push(String messageBody) {
        messages.add(messageBody);
        logger.info("===Message added===");
    }

    public Optional<Message> pull() {
        String randomUuid = UUID.randomUUID().toString();
        String messageBody = messages.poll();

        Runnable runnable = () -> {
            messages.add(messageBody);
            temporaryMessages.remove(randomUuid);
            logger.info("===Pull Message : " + messageBody + "===");
        };
        ScheduledFuture<Message> scheduleFuture = (ScheduledFuture<Message>) executorService.schedule(runnable, visibilityTimeout, TimeUnit.MILLISECONDS);

        temporaryMessages.put(randomUuid, scheduleFuture);

        Message msg = new Message()
                .withMessageId(randomUuid)
                .withBody(messageBody)
                .withReceiptHandle(randomUuid);

        return Optional.of(msg);
    }

    public void delete(String receiptHandle) {
        ScheduledFuture<Message> sFuture = temporaryMessages.remove(receiptHandle);
        if (sFuture == null) {
            throw new RuntimeException("Message not exist");
        }

        logger.info("===Message Deleted : " + receiptHandle + "===");

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
