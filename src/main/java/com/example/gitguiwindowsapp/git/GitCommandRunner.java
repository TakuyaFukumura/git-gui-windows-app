package com.example.gitguiwindowsapp.git;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * Executes Git without invoking a shell. Paths and user input therefore remain
 * separate arguments even when they contain spaces or shell metacharacters.
 */
public final class GitCommandRunner implements GitCommandExecutor {
    private final String executable;
    private final Charset charset;

    public GitCommandRunner() {
        this("git");
    }

    public GitCommandRunner(String executable) {
        this(executable, StandardCharsets.UTF_8);
    }

    public GitCommandRunner(Path executable) {
        this(executable.toString(), StandardCharsets.UTF_8);
    }

    public GitCommandRunner(String executable, Charset charset) {
        this.executable = Objects.requireNonNull(executable, "executable");
        this.charset = Objects.requireNonNull(charset, "charset");
    }

    private static byte[] readAll(InputStream stream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        stream.transferTo(output);
        return output.toByteArray();
    }

    public String executable() {
        return executable;
    }

    public GitCommandResult run(Path workingDirectory, List<String> arguments)
            throws IOException, InterruptedException {
        Objects.requireNonNull(workingDirectory, "workingDirectory");
        Objects.requireNonNull(arguments, "arguments");
        if (!Files.isDirectory(workingDirectory)) {
            throw new IOException("Working directory does not exist: " + workingDirectory);
        }

        List<String> command = new ArrayList<>(arguments.size() + 1);
        command.add(executable);
        command.addAll(arguments);
        Process process;
        try {
            process = new ProcessBuilder(command)
                    .directory(workingDirectory.toFile())
                    .redirectErrorStream(false)
                    .start();
        } catch (IOException e) {
            throw new IOException("Unable to start Git executable '" + executable + "'", e);
        }

        long started = System.nanoTime();
        try (InputStream stdout = process.getInputStream();
             InputStream stderr = process.getErrorStream();
             ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<byte[]> outputFuture = executor.submit(() -> readAll(stdout));
            Future<byte[]> errorFuture = executor.submit(() -> readAll(stderr));
            int exitCode = process.waitFor();
            byte[] output = outputFuture.get();
            byte[] error = errorFuture.get();
            return new GitCommandResult(exitCode, new String(output, charset),
                    new String(error, charset),
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw e;
        } catch (ExecutionException e) {
            process.destroyForcibly();
            Throwable cause = e.getCause();
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Unable to read Git output.", cause);
        }
    }
}
