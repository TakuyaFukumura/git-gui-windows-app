package com.example.gitguiwindowsapp.io;

import com.example.gitguiwindowsapp.model.Message;
import com.example.gitguiwindowsapp.validation.MessageValidator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * メッセージのテキスト／CSV入出力を提供します。
 */
public final class MessageFileService {

    private static final String CSV_HEADER = "id,text,created_at";

    private MessageFileService() {
    }

    /**
     * 指定形式でメッセージをファイルへ書き出します。
     *
     * @param path     出力先
     * @param messages 書き出すメッセージ
     * @param format   ファイル形式
     * @throws IOException ファイル操作に失敗した場合
     */
    public static void write(Path path, List<Message> messages, Format format) throws IOException {
        List<String> lines = new ArrayList<>();
        if (format == Format.CSV) {
            lines.add(CSV_HEADER);
            for (Message message : messages) {
                lines.add(message.getId() + "," + escapeCsv(MessageValidator.normalize(message.getText())) + ","
                        + message.getCreatedAt());
            }
        } else {
            for (Message message : messages) {
                lines.add(MessageValidator.normalize(message.getText()));
            }
        }
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    /**
     * 指定形式のファイルからメッセージを読み込みます。
     *
     * @param path   入力元
     * @param format ファイル形式
     * @return 読み込んだメッセージ
     * @throws IOException ファイル操作または形式解析に失敗した場合
     */
    public static List<Message> read(Path path, Format format) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (format == Format.CSV) {
            return readCsv(lines);
        }

        List<Message> messages = new ArrayList<>();
        for (String line : lines) {
            if (!line.isBlank()) {
                messages.add(new Message(MessageValidator.normalize(line), System.currentTimeMillis()));
            }
        }
        return messages;
    }

    private static List<Message> readCsv(List<String> lines) throws IOException {
        if (lines.isEmpty() || !CSV_HEADER.equals(lines.get(0))) {
            throw new IOException("CSVのヘッダーが正しくありません。");
        }

        List<Message> messages = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) {
                continue;
            }
            List<String> fields = parseCsvLine(lines.get(i));
            if (fields.size() != 3) {
                throw new IOException("CSVの" + (i + 1) + "行目の列数が正しくありません。");
            }
            try {
                messages.add(new Message(
                        Integer.parseInt(fields.get(0)),
                        MessageValidator.normalize(fields.get(1)),
                        Long.parseLong(fields.get(2))));
            } catch (IllegalArgumentException e) {
                throw new IOException("CSVの" + (i + 1) + "行目の値が正しくありません。", e);
            }
        }
        return messages;
    }

    private static String escapeCsv(String value) {
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private static List<String> parseCsvLine(String line) throws IOException {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean afterClosingQuote = false;

        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            if (inQuotes) {
                if (current == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else if (current == '"') {
                    inQuotes = false;
                    afterClosingQuote = true;
                } else {
                    field.append(current);
                }
            } else if (afterClosingQuote) {
                if (current != ',') {
                    throw new IOException("CSVの引用符の後に不正な文字があります。");
                }
                fields.add(field.toString());
                field.setLength(0);
                afterClosingQuote = false;
            } else if (current == '"') {
                if (!field.isEmpty()) {
                    throw new IOException("CSVの引用符の位置が正しくありません。");
                }
                inQuotes = true;
            } else if (current == ',') {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(current);
            }
        }
        if (inQuotes) {
            throw new IOException("CSVの引用符が閉じられていません。");
        }
        fields.add(field.toString());
        return fields;
    }

    public enum Format {
        TEXT,
        CSV
    }
}
