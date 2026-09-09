package com.example.gitguiwindowsapp.ui;

import javafx.concurrent.Task;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * JavaFX Taskの生成とバックグラウンドスレッド管理を担当します。
 */
public final class OperationCoordinator {
    public <T> void execute(Callable<T> work, Consumer<T> success,
                            Consumer<Throwable> failure, String operationName) {
        Objects.requireNonNull(work, "work");
        Objects.requireNonNull(success, "success");
        Objects.requireNonNull(failure, "failure");
        Objects.requireNonNull(operationName, "operationName");

        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(event -> success.accept(task.getValue()));
        task.setOnFailed(event -> failure.accept(task.getException()));
        Thread thread = new Thread(task, "git-" + operationName);
        thread.setDaemon(true);
        thread.start();
    }
}
