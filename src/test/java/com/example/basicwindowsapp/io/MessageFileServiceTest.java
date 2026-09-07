package com.example.basicwindowsapp.io;

import com.example.basicwindowsapp.model.Message;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessageFileServiceTest {

    @Test
    void csvRoundTripPreservesMessageFields() throws Exception {
        Path file = Files.createTempFile("messages", ".csv");
        try {
            List<Message> source = List.of(
                    new Message(1, "Hello, \"World\"", 123L),
                    new Message(2, "Second", 456L));

            MessageFileService.write(file, source, MessageFileService.Format.CSV);

            List<Message> result = MessageFileService.read(file, MessageFileService.Format.CSV);

            assertEquals(source.size(), result.size());
            for (int i = 0; i < source.size(); i++) {
                assertEquals(source.get(i).getId(), result.get(i).getId());
                assertEquals(source.get(i).getText(), result.get(i).getText());
                assertEquals(source.get(i).getCreatedAt(), result.get(i).getCreatedAt());
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void textRoundTripImportsOneMessagePerLine() throws Exception {
        Path file = Files.createTempFile("messages", ".txt");
        try {
            Files.writeString(file, "First\nSecond\n");

            List<Message> result = MessageFileService.read(file, MessageFileService.Format.TEXT);

            assertEquals(List.of("First", "Second"),
                    result.stream().map(Message::getText).toList());
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void textExportRejectsMessageContainingNewline() throws Exception {
        Path file = Files.createTempFile("messages", ".txt");
        try {
            Message message = new Message(1, "First\nSecond", 123L);

            assertThrows(IllegalArgumentException.class,
                    () -> MessageFileService.write(
                            file, List.of(message), MessageFileService.Format.TEXT));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void csvRejectsQuoteAfterUnquotedText() throws Exception {
        Path file = Files.createTempFile("messages", ".csv");
        try {
            Files.writeString(file, "id,text,created_at\n1,invalid\"text,123\n");

            assertThrows(Exception.class,
                    () -> MessageFileService.read(file, MessageFileService.Format.CSV));
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
