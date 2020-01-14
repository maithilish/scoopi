package org.codetab.scoopi.store.solo.simple;

import static org.apache.commons.lang3.Validate.notNull;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Singleton;

import org.codetab.scoopi.model.Payload;
import org.codetab.scoopi.store.solo.ISoloJobStore;

@Singleton
public class JobStore implements ISoloJobStore {

    // private static final int QUEUE_SIZE = 32768;

    private Queue<Payload> jobs = new ConcurrentLinkedQueue<>();

    private AtomicLong jobIdCounter = new AtomicLong();

    private State state = State.NEW;

    @Override
    public boolean open() {
        return true;
    }

    @Override
    public Payload takeJob() throws InterruptedException {
        Payload payload = jobs.poll();
        if (Objects.isNull(payload)) {
            throw new NoSuchElementException("jobs queue is empty");
        }
        return payload;
    }

    @Override
    public boolean putJob(final Payload payload) throws InterruptedException {
        notNull(payload, "payload must not be null");
        jobs.offer(payload);
        return true;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void setState(final State state) {
        this.state = state;
    }

    @Override
    public boolean markFinished(final long id) {
        // not implemented
        return true;
    }

    @Override
    public int getJobCount() {
        return jobs.size();
    }

    @Override
    public boolean isDone() {
        return jobs.size() == 0;
    }

    @Override
    public boolean changeStateToInitialize() {
        if (state.equals(State.NEW)) {
            state = State.INITIALIZE;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getNodeId() {
        return "solo";
    }

    @Override
    public int getJobTakenCount() {
        // solo no limit
        return 0;
    }

    @Override
    public int getJobTakeLimit() {
        // solo - no take job limit
        return Integer.MAX_VALUE;
    }

    @Override
    public long getJobIdSeq() {
        return jobIdCounter.getAndIncrement();
    }

    @Override
    public boolean close() {
        return true;
    }
}