package com.flowci.docker.domain;

import com.github.dockerjava.api.async.ResultCallback;
import lombok.Getter;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public abstract class DockerCallback<T> implements ResultCallback<T> {

    @Getter
    protected final CountDownLatch counter = new CountDownLatch(1);

    @Getter
    protected Throwable throwable;

    @Override
    public void onStart(Closeable closeable) {

    }

    @Override
    public void onError(Throwable throwable) {
        this.throwable = throwable;
        counter.countDown();
    }

    @Override
    public void onComplete() {
        counter.countDown();
    }

    @Override
    public void close() throws IOException {

    }

    public boolean hasError() {
        return this.throwable != null;
    }
}
